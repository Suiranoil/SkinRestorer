package net.lionarius.skinrestorer.compat.skinshuffle;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SkinShuffleSkinRefreshV2Payload(Property textureProperty) implements SkinShuffleSkinRefreshPayload {

    public static final ResourceLocation PACKET_ID = SkinShuffleCompatibility.resourceLocation("skin_refresh");

    public static void encode(FriendlyByteBuf buf, SkinShuffleSkinRefreshV2Payload value) {
        var textureProperty = value.textureProperty();

        buf.writeBoolean(textureProperty.hasSignature());
        buf.writeUtf(textureProperty.getName());
        buf.writeUtf(textureProperty.getValue());
        if (textureProperty.hasSignature()) {
            assert textureProperty.getSignature() != null;

            buf.writeUtf(textureProperty.getSignature());
        }
    }

    public static SkinShuffleSkinRefreshV2Payload decode(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return new SkinShuffleSkinRefreshV2Payload(new Property(buf.readUtf(), buf.readUtf(), buf.readUtf()));
        }
        return new SkinShuffleSkinRefreshV2Payload(new Property(buf.readUtf(), buf.readUtf(), null));
    }
}
