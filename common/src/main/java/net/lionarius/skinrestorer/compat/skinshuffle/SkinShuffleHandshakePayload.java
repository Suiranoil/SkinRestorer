package net.lionarius.skinrestorer.compat.skinshuffle;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record SkinShuffleHandshakePayload() implements CustomPacketPayload {
    
    public static final SkinShuffleHandshakePayload INSTANCE = new SkinShuffleHandshakePayload();
    
    public static final CustomPacketPayload.Type<SkinShuffleHandshakePayload> PACKET_ID = new CustomPacketPayload.Type<>(SkinShuffleCompatibility.resourceLocation("handshake"));
    public static final StreamCodec<FriendlyByteBuf, SkinShuffleHandshakePayload> PACKET_CODEC = StreamCodec.unit(INSTANCE);
    
    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
