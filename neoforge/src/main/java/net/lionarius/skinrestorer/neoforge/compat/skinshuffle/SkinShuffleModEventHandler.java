package net.lionarius.skinrestorer.neoforge.compat.skinshuffle;

import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.lionarius.skinrestorer.compat.skinshuffle.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;

public final class SkinShuffleModEventHandler {
    private SkinShuffleModEventHandler() {}
    
    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1").optional().executesOn(HandlerThread.NETWORK);
        
        registrar
                .playToClient(SkinShuffleHandshakePayload.PACKET_ID, SkinShuffleHandshakePayload.PACKET_CODEC,
                        (payload, context) -> {})
                .playToServer(SkinShuffleSkinRefreshV1Payload.PACKET_ID, SkinShuffleSkinRefreshV1Payload.PACKET_CODEC,
                        SkinShuffleModEventHandler::handleSkinRefreshPacket)
                .playToServer(SkinShuffleSkinRefreshV2Payload.PACKET_ID, SkinShuffleSkinRefreshV2Payload.PACKET_CODEC,
                        SkinShuffleModEventHandler::handleSkinRefreshPacket);
    }
    
    private static void handleSkinRefreshPacket(SkinShuffleSkinRefreshPayload payload, IPayloadContext context) {
        var player = (ServerPlayer) context.player();
        SkinShuffleCompatibility.handleSkinRefresh(player.getServer(), player, payload);
    }
}
