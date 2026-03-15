package net.lionarius.skinrestorer.neoforge.compat.skinshuffle;

import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class SkinShuffleGameEventHandler {

    private SkinShuffleGameEventHandler() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        SkinShuffleCompatibility.onPlayerJoin((ServerPlayer) event.getEntity());
    }
}
