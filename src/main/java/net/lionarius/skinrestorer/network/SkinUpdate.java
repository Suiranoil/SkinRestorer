package net.lionarius.skinrestorer.network;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.lionarius.skinrestorer.skin.SkinTexture;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class SkinUpdate implements IMessage {

    private static final int MAX_STRING_BYTES = 24000;

    public UUID playerId;
    public long revision;
    public SkinTexture texture;
    public transient net.minecraft.network.NetworkManager connection;
    public transient boolean valid = true;

    public SkinUpdate() {}

    public SkinUpdate(UUID playerId, long revision, SkinTexture texture) {
        this.playerId = playerId;
        this.revision = revision;
        this.texture = texture;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        try {
            playerId = new UUID(buffer.readLong(), buffer.readLong());
            revision = buffer.readLong();
            if (revision < 0) throw new IllegalArgumentException("Invalid skin revision");
            texture = buffer.readBoolean() ? new SkinTexture(readString(buffer), readString(buffer)) : null;
            if (buffer.isReadable()) throw new IllegalArgumentException("Unexpected skin packet data");
        } catch (RuntimeException | LinkageError failure) {
            valid = false;
            texture = null;
            net.lionarius.skinrestorer.SkinRestorer.LOGGER
                .warn("Ignoring invalid skin update; gameplay continues", failure);
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(playerId.getMostSignificantBits());
        buffer.writeLong(playerId.getLeastSignificantBits());
        buffer.writeLong(revision);
        buffer.writeBoolean(texture != null);
        if (texture != null) {
            writeString(buffer, texture.value());
            writeString(buffer, texture.signature());
        }
    }

    private static String readString(ByteBuf buffer) {
        int length = buffer.readUnsignedShort();
        if (length < 1 || length > MAX_STRING_BYTES || length > buffer.readableBytes()) {
            throw new IllegalArgumentException("Invalid skin property length");
        }
        byte[] value = new byte[length];
        buffer.readBytes(value);
        return new String(value, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteBuf buffer, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 1 || bytes.length > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("Invalid skin property length");
        }
        buffer.writeShort(bytes.length);
        buffer.writeBytes(bytes);
    }
}
