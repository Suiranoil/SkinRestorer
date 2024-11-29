package net.lionarius.skinrestorer.fabric.platform;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleHandshakePayload;
import net.lionarius.skinrestorer.platform.services.CompatibilityHelper;
import net.minecraft.server.level.ServerPlayer;

public final class FabricCompatibilityHelper implements CompatibilityHelper {
    @Override
    public void skinShuffle_sendHandshake(ServerPlayer player) {
        ServerPlayNetworking.send(player, SkinShuffleHandshakePayload.INSTANCE);
    }
}
