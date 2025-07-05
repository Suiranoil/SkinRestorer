package net.lionarius.skinrestorer.util;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import net.lionarius.skinrestorer.mixin.ChunkMapAccessor;
import net.lionarius.skinrestorer.mixin.TrackedEntityAccessorInvoker;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.biome.BiomeManager;

import java.util.*;

public final class PlayerUtils {
    
    public static final String TEXTURES_KEY = "textures";
    
    private PlayerUtils() {}
    
    public static Component createPlayerListComponent(Collection<ServerPlayer> players) {
        var component = Component.empty();
        int index = 0;
        for (var player : players) {
            component.append(Objects.requireNonNull(player.getDisplayName()));
            index++;
            if (index < players.size())
                component.append(", ");
        }
        return component;
    }
    
    public static boolean isFakePlayer(ServerPlayer player) {
        return player.getClass() != ServerPlayer.class; // if the player isn't a server player entity, it must be someone's fake player
    }
    
    public static void refreshPlayer(ServerPlayer player) {
        ServerLevel serverLevel = player.getLevel();
        PlayerList playerList = serverLevel.getServer().getPlayerList();
        ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;
        
        playerList.broadcastAll(new ClientboundBundlePacket(
                List.of(
                        new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())),
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(player))
                )
        ));
        
        var trackedEntity = (TrackedEntityAccessorInvoker) ((ChunkMapAccessor) chunkMap).getEntityMap().get(player.getId());
        if (trackedEntity != null) {
            var seenBy = Set.copyOf(trackedEntity.getSeenBy());
            for (var observerConnection : seenBy) {
                var observer = observerConnection.getPlayer();
                trackedEntity.invokeRemovePlayer(observer);
                
                var trackedObserverEntity = (TrackedEntityAccessorInvoker) ((ChunkMapAccessor) chunkMap).getEntityMap().get(observer.getId());
                if (trackedObserverEntity != null) {
                    trackedObserverEntity.invokeRemovePlayer(player);
                    trackedObserverEntity.invokeUpdatePlayer(player);
                }
                trackedEntity.invokeUpdatePlayer(observer);
            }
        }
        
        if (!player.isDeadOrDying()) {
            player.connection.send(
                    new ClientboundRespawnPacket(
                            player.getLevel().dimensionTypeId(),
                            player.getLevel().dimension(),
                            BiomeManager.obfuscateSeed(player.getLevel().getSeed()),
                            player.gameMode.getGameModeForPlayer(),
                            player.gameMode.getPreviousGameModeForPlayer(),
                            player.getLevel().isDebug(),
                            player.getLevel().isFlat(),
                            (byte) 3,
                            player.getLastDeathLocation()
                    )
            );
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            var vehicle = player.getVehicle();
            if (vehicle != null)
                player.connection.send(new ClientboundSetPassengersPacket(vehicle));
            if (!player.getPassengers().isEmpty())
                player.connection.send(new ClientboundSetPassengersPacket(player));
            
            player.onUpdateAbilities();
            player.giveExperiencePoints(0);
            playerList.sendPlayerPermissionLevel(player);
            playerList.sendLevelInfo(player, serverLevel);
            playerList.sendAllPlayerInfo(player);
            PlayerUtils.sendActivePlayerEffects(player);
        }
    }
    
    private static void sendActivePlayerEffects(ServerPlayer player) {
        for (var effect : player.getActiveEffects()) {
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
        }
    }
    
    public static GameProfile cloneGameProfile(GameProfile profile) {
        var newProfile = new GameProfile(profile.getId(), profile.getName());
        newProfile.getProperties().putAll(profile.getProperties());
        
        return newProfile;
    }
    
    public static Property getPlayerSkin(GameProfile profile) {
        return Iterables.getFirst(profile.getProperties().get(TEXTURES_KEY), null);
    }
    
    public static void applyRestoredSkin(GameProfile profile, Property skin) {
        profile.getProperties().removeAll(TEXTURES_KEY);
        
        if (skin != null)
            profile.getProperties().put(TEXTURES_KEY, skin);
    }
    
    public static boolean areSkinPropertiesEquals(Property x, Property y) {
        if (x == y)
            return true;
        
        if (x == null || y == null)
            return false;
        
        if (x.equals(y))
            return true;
        
        JsonObject xJson = JsonUtils.skinPropertyToJson(x);
        JsonObject yJson = JsonUtils.skinPropertyToJson(y);
        
        if (xJson == null || yJson == null)
            return false;
        
        return xJson.equals(yJson);
    }
    
    public static GameProfile toProfile(MinecraftProfilePropertiesResponse response) {
        final GameProfile profile = new GameProfile(response.getId(), response.getName());
        profile.getProperties().putAll(response.getProperties());
        return profile;
    }
}
