package net.lionarius.skinrestorer.forge.compat.skinshuffle;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.compat.skinshuffle.*;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;

public class SkinShufflePacketHandler {
    private static final Channel<CustomPacketPayload> INSTANCE = ChannelBuilder.named(
                    SkinRestorer.resourceLocation("skin_shuffle_compat"))
            .optional()
            .payloadChannel()
            .any()
            .clientbound()
            .add(
                    SkinShuffleHandshakePayload.PACKET_ID,
                    SkinShuffleHandshakePayload.PACKET_CODEC,
                    (payload, context) -> {})
            .serverbound()
            .add(
                    SkinShuffleSkinRefreshV1Payload.PACKET_ID,
                    SkinShuffleSkinRefreshV1Payload.PACKET_CODEC,
                    SkinShufflePacketHandler::handleSkinRefreshPacket)
            .add(
                    SkinShuffleSkinRefreshV2Payload.PACKET_ID,
                    SkinShuffleSkinRefreshV2Payload.PACKET_CODEC,
                    SkinShufflePacketHandler::handleSkinRefreshPacket)
            .build();

    private SkinShufflePacketHandler() {}

    protected static void initialize() {
        // NO-OP
    }

    public static void sendHandshake(Connection connection) {
        INSTANCE.send(SkinShuffleHandshakePayload.INSTANCE, connection);
    }

    private static void handleSkinRefreshPacket(
            SkinShuffleSkinRefreshPayload payload, CustomPayloadEvent.Context context) {
        var sender = context.getSender();

        if (!context.isServerSide() || sender == null) return;

        SkinShuffleCompatibility.handleSkinRefresh(sender.getServer(), sender, payload);
    }
}
