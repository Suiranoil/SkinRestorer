package net.lionarius.skinrestorer.compat.skinshuffle;

import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SkinShuffleSkinRefreshV2Payload(
        Property textureProperty) implements CustomPacketPayload, SkinShuffleSkinRefreshPayload {
    
    public static final ResourceLocation PACKET_ID = SkinShuffleCompatibility.resourceLocation("skin_refresh");

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
    public void write(@NotNull FriendlyByteBuf buf) {
        encode(buf, this);
    }
    
    @Override
    public @NotNull ResourceLocation id() {
        return PACKET_ID;
    }
}
