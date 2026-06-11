package net.lionarius.skinrestorer.compat.skinshuffle;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SkinShuffleHandshakePayload() implements CustomPacketPayload {
    public static final SkinShuffleHandshakePayload INSTANCE = new SkinShuffleHandshakePayload();

    public static final ResourceLocation PACKET_ID = SkinShuffleCompatibility.resourceLocation("handshake");

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        // NO-OP
    }

    @Override
    public @NotNull ResourceLocation id() {
        return PACKET_ID;
    }
}
