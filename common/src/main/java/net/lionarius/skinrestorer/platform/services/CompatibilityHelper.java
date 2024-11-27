package net.lionarius.skinrestorer.platform.services;

import net.minecraft.server.level.ServerPlayer;

public interface CompatibilityHelper {
    
    void skinShuffle_sendHandshake(ServerPlayer player);
}
