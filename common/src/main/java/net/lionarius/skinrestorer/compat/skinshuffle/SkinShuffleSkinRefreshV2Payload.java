package net.lionarius.skinrestorer.compat.skinshuffle;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record SkinShuffleSkinRefreshV2Payload(Property textureProperty)
        implements CustomPacketPayload, SkinShuffleSkinRefreshPayload {

    public static final CustomPacketPayload.Type<SkinShuffleSkinRefreshV2Payload> PACKET_ID =
            new CustomPacketPayload.Type<>(SkinShuffleCompatibility.resourceLocation("skin_refresh"));
    public static final StreamCodec<FriendlyByteBuf, SkinShuffleSkinRefreshV2Payload> PACKET_CODEC =
            StreamCodec.of(SkinShuffleSkinRefreshV2Payload::encode, SkinShuffleSkinRefreshV2Payload::decode);

    public static void encode(FriendlyByteBuf buf, SkinShuffleSkinRefreshV2Payload value) {
        var textureProperty = value.textureProperty();

        buf.writeBoolean(textureProperty.hasSignature());
        buf.writeUtf(textureProperty.name());
        buf.writeUtf(textureProperty.value());
        if (textureProperty.hasSignature()) {
            assert textureProperty.signature() != null;

            buf.writeUtf(textureProperty.signature());
        }
    }

    public static SkinShuffleSkinRefreshV2Payload decode(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return new SkinShuffleSkinRefreshV2Payload(new Property(buf.readUtf(), buf.readUtf(), buf.readUtf()));
        }
        return new SkinShuffleSkinRefreshV2Payload(new Property(buf.readUtf(), buf.readUtf(), null));
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
