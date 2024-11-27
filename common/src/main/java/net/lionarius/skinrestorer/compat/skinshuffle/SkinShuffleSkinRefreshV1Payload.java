package net.lionarius.skinrestorer.compat.skinshuffle;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record SkinShuffleSkinRefreshV1Payload(
        Property textureProperty) implements CustomPacketPayload, SkinShuffleSkinRefreshPayload {
    
    public static final CustomPacketPayload.Type<SkinShuffleSkinRefreshV1Payload> PACKET_ID = new CustomPacketPayload.Type<>(SkinShuffleCompatibility.resourceLocation("refresh"));
    public static final StreamCodec<FriendlyByteBuf, SkinShuffleSkinRefreshV1Payload> PACKET_CODEC = StreamCodec.of(
            SkinShuffleSkinRefreshV1Payload::encode,
            SkinShuffleSkinRefreshV1Payload::decode
    );
    
    private static void encode(FriendlyByteBuf buf, SkinShuffleSkinRefreshV1Payload value) {
        var textureProperty = value.textureProperty();
        
        buf.writeUtf(textureProperty.name());
        buf.writeUtf(textureProperty.value());
        buf.writeNullable(textureProperty.signature(), FriendlyByteBuf::writeUtf);
    }
    
    private static SkinShuffleSkinRefreshV1Payload decode(FriendlyByteBuf buf) {
        return new SkinShuffleSkinRefreshV1Payload(new Property(buf.readUtf(), buf.readUtf(), buf.readNullable(FriendlyByteBuf::readUtf)));
    }
    
    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
