package net.lionarius.skinrestorer.neoforge.compat.skinshuffle;

import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.lionarius.skinrestorer.compat.skinshuffle.*;
import net.neoforged.neoforge.network.NetworkConstants;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.event.EventNetworkChannel;

public final class SkinShufflePacketHandler {
    private SkinShufflePacketHandler() {
    }
    
    // there is no need for handshake channel but register it to not allow other mods to use it
    private static final EventNetworkChannel HANDSHAKE_INSTANCE = NetworkRegistry.ChannelBuilder
            .named(SkinShuffleHandshakePayload.PACKET_ID)
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr("1"))
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr("1"))
            .networkProtocolVersion(() -> NetworkConstants.NETVERSION)
            .eventNetworkChannel();
    
    private static final EventNetworkChannel SKIN_REFRESH_V1_INSTANCE = NetworkRegistry.ChannelBuilder
            .named(SkinShuffleSkinRefreshV1Payload.PACKET_ID)
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr("1"))
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr("1"))
            .networkProtocolVersion(() -> NetworkConstants.NETVERSION)
            .eventNetworkChannel();
    
    private static final EventNetworkChannel SKIN_REFRESH_V2_INSTANCE = NetworkRegistry.ChannelBuilder
            .named(SkinShuffleSkinRefreshV2Payload.PACKET_ID)
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr("1"))
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr("1"))
            .networkProtocolVersion(() -> NetworkConstants.NETVERSION)
            .eventNetworkChannel();
    
    static void initialize() {
        SKIN_REFRESH_V1_INSTANCE.addListener(SkinShufflePacketHandler::skinRefreshV1Listener);
        SKIN_REFRESH_V2_INSTANCE.addListener(SkinShufflePacketHandler::skinRefreshV2Listener);
    }
    
    private static void skinRefreshV1Listener(NetworkEvent.ServerCustomPayloadEvent event) {
        var payload = SkinShuffleSkinRefreshV1Payload.decode(event.getPayload());
        handleSkinRefreshPacket(payload, event.getSource());
    }
    
    private static void skinRefreshV2Listener(NetworkEvent.ServerCustomPayloadEvent event) {
        var payload = SkinShuffleSkinRefreshV2Payload.decode(event.getPayload());
        handleSkinRefreshPacket(payload, event.getSource());
    }
    
    private static void handleSkinRefreshPacket(SkinShuffleSkinRefreshPayload payload, NetworkEvent.Context context) {
        var player = context.getSender();
        SkinShuffleCompatibility.handleSkinRefresh(player.getServer(), player, payload);
    }
}
