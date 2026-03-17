package net.lionarius.skinrestorer.forge.compat.skinshuffle;

import net.minecraftforge.common.MinecraftForge;

public final class SkinShuffleCompatibility {

    private SkinShuffleCompatibility() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.register(SkinShuffleGameEventHandler.class);

        SkinShufflePacketHandler.initialize();
    }
}
