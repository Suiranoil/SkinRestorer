package net.lionarius.skinrestorer.compat.skinshuffle;

import net.minecraft.resources.ResourceLocation;

public record SkinShuffleHandshakePayload() {

    public static final SkinShuffleHandshakePayload INSTANCE = new SkinShuffleHandshakePayload();

    public static final ResourceLocation PACKET_ID = SkinShuffleCompatibility.resourceLocation("handshake");
}
