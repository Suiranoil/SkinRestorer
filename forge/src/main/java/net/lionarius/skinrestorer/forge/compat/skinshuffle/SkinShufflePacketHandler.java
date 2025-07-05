package net.lionarius.skinrestorer.forge.compat.skinshuffle;

import io.netty.buffer.Unpooled;
import net.lionarius.skinrestorer.compat.skinshuffle.*;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;

public class SkinShufflePacketHandler {

    private static final EventNetworkChannel HANDSHAKE_INSTANCE = ChannelBuilder
            .named(SkinShuffleHandshakePayload.PACKET_ID)
            .optional()
            .eventNetworkChannel();
    
    private static final EventNetworkChannel SKIN_REFRESH_V1_INSTANCE = ChannelBuilder
            .named(SkinShuffleSkinRefreshV1Payload.PACKET_ID)
            .optional()
            .eventNetworkChannel()
            .addListener(SkinShufflePacketHandler::skinRefreshV1Listener);
    
    private static final EventNetworkChannel SKIN_REFRESH_V2_INSTANCE = ChannelBuilder
            .named(SkinShuffleSkinRefreshV2Payload.PACKET_ID)
            .optional()
            .eventNetworkChannel()
            .addListener(SkinShufflePacketHandler::skinRefreshV2Listener);

    protected static void initialize() {
        // NO-OP
    }
    
    private SkinShufflePacketHandler() {
    }

    public static void sendHandshake(Connection connection) {
        HANDSHAKE_INSTANCE.send(new FriendlyByteBuf(Unpooled.buffer(0, 0)), connection);
    }
    
    private static void skinRefreshV1Listener(CustomPayloadEvent event) {
        var payload = SkinShuffleSkinRefreshV1Payload.decode(event.getPayload());
        handleSkinRefreshPacket(payload, event.getSource());
    }

    private static void skinRefreshV2Listener(CustomPayloadEvent event) {
        var payload = SkinShuffleSkinRefreshV2Payload.decode(event.getPayload());
        handleSkinRefreshPacket(payload, event.getSource());
    }

    private static void handleSkinRefreshPacket(SkinShuffleSkinRefreshPayload payload, CustomPayloadEvent.Context context) {
        var sender = context.getSender();
        
        if (!context.isServerSide() || sender == null)
            return;
        
        SkinShuffleCompatibility.handleSkinRefresh(sender.getServer(), sender, payload);
    }
}
