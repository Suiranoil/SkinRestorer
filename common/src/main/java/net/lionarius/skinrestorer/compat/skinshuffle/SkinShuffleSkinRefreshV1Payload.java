package net.lionarius.skinrestorer.compat.skinshuffle;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SkinShuffleSkinRefreshV1Payload(Property textureProperty) implements SkinShuffleSkinRefreshPayload {

    public static final ResourceLocation PACKET_ID = SkinShuffleCompatibility.resourceLocation("refresh");

    public static void encode(FriendlyByteBuf buf, SkinShuffleSkinRefreshV1Payload value) {
        var textureProperty = value.textureProperty();

        buf.writeUtf(textureProperty.getName());
        buf.writeUtf(textureProperty.getValue());
        buf.writeNullable(textureProperty.getSignature(), FriendlyByteBuf::writeUtf);
    }

    public static SkinShuffleSkinRefreshV1Payload decode(FriendlyByteBuf buf) {
        return new SkinShuffleSkinRefreshV1Payload(
                new Property(buf.readUtf(), buf.readUtf(), buf.readNullable(FriendlyByteBuf::readUtf)));
    }
}
