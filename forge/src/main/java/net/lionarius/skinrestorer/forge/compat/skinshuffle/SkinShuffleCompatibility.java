package net.lionarius.skinrestorer.forge.compat.skinshuffle;

import net.minecraftforge.event.entity.player.PlayerEvent;

public final class SkinShuffleCompatibility {

    private SkinShuffleCompatibility() {}

    public static void initialize() {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(SkinShuffleGameEventHandler::onPlayerLoggedIn);

        SkinShufflePacketHandler.initialize();
    }
}
