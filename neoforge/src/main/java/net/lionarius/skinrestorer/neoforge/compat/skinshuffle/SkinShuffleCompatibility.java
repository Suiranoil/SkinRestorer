package net.lionarius.skinrestorer.neoforge.compat.skinshuffle;

import net.lionarius.skinrestorer.SkinRestorer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

public final class SkinShuffleCompatibility {

    private SkinShuffleCompatibility() {}

    public static void initialize() {
        NeoForge.EVENT_BUS.register(SkinShuffleGameEventHandler.class);

        final var mod = ModList.get().getModContainerById(SkinRestorer.MOD_ID).get();
        mod.getEventBus().register(SkinShuffleModEventHandler.class);
    }
}
