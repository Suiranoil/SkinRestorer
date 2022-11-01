package net.lionarius.skinrestorer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.biome.source.BiomeAccess;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SkinRestorer implements DedicatedServerModInitializer {

    private static SkinStorage skinStorage;

    public static final Logger LOGGER = LoggerFactory.getLogger("SkinRestorer");

    public static SkinStorage getSkinStorage() {
        return skinStorage;
    }

    @Override
    public void onInitializeServer() {
        skinStorage = new SkinStorage(new SkinIO(FabricLoader.getInstance().getConfigDir().resolve("skinrestorer")));
    }

    public static CompletableFuture<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>> setSkinAsync(MinecraftServer server, Collection<GameProfile> targets, Supplier<Property> skinSupplier) {
        return CompletableFuture.<Pair<Property, Collection<GameProfile>>>supplyAsync(() -> {
            HashSet<GameProfile> acceptedProfiles = new HashSet<>();
            Property skin = skinSupplier.get();
            if (skin == null) {
                SkinRestorer.LOGGER.error("Cannot get the skin for {}", targets.stream().findFirst().orElseThrow());
                return Pair.of(null, Collections.emptySet());
            }

            for (GameProfile profile : targets) {
                SkinRestorer.getSkinStorage().setSkin(profile.getId(), skin);
                acceptedProfiles.add(profile);
            }

            return Pair.of(skin, acceptedProfiles);
        }).<Pair<Collection<ServerPlayerEntity>, Collection<GameProfile>>>thenApplyAsync(pair -> {
            Property skin = pair.left();
            if (skin == null) {
                return Pair.of(Collections.emptySet(), Collections.emptySet());
            }
            Collection<GameProfile> acceptedProfiles = pair.right();
            HashSet<ServerPlayerEntity> acceptedPlayers = new HashSet<>();
            JsonObject newSkinJson = gson.fromJson(new String(Base64.getDecoder().decode(skin.getValue()), StandardCharsets.UTF_8), JsonObject.class);
            newSkinJson.remove("timestamp");
            for (GameProfile profile : acceptedProfiles) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(profile.getId());
                if (player == null) {
                    continue;
                }
                if (arePropertiesEquals(newSkinJson, player.getGameProfile())) {
                    continue;
                }

                applyRestoredSkin(player, skin);
                for (PlayerEntity observer : player.world.getPlayers()) {
                    ServerPlayerEntity observer1 = (ServerPlayerEntity) observer;
                    observer1.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
                    observer1.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player)); // refresh the player information
                    if (player != observer1 && observer1.canSee(player)) {
                        observer1.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                        observer1.networkHandler.sendPacket(new PlayerSpawnS2CPacket(player));
                        observer1.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(player.getId(), player.getDataTracker(), true));
                        observer1.networkHandler.sendPacket(new EntityPositionS2CPacket(player));
                    } else if (player == observer1) {
                        observer1.networkHandler.sendPacket(new PlayerRespawnS2CPacket(
                                observer1.world.getDimensionKey(),
                                observer1.world.getRegistryKey(),
                                BiomeAccess.hashSeed(observer1.getWorld().getSeed()),
                                observer1.interactionManager.getGameMode(),
                                observer1.interactionManager.getPreviousGameMode(),
                                observer1.getWorld().isDebugWorld(),
                                observer1.getWorld().isFlat(),
                                true,
                                Optional.of(GlobalPos.create(observer1.getWorld().getRegistryKey(), observer1.getBlockPos()))
                        ));
                        observer1.requestTeleport(observer1.getX(), observer1.getY(), observer1.getZ());
                        observer1.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(observer1.getInventory().selectedSlot));
                        observer1.sendAbilitiesUpdate();
                        observer1.playerScreenHandler.updateToClient();
                        for (StatusEffectInstance instance : observer1.getStatusEffects()) {
                            observer1.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(observer1.getId(), instance));
                        }
                        observer1.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(player.getId(), player.getDataTracker(), true));
                        observer1.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
                    }
                }
                acceptedPlayers.add(player);
            }
            return Pair.of(acceptedPlayers, acceptedProfiles);
        }, server).orTimeout(10, TimeUnit.SECONDS).exceptionally(e -> Pair.of(Collections.emptySet(), Collections.emptySet()));
    }

    private static void applyRestoredSkin(ServerPlayerEntity playerEntity, Property skin) {
        playerEntity.getGameProfile().getProperties().removeAll("textures");
        playerEntity.getGameProfile().getProperties().put("textures", skin);
    }

    private static final Gson gson = new Gson();

    private static boolean arePropertiesEquals(@NotNull JsonObject x, @NotNull GameProfile y) {
        Property py = y.getProperties().get("textures").stream().findFirst().orElse(null);
        if (py == null) {
            return false;
        } else {
            try {
                JsonObject jy = gson.fromJson(new String(Base64.getDecoder().decode(py.getValue()), StandardCharsets.UTF_8), JsonObject.class);
                jy.remove("timestamp");
                return x.equals(jy);
            } catch (Exception ex) {
                SkinRestorer.LOGGER.info("Can not compare skin", ex);
                return false;
            }
        }
    }
}
