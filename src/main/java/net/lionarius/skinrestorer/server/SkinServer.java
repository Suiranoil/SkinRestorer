package net.lionarius.skinrestorer.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.lionarius.skinrestorer.SkinConfig;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.http.MojangClient;
import net.lionarius.skinrestorer.network.SkinNetwork;
import net.lionarius.skinrestorer.network.SkinUpdate;
import net.lionarius.skinrestorer.skin.SkinState;
import net.lionarius.skinrestorer.skin.SkinStore;
import net.lionarius.skinrestorer.skin.SkinTexture;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public final class SkinServer implements AutoCloseable {

    private final MinecraftServer minecraft;
    private final Path world;
    private final Map<UUID, SkinState> states = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Map<UUID, EntityPlayerMP> online = new HashMap<>();
    private final Map<UUID, Long> operations = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> bridges = new HashSet<>();
    private final Set<UUID> saving = new HashSet<>();
    private final Map<UUID, List<Runnable>> afterSave = new HashMap<>();
    private final ArrayBlockingQueue<Runnable> inbound = new ArrayBlockingQueue<>(64);
    private final ConcurrentLinkedQueue<Runnable> completions = new ConcurrentLinkedQueue<>();
    private final ThreadPoolExecutor disk = new ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(256),
        task -> {
            Thread thread = new Thread(task, "SkinRestorer-Storage");
            thread.setDaemon(true);
            return thread;
        });
    private volatile boolean active = true;
    private volatile SkinStore store;
    private SkinConfig config;
    private MojangClient mojang;
    private boolean ready;
    private String storageFailure;
    private long revision;

    public SkinServer(MinecraftServer minecraft, Path world, SkinConfig config) {
        this.minecraft = minecraft;
        this.world = world;
        this.config = config;
        this.mojang = createClient(config);
        disk.execute(() -> {
            try {
                store = new SkinStore(world.resolve("skinrestorer"));
                Map<UUID, SkinState> loaded = new LinkedHashMap<>();
                for (UUID id : store.loadKnownPlayers()
                    .keySet()) {
                    store.read(id)
                        .ifPresent(state -> loaded.put(id, state));
                }
                Set<UUID> historical = loadPlayerIds(world.resolve("playerdata"));
                complete(() -> {
                    states.putAll(loaded);
                    for (SkinState state : loaded.values()) {
                        names.put(state.id(), state.playerName());
                        revision = Math.max(revision, state.revision());
                    }
                    for (UUID id : historical) {
                        GameProfile profile = minecraft.func_152358_ax()
                            .func_152652_a(id);
                        if (profile != null) names.put(id, profile.getName());
                        else names.putIfAbsent(id, "");
                    }
                    ready = true;
                    for (EntityPlayerMP player : new ArrayList<>(online.values())) prepareJoin(player);
                });
            } catch (Exception | LinkageError error) {
                SkinRestorer.LOGGER.error("Cannot initialize world skin storage; skin commands are unavailable", error);
                complete(() -> {
                    storageFailure = "Skin storage is unavailable; normal login and gameplay remain available.";
                    for (EntityPlayerMP player : online.values()) say(player, storageFailure);
                });
            }
        });
    }

    public boolean enqueue(Runnable task) {
        return active && inbound.offer(task);
    }

    private void complete(Runnable task) {
        if (active) completions.offer(task);
    }

    public void tick() {
        drain(completions, 128);
        for (int i = 0; i < 32; i++) {
            Runnable task = inbound.poll();
            if (task == null) break;
            run(task);
        }
    }

    private void drain(ConcurrentLinkedQueue<Runnable> queue, int limit) {
        for (int i = 0; i < limit; i++) {
            Runnable task = queue.poll();
            if (task == null) break;
            run(task);
        }
    }

    private void run(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException | LinkageError error) {
            SkinRestorer.LOGGER.error("Skin operation failed; gameplay continues", error);
        }
    }

    public void joined(EntityPlayerMP player) {
        if (player instanceof FakePlayer) return;
        UUID id = player.getUniqueID();
        online.put(id, player);
        names.put(id, player.getCommandSenderName());
        if (SkinRestorer.startupWarning() != null) say(player, SkinRestorer.startupWarning());
        if (storageFailure != null) say(player, storageFailure);
        if (ready) run(() -> prepareJoin(player));
    }

    private void prepareJoin(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        if (saving.contains(id)) {
            afterSave.computeIfAbsent(id, ignored -> new ArrayList<>())
                .add(() -> { if (online.get(id) == player) prepareJoin(player); });
            return;
        }
        String name = player.getCommandSenderName();
        names.put(id, name);
        SkinState saved = states.get(id);
        SkinTexture original = SkinTexture.fromProfile(player.getGameProfile())
            .orElse(null);
        String selected = saved == null ? null : saved.selectedName();
        SkinTexture cached = saved == null ? null : saved.texture();
        SkinTexture initial = selected != null ? cached : (original != null ? original : cached);
        SkinState state = new SkinState(id, name, selected, initial, ++revision);
        states.put(id, state);
        long token = nextOperation(id);
        persist(state, token, null, false, () -> {});
        if (initial != null && (selected != null || original == null)) apply(player, state);
        else if (initial != null) broadcast(state);
        if (selected != null || original == null) fetch(state, token, null, saved != null);
    }

    public void rebuilt(EntityPlayerMP player) {
        if (player instanceof FakePlayer) return;
        online.put(player.getUniqueID(), player);
        SkinState state = states.get(player.getUniqueID());
        if (state != null && state.texture() != null) apply(player, state);
    }

    public void left(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        if (online.get(id) != player) return;
        online.remove(id);
        bridges.remove(id);
        cooldowns.remove(id);
        nextOperation(id);
    }

    public void handshake(EntityPlayerMP player) {
        UUID id = player.getUniqueID();
        EntityPlayerMP current = online.get(id);
        if (current == null || current.playerNetServerHandler.netManager != player.playerNetServerHandler.netManager
            || !current.playerNetServerHandler.netManager.isChannelOpen()) return;
        try {
            SkinNetwork.acknowledge(current);
        } catch (RuntimeException | LinkageError failure) {
            say(current, "Skin bridge handshake failed; gameplay continues and synchronization will retry.");
            SkinRestorer.LOGGER.warn("Skin bridge handshake failed for {}", current.getCommandSenderName(), failure);
            return;
        }
        if (!bridges.add(id)) return;
        for (EntityPlayerMP target : online.values()) {
            SkinState state = states.get(target.getUniqueID());
            SkinTexture texture = state == null ? SkinTexture.fromProfile(target.getGameProfile())
                .orElse(null) : state.texture();
            if (texture != null) {
                sendUpdate(player, new SkinUpdate(target.getUniqueID(), state == null ? 0 : state.revision(), texture));
            }
        }
    }

    public List<String> knownNames() {
        return names.values()
            .stream()
            .filter(name -> !name.isEmpty())
            .distinct()
            .sorted()
            .toList();
    }

    public Target resolve(String input) {
        UUID supplied = null;
        try {
            supplied = UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {}
        if (supplied != null) {
            String name = names.get(supplied);
            if (name == null) throw new IllegalArgumentException("Player UUID is not known to this world");
            return new Target(supplied, name);
        }
        List<Target> matches = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : names.entrySet()) {
            if (entry.getValue()
                .equalsIgnoreCase(input)) matches.add(new Target(entry.getKey(), entry.getValue()));
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException(
                matches.isEmpty() ? "Player has not joined this world; unknown identity"
                    : "Player name is ambiguous; specify a known UUID");
        }
        return matches.get(0);
    }

    public boolean beginCommand(ICommandSender sender, Target target) {
        if (sender instanceof FakePlayer) {
            say(sender, "Simulated players cannot request player skins.");
            return false;
        }
        if (!ready) {
            say(sender, "Skin storage is not ready. Login and gameplay remain available.");
            return false;
        }
        if (saving.contains(target.id())) {
            say(sender, "The previous skin update is being saved; try again shortly.");
            return false;
        }
        if (!(sender instanceof EntityPlayerMP player) || sender.canCommandSenderUseCommand(2, "skin")) return true;
        UUID id = player.getUniqueID();
        long now = System.nanoTime();
        long deadline = cooldowns.getOrDefault(id, 0L);
        if (deadline - now > 0) {
            say(
                sender,
                "Wait " + ((deadline - now + 999999999L) / 1000000000L) + " seconds before another skin command.");
            return false;
        }
        cooldowns.put(id, now + TimeUnit.SECONDS.toNanos(config.cooldownSeconds()));
        return true;
    }

    public void set(ICommandSender sender, Target target, String source) {
        MojangClient.validateUsername(source);
        long token = nextOperation(target.id());
        SkinState previous = states.get(target.id());
        SkinState requested = new SkinState(
            target.id(),
            target.name(),
            source,
            previous == null ? null : previous.texture(),
            revision);
        say(sender, "Loading Mojang skin for " + source + "...");
        fetch(requested, token, sender, true);
    }

    public void reset(ICommandSender sender, Target target) {
        long token = nextOperation(target.id());
        SkinState previous = states.get(target.id());
        SkinState reset = new SkinState(
            target.id(),
            target.name(),
            null,
            previous == null ? null : previous.texture(),
            ++revision);
        persist(reset, token, sender, true, () -> {
            say(sender, "Skin override removed for " + label(target.id(), target.name()) + ".");
            fetch(reset, token, sender, true);
        });
    }

    public void refresh(ICommandSender sender, Target target) {
        long token = nextOperation(target.id());
        SkinState previous = states.get(target.id());
        SkinState requested = previous == null ? new SkinState(target.id(), target.name(), null, null, revision)
            : new SkinState(
                target.id(),
                target.name(),
                previous.selectedName(),
                previous.texture(),
                previous.revision());
        say(sender, "Refreshing Mojang skin...");
        fetch(requested, token, sender, true);
    }

    private void fetch(SkinState requested, long token, ICommandSender sender, boolean force) {
        MojangClient client = mojang;
        String source = requested.selectedName() == null ? requested.playerName() : requested.selectedName();
        if (source.isEmpty()) {
            if (sender != null)
                say(sender, "This player's name is unavailable; automatic restoration resumes on their next login.");
            return;
        }
        client.fetch(source, force)
            .thenApply(texture -> {
                GameProfile snapshot = new GameProfile(requested.id(), requested.playerName());
                snapshot.getProperties()
                    .put("textures", texture.toProperty());
                if (!minecraft.func_147130_as()
                    .getTextures(snapshot, true)
                    .containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    throw new IllegalArgumentException("Mojang texture was rejected by this server's Authlib");
                }
                return texture;
            })
            .whenComplete((texture, failure) -> complete(() -> {
                if (!current(requested.id(), token)) return;
                if (failure != null) {
                    Throwable cause = failure;
                    while ((cause instanceof java.util.concurrent.CompletionException
                        || cause instanceof java.util.concurrent.ExecutionException) && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    String reason = cause.getMessage();
                    if (reason == null || reason.isBlank()) {
                        reason = "The skin service is temporarily unavailable; please try again later.";
                    }
                    String message = "Skin lookup failed; current skin kept. " + reason;
                    say(sender != null ? sender : online.get(requested.id()), message);
                    SkinRestorer.LOGGER.warn("Unable to refresh {}'s Mojang skin: {}", requested.playerName(), reason);
                    return;
                }
                SkinState result = new SkinState(
                    requested.id(),
                    requested.playerName(),
                    requested.selectedName(),
                    texture,
                    ++revision);
                persist(result, token, sender, true, () -> {
                    EntityPlayerMP player = online.get(result.id());
                    if (player != null) apply(player, result);
                    if (sender != null) say(
                        sender,
                        "Skin saved for " + label(result.id(), result.playerName())
                            + (player == null ? "; applies on next login." : "."));
                });
            }));
    }

    private void persist(SkinState state, long token, ICommandSender sender, boolean updateState, Runnable committed) {
        if (!current(state.id(), token)) return;
        if (!saving.add(state.id())) {
            afterSave.computeIfAbsent(state.id(), ignored -> new ArrayList<>())
                .add(() -> persist(state, token, sender, updateState, committed));
            return;
        }
        try {
            disk.execute(() -> {
                try {
                    store.write(state);
                    complete(() -> {
                        // Preserve a durable choice even if its player disconnected during the write.
                        if (updateState) states.put(state.id(), state);
                        saving.remove(state.id());
                        try {
                            if (current(state.id(), token)) committed.run();
                        } finally {
                            finishSave(state.id());
                        }
                    });
                } catch (Exception | LinkageError error) {
                    complete(() -> {
                        saving.remove(state.id());
                        try {
                            if (current(state.id(), token)) {
                                say(
                                    sender != null ? sender : online.get(state.id()),
                                    "Skin change was not saved; current selection kept. " + error.getMessage());
                            }
                            SkinRestorer.LOGGER.error("Unable to save skin for {}", state.playerName(), error);
                        } finally {
                            finishSave(state.id());
                        }
                    });
                }
            });
        } catch (RejectedExecutionException error) {
            saving.remove(state.id());
            try {
                say(sender != null ? sender : online.get(state.id()), "Skin storage queue is full; try again later.");
                SkinRestorer.LOGGER.warn("Skin storage queue is full");
            } finally {
                finishSave(state.id());
            }
        }
    }

    private void finishSave(UUID id) {
        List<Runnable> waiting = afterSave.remove(id);
        if (waiting != null) for (Runnable task : waiting) run(task);
    }

    private void apply(EntityPlayerMP player, SkinState state) {
        if (state.texture() == null) return;
        SkinTexture previous = SkinTexture.fromProfile(player.getGameProfile())
            .orElse(null);
        if (!Objects.equals(previous, state.texture())) {
            player.getGameProfile()
                .getProperties()
                .removeAll("textures");
            player.getGameProfile()
                .getProperties()
                .put(
                    "textures",
                    state.texture()
                        .toProperty());
            try {
                ObserverRefresh.refresh(player);
            } catch (RuntimeException | LinkageError failure) {
                say(
                    player,
                    "Skin saved, but some displays could not refresh. Gameplay continues; try refreshing later.");
                SkinRestorer.LOGGER.warn("Observer skin refresh failed for {}", player.getCommandSenderName(), failure);
            }
        }
        broadcast(state);
    }

    private void broadcast(SkinState state) {
        for (UUID id : bridges) {
            EntityPlayerMP player = online.get(id);
            if (player != null) sendUpdate(player, new SkinUpdate(state.id(), state.revision(), state.texture()));
        }
    }

    private void sendUpdate(EntityPlayerMP player, SkinUpdate update) {
        try {
            SkinNetwork.send(player, update);
        } catch (RuntimeException | LinkageError failure) {
            say(player, "Skin display synchronization failed; normal gameplay remains available.");
            SkinRestorer.LOGGER.warn("Skin synchronization failed for {}", player.getCommandSenderName(), failure);
        }
    }

    private boolean current(UUID id, long token) {
        return active && operations.getOrDefault(id, 0L) == token;
    }

    private long nextOperation(UUID id) {
        long token = operations.getOrDefault(id, 0L) + 1;
        operations.put(id, token);
        return token;
    }

    public void reload(ICommandSender sender, java.io.File configFile) {
        try {
            disk.execute(() -> {
                try {
                    SkinConfig loaded = SkinConfig.load(configFile);
                    complete(() -> {
                        MojangClient old = mojang;
                        config = loaded;
                        mojang = createClient(loaded);
                        old.close();
                        say(sender, "SkinRestorer configuration reloaded.");
                    });
                } catch (Exception error) {
                    complete(() -> say(sender, "Cannot reload configuration: " + error.getMessage()));
                }
            });
        } catch (RejectedExecutionException error) {
            say(sender, "Skin storage queue is full; try again later.");
        }
    }

    @Override
    public void close() {
        active = false;
        mojang.close();
        inbound.clear();
        completions.clear();
        online.clear();
        bridges.clear();
        disk.shutdown();
        Thread cleanup = new Thread(() -> {
            try {
                if (!disk.awaitTermination(30, TimeUnit.SECONDS)) {
                    SkinRestorer.LOGGER.warn("Skin storage is still flushing after server stop");
                }
            } catch (InterruptedException error) {
                Thread.currentThread()
                    .interrupt();
            }
            if (store != null) store.close();
        }, "SkinRestorer-Storage-Close");
        cleanup.setDaemon(true);
        cleanup.start();
    }

    public static void say(ICommandSender sender, String message) {
        if (sender == null) return;
        try {
            if (sender instanceof EntityPlayerMP player && !player.playerNetServerHandler.netManager.isChannelOpen())
                return;
            if (!(sender instanceof EntityPlayerMP) && "Rcon".equals(sender.getCommandSenderName())) {
                SkinRestorer.LOGGER.info("[SkinRestorer] " + message);
            }
            sender.addChatMessage(new ChatComponentText("[SkinRestorer] " + message));
        } catch (RuntimeException | LinkageError failure) {
            SkinRestorer.LOGGER.warn("Cannot deliver skin status message; gameplay continues: " + message, failure);
        }
    }

    private static MojangClient createClient(SkinConfig config) {
        return new MojangClient(
            Duration.ofSeconds(config.requestTimeoutSeconds()),
            config.fetchThreads(),
            config.queueCapacity(),
            config.cacheSeconds());
    }

    private static Set<UUID> loadPlayerIds(Path directory) throws IOException {
        Set<UUID> ids = new HashSet<>();
        if (!Files.isDirectory(directory)) return ids;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.dat")) {
            for (Path file : files) {
                String name = file.getFileName()
                    .toString();
                try {
                    ids.add(UUID.fromString(name.substring(0, name.length() - 4)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return ids;
    }

    private static String label(UUID id, String name) {
        return name.isEmpty() ? id.toString() : name;
    }

    public record Target(UUID id, String name) {}
}
