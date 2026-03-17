package net.lionarius.skinrestorer.forge.compat.skinshuffle;

import io.netty.buffer.Unpooled;
import net.lionarius.skinrestorer.compat.skinshuffle.*;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

public class SkinShufflePacketHandler {

    private static final EventNetworkChannel HANDSHAKE_INSTANCE = NetworkRegistry.ChannelBuilder.named(
                    SkinShuffleHandshakePayload.PACKET_ID)
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(""))
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(""))
            .networkProtocolVersion(() -> "")
            .eventNetworkChannel();

    private static final EventNetworkChannel SKIN_REFRESH_V1_INSTANCE = NetworkRegistry.ChannelBuilder.named(
                    SkinShuffleSkinRefreshV1Payload.PACKET_ID)
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(""))
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(""))
            .networkProtocolVersion(() -> "")
            .eventNetworkChannel();

    private static final EventNetworkChannel SKIN_REFRESH_V2_INSTANCE = NetworkRegistry.ChannelBuilder.named(
                    SkinShuffleSkinRefreshV2Payload.PACKET_ID)
            .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(""))
            .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(""))
            .networkProtocolVersion(() -> "")
            .eventNetworkChannel();

    protected static void initialize() {
        SKIN_REFRESH_V1_INSTANCE.addListener(SkinShufflePacketHandler::skinRefreshV1Listener);
        SKIN_REFRESH_V2_INSTANCE.addListener(SkinShufflePacketHandler::skinRefreshV2Listener);
    }

    private SkinShufflePacketHandler() {}

    public static void sendHandshake(Connection connection) {
        connection.send(new ClientboundCustomPayloadPacket(
                SkinShuffleHandshakePayload.PACKET_ID, new FriendlyByteBuf(Unpooled.buffer(0, 0))));
    }

    private static void skinRefreshV1Listener(NetworkEvent.ClientCustomPayloadEvent event) {
        var payload = SkinShuffleSkinRefreshV1Payload.decode(event.getPayload());
        handleSkinRefreshPacket(payload, event.getSource().get());
        event.getSource().get().setPacketHandled(true);
    }

    private static void skinRefreshV2Listener(NetworkEvent.ClientCustomPayloadEvent event) {
        var payload = SkinShuffleSkinRefreshV2Payload.decode(event.getPayload());
        handleSkinRefreshPacket(payload, event.getSource().get());
        event.getSource().get().setPacketHandled(true);
    }

    private static void handleSkinRefreshPacket(SkinShuffleSkinRefreshPayload payload, NetworkEvent.Context context) {
        var sender = context.getSender();

        if (!context.getDirection().getReceptionSide().isServer() || sender == null) return;

        SkinShuffleCompatibility.handleSkinRefresh(sender.getServer(), sender, payload);
    }
}
