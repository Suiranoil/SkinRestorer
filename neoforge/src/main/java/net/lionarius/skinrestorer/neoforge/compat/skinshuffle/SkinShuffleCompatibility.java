package net.lionarius.skinrestorer.neoforge.compat.skinshuffle;

import net.neoforged.neoforge.common.NeoForge;

public final class SkinShuffleCompatibility {

    private SkinShuffleCompatibility() {}

    public static void initialize() {
        NeoForge.EVENT_BUS.register(SkinShuffleGameEventHandler.class);

        SkinShufflePacketHandler.initialize();
    }
}
