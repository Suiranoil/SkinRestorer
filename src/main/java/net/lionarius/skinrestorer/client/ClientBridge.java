package net.lionarius.skinrestorer.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.network.SkinNetwork;
import net.lionarius.skinrestorer.network.SkinUpdate;
import net.lionarius.skinrestorer.skin.SkinTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;

/** All Minecraft access is on the client tick; ingress and download callbacks only enqueue. */
public final class ClientBridge {

    private static final int MAX_PENDING_PLAYERS = 64;
    private static final int MAX_DESIRED_PLAYERS = 128;
    private static final int MAX_CALLBACKS = 64;
    private static final int MAX_BOUND_ENTITIES = 64;
    private static final int PLAYERS_PER_TICK = 20;
    private static final int HELLO_RETRY_TICKS = 100;
    private static final int MAX_HELLO_ATTEMPTS = 12;
    private static volatile Session session;
    private static volatile Thread clientThread;

    private ClientBridge() {}

    static void registerClientThread() {
        clientThread = Thread.currentThread();
    }

    static synchronized void connected(NetworkManager manager) {
        session = new Session(manager);
    }

    static synchronized void disconnected(NetworkManager manager) {
        Session current = session;
        // A delayed disconnect from the preceding connection must not reset the new one.
        if (current != null && current.manager == manager) {
            session = null;
        }
    }

    static void acknowledge(NetworkManager manager) {
        Session current = session;
        if (current != null && current.manager == manager) {
            // Network-thread ingress: no Minecraft/entity access or scheduled task.
            current.helloAcknowledged = true;
        }
    }

    static void receiveSkin(SkinUpdate update) {
        Session current = session;
        if (current == null || current.disabled || update == null || update.connection != current.manager) return;
        if (!update.valid || update.playerId == null || update.revision < 0) {
            current.invalidUpdateSeen = true;
            return;
        }
        if (current.disabled) return;
        PendingSkin pending = new PendingSkin(update.playerId, update.revision, update.texture);
        synchronized (current.pending) {
            if (session != current) {
                return;
            }
            PendingSkin previous = current.pending.get(pending.playerId);
            if (previous != null && previous.revision >= pending.revision) {
                return;
            }
            if (previous != null || current.pending.size() < MAX_PENDING_PLAYERS) {
                current.pending.put(pending.playerId, pending);
            }
        }
    }

    static void tick() {
        clientThread = Thread.currentThread();
        Session current = session;
        if (current == null) {
            return;
        }
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            notifyMinorFailures(current, minecraft);
            if (current.disabled) {
                notifyFailure(current, minecraft);
                return;
            }
            tickActive(current, minecraft);
        } catch (RuntimeException | LinkageError failure) {
            disable(current, failure);
            try {
                notifyFailure(current, Minecraft.getMinecraft());
            } catch (RuntimeException | LinkageError ignored) {
                // Keep the deferred notice until the client/renderer is available.
            }
        }
    }

    private static void tickActive(Session current, Minecraft minecraft) {
        if (minecraft.theWorld == null || minecraft.thePlayer == null
            || minecraft.getNetHandler() == null
            || minecraft.thePlayer.sendQueue != minecraft.getNetHandler()
            || minecraft.getNetHandler()
                .getNetworkManager() != current.manager) {
            return;
        }
        if (!(minecraft.thePlayer instanceof ClientPlayerAccess)) {
            throw new IllegalStateException("The client player skin bridge mixin was not applied");
        }
        handshake(current);
        notifyMinorFailures(current, minecraft);
        drainUpdates(current);
        if (session != current || current.disabled) {
            return;
        }
        // Keep desired revisions through respawn / dimension change, but retire old entities.
        current.bindings.keySet()
            .removeIf(key -> !isCurrentEntity(key.player, minecraft));
        apply(current, minecraft.thePlayer, minecraft);
        List<?> players = minecraft.theWorld.playerEntities;
        int size = players.size();
        int count = Math.min(size, PLAYERS_PER_TICK);
        for (int i = 0; i < count && session == current && !current.disabled; i++) {
            int index = (current.playerCursor + i) % size;
            Object candidate = players.get(index);
            if (candidate instanceof AbstractClientPlayer player && player != minecraft.thePlayer) {
                apply(current, player, minecraft);
            }
        }
        current.playerCursor = size == 0 ? 0 : (current.playerCursor + count) % size;
        if (!current.disabled) drainCallbacks(current);
    }

    /** Proxy ingress uses only the event/packet's connection identity, never Minecraft. */
    static void failure(NetworkManager manager, Throwable cause) {
        Session current = session;
        if (current != null && current.manager == manager) disable(current, cause);
    }

    private static void disable(Session current, Throwable cause) {
        if (current == null || current != session) return;
        synchronized (current) {
            if (current == session && !current.disabled) {
                current.failure = cause;
                current.disabled = true;
            }
        }
    }

    private static void notifyFailure(Session current, Minecraft minecraft) {
        if (current != session || !current.disabled) return;
        if (!current.failureLogged) {
            current.failureLogged = true;
            try {
                SkinRestorer.LOGGER.warn(
                    "SkinRestorer client bridge disabled for this connection; normal game rendering continues.",
                    current.failure);
            } catch (RuntimeException | LinkageError ignored) {
                // A broken logger must not turn an optional bridge failure into a game failure.
            }
            synchronized (current.pending) {
                current.pending.clear();
            }
            synchronized (current.callbacks) {
                current.callbacks.clear();
            }
            current.desired.clear();
            current.bindings.clear();
        }
        if (!current.noticeDelivered && minecraft.thePlayer != null
            && minecraft.thePlayer.sendQueue != null
            && minecraft.thePlayer.sendQueue.getNetworkManager() == current.manager) {
            current.noticeDelivered = true;
            try {
                minecraft.thePlayer
                    .addChatMessage(new ChatComponentText("[SkinRestorer] 在线皮肤桥接暂时不可用，本次连接已停用。游戏可继续，当前显示会保留；重连可重试。"));
            } catch (RuntimeException | LinkageError ignored) {
                // Do not spam retries or escape into the client's tick if chat itself is unavailable.
            }
        }
    }

    private static void notifyMinorFailures(Session current, Minecraft minecraft) {
        if (current != session) return;
        if (current.invalidUpdateSeen) {
            if (!current.invalidUpdateLogged) {
                current.invalidUpdateLogged = true;
                try {
                    SkinRestorer.LOGGER.warn(
                        "SkinRestorer ignored invalid skin data; current skins and gameplay continue.",
                        (Throwable) null);
                } catch (RuntimeException | LinkageError ignored) {
                    // A warning must not disable otherwise healthy skin synchronization.
                }
            }
            if (!current.invalidNoticeDelivered) {
                current.invalidNoticeDelivered = sendNotice(
                    current,
                    minecraft,
                    "[SkinRestorer] 收到无效的皮肤数据，已忽略；当前皮肤和游戏不受影响。");
            }
        }
        if (current.helloFailureReported && !current.helloAcknowledged && !current.helloNoticeDelivered) {
            current.helloNoticeDelivered = sendNotice(
                current,
                minecraft,
                "[SkinRestorer] 服务端暂未确认在线皮肤桥接；当前皮肤保留，游戏可继续。重连可重新尝试。");
        }
    }

    private static boolean sendNotice(Session current, Minecraft minecraft, String message) {
        if (current != session) return false;
        try {
            if (minecraft.thePlayer == null || minecraft.thePlayer.sendQueue == null
                || minecraft.thePlayer.sendQueue.getNetworkManager() != current.manager) return false;
            minecraft.thePlayer.addChatMessage(new ChatComponentText(message));
        } catch (RuntimeException | LinkageError ignored) {
            // One attempt is enough if the optional chat/loader path itself is broken.
        }
        return true;
    }

    private static void handshake(Session current) {
        if (current.helloAcknowledged || session != current) {
            return;
        }
        if (current.helloDelay > 0) {
            current.helloDelay--;
            if (current.helloDelay > 0) {
                return;
            }
        }
        if (current.helloAttempts >= MAX_HELLO_ATTEMPTS) {
            if (!current.helloFailureReported) {
                current.helloFailureReported = true;
                try {
                    SkinRestorer.LOGGER.warn(
                        "SkinRestorer client handshake was not acknowledged after 12 attempts; reconnect to retry.",
                        current.lastHelloFailure);
                } catch (RuntimeException | LinkageError ignored) {
                    // A missing logger must not disrupt an otherwise usable connection.
                }
            }
            return;
        }
        current.helloAttempts++;
        current.helloDelay = HELLO_RETRY_TICKS;
        try {
            SkinNetwork.hello();
        } catch (RuntimeException exception) {
            // A transient send failure consumes this attempt, without disrupting skin/tick work.
            current.lastHelloFailure = exception;
        }
    }

    private static void drainUpdates(Session current) {
        List<PendingSkin> updates;
        synchronized (current.pending) {
            updates = new ArrayList<>(current.pending.values());
            current.pending.clear();
        }
        for (PendingSkin update : updates) {
            DesiredSkin previous = current.desired.get(update.playerId);
            if (previous != null && previous.revision >= update.revision) {
                continue;
            }
            Descriptor descriptor = update.texture == null ? null : Descriptor.of(update.texture);
            if (!current.desired.containsKey(update.playerId) && current.desired.size() >= MAX_DESIRED_PLAYERS) {
                retireOldestUnbound(current);
            }
            current.desired.put(update.playerId, new DesiredSkin(update.revision, descriptor));
        }
    }

    private static void retireOldestUnbound(Session current) {
        Set<UUID> bound = ConcurrentHashMap.newKeySet();
        for (EntityKey key : current.bindings.keySet()) {
            bound.add(key.player.getUniqueID());
        }
        Iterator<UUID> iterator = current.desired.keySet()
            .iterator();
        while (iterator.hasNext()) {
            if (!bound.contains(iterator.next())) {
                iterator.remove();
                return;
            }
        }
        // At most 64 bound entities are retained, so 128 desired entries always
        // contain an unbound candidate. Active players are never evicted.
    }

    private static boolean isCurrentEntity(AbstractClientPlayer player, Minecraft minecraft) {
        return !player.isDead && player.worldObj == minecraft.theWorld
            && (player == minecraft.thePlayer || minecraft.theWorld.getEntityByID(player.getEntityId()) == player);
    }

    private static void apply(Session current, AbstractClientPlayer player, Minecraft minecraft) {
        if (!isCurrentEntity(player, minecraft)) {
            return;
        }
        DesiredSkin desired = current.desired.get(player.getUniqueID());
        if (desired == null) return;
        if (!(player instanceof ClientPlayerAccess access)) {
            throw new IllegalStateException("A client player is missing the skin bridge accessor");
        }
        Binding binding = current.bindings.get(new EntityKey(player));
        if (binding != null && binding.revision == desired.revision) {
            return;
        }
        GameProfile live = player.getGameProfile();
        if (binding == null) {
            if (current.bindings.size() >= MAX_BOUND_ENTITIES) {
                return;
            }
            // SSB initializes its configured default model when getLocationSkin is read.
            player.getLocationSkin();
            List<Property> originals = new ArrayList<>(
                live.getProperties()
                    .get("textures"));
            GameProfile baseline = snapshot(live);
            Descriptor original = SkinTexture.fromProfile(baseline)
                .map(Descriptor::of)
                .orElse(null);
            binding = new Binding(originals, access.skinrestorer$getSkin(), access.skinrestorer$isSlim(), original);
            current.bindings.put(new EntityKey(player), binding);
        }
        Descriptor target = desired.descriptor == null ? binding.originalDescriptor : desired.descriptor;
        binding.target = target;
        binding.revision = desired.revision;
        ResourceLocation previousLocation = access.skinrestorer$getSkin();
        boolean previousSlim = access.skinrestorer$isSlim();
        List<Property> previousProperties = new ArrayList<>(
            live.getProperties()
                .get("textures"));
        try {
            live.getProperties()
                .removeAll("textures");
            if (desired.descriptor == null) {
                live.getProperties()
                    .putAll("textures", binding.originalProperties);
                binding.displayedSlim = binding.originalSlim;
                access.skinrestorer$setSlim(binding.originalSlim);
                access.skinrestorer$setSkin(binding.originalLocation);
            } else {
                live.getProperties()
                    .put("textures", desired.descriptor.texture.toProperty());
            }
            if (target != null && session == current) {
                // The immutable snapshot keeps later revisions out of SkinManager's worker.
                // The actual entity must be the callback: SSB's Utils.isPlayer checks EntityPlayer.
                minecraft.func_152342_ad()
                    .func_152790_a(snapshot(live), player, true);
            }
        } catch (RuntimeException | LinkageError failure) {
            disable(current, failure);
            try {
                live.getProperties()
                    .removeAll("textures");
                live.getProperties()
                    .putAll("textures", previousProperties);
                access.skinrestorer$setSkin(previousLocation);
                access.skinrestorer$setSlim(previousSlim);
            } catch (RuntimeException | LinkageError ignored) {
                // Preserve the entity and visible texture even if its profile map is unavailable.
            }
        }
    }

    private static GameProfile snapshot(GameProfile profile) {
        GameProfile copy = new GameProfile(profile.getId(), profile.getName());
        copy.getProperties()
            .putAll(profile.getProperties());
        return copy;
    }

    /** Called only for SKIN; cape callbacks keep their original behavior. */
    public static boolean acceptCallback(AbstractClientPlayer player, ResourceLocation location) {
        Session current = session;
        if (current == null || current.disabled) return true;
        try {
            return acceptCallbackActive(current, player, location);
        } catch (RuntimeException | LinkageError failure) {
            disable(current, failure);
            return true;
        }
    }

    private static boolean acceptCallbackActive(Session current, AbstractClientPlayer player,
        ResourceLocation location) {
        Binding binding = current.bindings.get(new EntityKey(player));
        if (binding != null && !matches(binding, location)) {
            return false;
        }
        if (Thread.currentThread() != clientThread) {
            synchronized (current.callbacks) {
                if (session == current
                    && (current.callbacks.containsKey(player) || current.callbacks.size() < MAX_CALLBACKS)) {
                    current.callbacks.put(player, location);
                }
            }
            return false;
        }
        if (binding != null && binding.target != null) {
            binding.displayedSlim = binding.target.slim;
            ((ClientPlayerAccess) player).skinrestorer$setSlim(binding.displayedSlim);
        }
        return true;
    }

    private static boolean matches(Binding binding, ResourceLocation location) {
        Descriptor target = binding.target;
        if (location == null) {
            return target == null;
        }
        if (target == null) {
            return location.equals(binding.originalLocation);
        }
        String domain = location.getResourceDomain();
        return ("minecraft".equals(domain) || "simpleskinbackport".equals(domain))
            && target.resourcePath.equals(location.getResourcePath());
    }

    /** SSB calls this before the download callback and after SkinManager returns. */
    public static boolean acceptArmChange(AbstractClientPlayer player, boolean slim) {
        Session current = session;
        if (current == null || current.disabled) return true;
        try {
            Binding binding = current.bindings.get(new EntityKey(player));
            // Download-buffer metadata precedes the skin callback. Its worker may
            // finish after a newer main-thread callback, so it must never write even
            // an apparently equal value. The accepted callback installs the model.
            return binding == null || (Thread.currentThread() == clientThread && binding.displayedSlim == slim);
        } catch (RuntimeException | LinkageError failure) {
            disable(current, failure);
            return true;
        }
    }

    private static void drainCallbacks(Session current) {
        Map<AbstractClientPlayer, ResourceLocation> callbacks;
        synchronized (current.callbacks) {
            callbacks = new IdentityHashMap<>(current.callbacks);
            current.callbacks.clear();
        }
        for (Map.Entry<AbstractClientPlayer, ResourceLocation> callback : callbacks.entrySet()) {
            if (session != current) {
                return;
            }
            AbstractClientPlayer player = callback.getKey();
            if (isCurrentEntity(player, Minecraft.getMinecraft())) {
                player.func_152121_a(MinecraftProfileTexture.Type.SKIN, callback.getValue());
            }
        }
    }

    private static final class Session {

        private final NetworkManager manager;
        private final Map<UUID, PendingSkin> pending = new LinkedHashMap<>();
        private final Map<UUID, DesiredSkin> desired = new LinkedHashMap<>();
        private final Map<EntityKey, Binding> bindings = new ConcurrentHashMap<>();
        private final Map<AbstractClientPlayer, ResourceLocation> callbacks = new IdentityHashMap<>();
        private volatile boolean helloAcknowledged;
        private volatile boolean invalidUpdateSeen;
        private boolean invalidUpdateLogged;
        private boolean invalidNoticeDelivered;
        private boolean helloNoticeDelivered;
        private volatile boolean disabled;
        private Throwable failure;
        private boolean failureLogged;
        private boolean noticeDelivered;
        private int helloAttempts;
        private int helloDelay;
        private boolean helloFailureReported;
        private RuntimeException lastHelloFailure;
        private int playerCursor;

        private Session(NetworkManager manager) {
            this.manager = manager;
        }
    }

    private record EntityKey(AbstractClientPlayer player) {

        @Override
        public boolean equals(Object other) {
            return other instanceof EntityKey key && player == key.player;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(player);
        }
    }

    private static final class Binding {

        private final List<Property> originalProperties;
        private final ResourceLocation originalLocation;
        private final boolean originalSlim;
        private final Descriptor originalDescriptor;
        private long revision = Long.MIN_VALUE;
        private volatile Descriptor target;
        private volatile boolean displayedSlim;

        private Binding(List<Property> properties, ResourceLocation location, boolean slim, Descriptor descriptor) {
            originalProperties = List.copyOf(properties);
            originalLocation = location;
            originalSlim = slim;
            originalDescriptor = descriptor;
            displayedSlim = slim;
        }
    }

    private record PendingSkin(UUID playerId, long revision, SkinTexture texture) {}

    private record DesiredSkin(long revision, Descriptor descriptor) {}

    private record Descriptor(SkinTexture texture, String resourcePath, boolean slim) {

        private static Descriptor of(SkinTexture texture) {
            String json = new String(
                Base64.getDecoder()
                    .decode(texture.value()),
                StandardCharsets.UTF_8);
            JsonObject skin = new JsonParser().parse(json)
                .getAsJsonObject()
                .getAsJsonObject("textures")
                .getAsJsonObject("SKIN");
            JsonObject metadata = skin.getAsJsonObject("metadata");
            boolean slim = metadata != null && metadata.has("model")
                && "slim".equals(
                    metadata.get("model")
                        .getAsString());
            // SkinTexture permits only /texture/<hex> URLs, without query or suffix.
            // Their final segment is the hash returned by all supported old Authlib versions.
            String url = skin.get("url")
                .getAsString();
            String hash = url.substring(url.lastIndexOf('/') + 1);
            return new Descriptor(texture, "skins/" + hash, slim);
        }
    }
}
