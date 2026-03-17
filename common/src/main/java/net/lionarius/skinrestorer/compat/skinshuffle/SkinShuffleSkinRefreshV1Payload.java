package net.lionarius.skinrestorer.compat.skinshuffle;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SkinShuffleSkinRefreshV1Payload(
        Property textureProperty) implements CustomPacketPayload, SkinShuffleSkinRefreshPayload {

    public static final ResourceLocation PACKET_ID = SkinShuffleCompatibility.resourceLocation("refresh");

    public static void encode(FriendlyByteBuf buf, SkinShuffleSkinRefreshV1Payload value) {
        var textureProperty = value.textureProperty();

        buf.writeUtf(textureProperty.name());
        buf.writeUtf(textureProperty.value());
        buf.writeNullable(textureProperty.signature(), FriendlyByteBuf::writeUtf);
    }

    public static SkinShuffleSkinRefreshV1Payload decode(FriendlyByteBuf buf) {
        return new SkinShuffleSkinRefreshV1Payload(
                new Property(buf.readUtf(), buf.readUtf(), buf.readNullable(FriendlyByteBuf::readUtf)));
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        encode(buf, this);
    }
    
    @Override
    public @NotNull ResourceLocation id() {
        return PACKET_ID;
    }
}
