package net.lionarius.skinrestorer.forge.platform;

import net.lionarius.skinrestorer.forge.compat.skinshuffle.SkinShufflePacketHandler;
import net.lionarius.skinrestorer.platform.services.CompatibilityHelper;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeCompatibilityHelper implements CompatibilityHelper {

    @Override
    public void skinShuffle_sendHandshake(ServerPlayer player) {
        SkinShufflePacketHandler.sendHandshake(player.connection.getConnection());
    }
}
