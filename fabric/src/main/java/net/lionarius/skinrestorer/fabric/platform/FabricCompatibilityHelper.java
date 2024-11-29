package net.lionarius.skinrestorer.fabric.platform;

import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleHandshakePayload;
import net.lionarius.skinrestorer.platform.services.CompatibilityHelper;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

public final class FabricCompatibilityHelper implements CompatibilityHelper {
    @Override
    public void skinShuffle_sendHandshake(ServerPlayer player) {
        player.connection.send(new ServerboundCustomPayloadPacket(SkinShuffleHandshakePayload.INSTANCE));
    }
}
