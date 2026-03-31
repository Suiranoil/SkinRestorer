package net.lionarius.skinrestorer.fabric.compat.skinshuffle;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleHandshakePayload;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleSkinRefreshPayload;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleSkinRefreshV1Payload;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleSkinRefreshV2Payload;
import net.lionarius.skinrestorer.fabric.SkinRestorerFabric;

public final class SkinShuffleCompatibility {

    private SkinShuffleCompatibility() {}

    public static void initialize() {
        if (!SkinRestorerFabric.FABRIC_API_LOADED) {
            SkinRestorer.LOGGER.warn("fabric-api is not loaded, SkinShuffle compatibility will not be available");
            return;
        }

        PayloadTypeRegistry.clientboundPlay()
                .register(SkinShuffleHandshakePayload.PACKET_ID, SkinShuffleHandshakePayload.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(SkinShuffleSkinRefreshV1Payload.PACKET_ID, SkinShuffleSkinRefreshV1Payload.PACKET_CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(SkinShuffleSkinRefreshV2Payload.PACKET_ID, SkinShuffleSkinRefreshV2Payload.PACKET_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility.onPlayerJoin(
                        handler.getPlayer()));

        ServerPlayNetworking.registerGlobalReceiver(
                SkinShuffleSkinRefreshV1Payload.PACKET_ID, SkinShuffleCompatibility::handleSkinRefreshPacket);

        ServerPlayNetworking.registerGlobalReceiver(
                SkinShuffleSkinRefreshV2Payload.PACKET_ID, SkinShuffleCompatibility::handleSkinRefreshPacket);
    }

    private static void handleSkinRefreshPacket(
            SkinShuffleSkinRefreshPayload payload, ServerPlayNetworking.Context context) {
        net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility.handleSkinRefresh(
                SkinRestorer.getMinecraftServer(), context.player(), payload);
    }
}
