package net.lionarius.skinrestorer.fabric.compat.skinshuffle;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleSkinRefreshPayload;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleSkinRefreshV1Payload;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleSkinRefreshV2Payload;
import net.lionarius.skinrestorer.fabric.SkinRestorerFabric;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class SkinShuffleCompatibility {
    
    private SkinShuffleCompatibility() {}
    
    public static void initialize() {
        if (!SkinRestorerFabric.FABRIC_API_LOADED) {
            SkinRestorer.LOGGER.warn("fabric-api is not loaded, SkinShuffle compatibility will not be available");
            return;
        }
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility.onPlayerJoin(handler.getPlayer()));
        
        ServerPlayNetworking.registerGlobalReceiver(SkinShuffleSkinRefreshV1Payload.PACKET_ID, SkinShuffleCompatibility::handleSkinRefreshV1Packet);
        
        ServerPlayNetworking.registerGlobalReceiver(SkinShuffleSkinRefreshV2Payload.PACKET_ID, SkinShuffleCompatibility::handleSkinRefreshV2Packet);
    }
    
    private static void handleSkinRefreshV1Packet(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, FriendlyByteBuf buf, PacketSender sender) {
        var payload = SkinShuffleSkinRefreshV1Payload.decode(buf);
        SkinShuffleCompatibility.handleSkinRefreshPacket(payload, server, player);
    }
    
    private static void handleSkinRefreshV2Packet(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl listener, FriendlyByteBuf buf, PacketSender sender) {
        var payload = SkinShuffleSkinRefreshV2Payload.decode(buf);
        SkinShuffleCompatibility.handleSkinRefreshPacket(payload, server, player);
    }
    
    private static void handleSkinRefreshPacket(SkinShuffleSkinRefreshPayload payload, MinecraftServer server, ServerPlayer player) {
        net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility.handleSkinRefresh(server, player, payload);
    }
}
