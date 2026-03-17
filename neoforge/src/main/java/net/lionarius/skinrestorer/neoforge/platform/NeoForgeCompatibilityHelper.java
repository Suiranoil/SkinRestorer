package net.lionarius.skinrestorer.neoforge.platform;

import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleHandshakePayload;
import net.lionarius.skinrestorer.platform.services.CompatibilityHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

public final class NeoForgeCompatibilityHelper implements CompatibilityHelper {

    @Override
    public void skinShuffle_sendHandshake(ServerPlayer player) {
        // we can't use the packet distributor here because
        // neoforge doesn't support sending packets to non-neoforge players
        player.connection
                .getConnection()
                .send(new ClientboundCustomPayloadPacket(SkinShuffleHandshakePayload.INSTANCE));
    }
}
