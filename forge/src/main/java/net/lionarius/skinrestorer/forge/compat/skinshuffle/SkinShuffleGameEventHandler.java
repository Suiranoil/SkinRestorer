package net.lionarius.skinrestorer.forge.compat.skinshuffle;

import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SkinShuffleGameEventHandler {
    
    private SkinShuffleGameEventHandler() {}
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        SkinShuffleCompatibility.onPlayerJoin((ServerPlayer) event.getEntity());
    }
}
