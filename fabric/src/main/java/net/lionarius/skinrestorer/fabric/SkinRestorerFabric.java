package net.lionarius.skinrestorer.fabric;

import net.fabricmc.api.ModInitializer;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.lionarius.skinrestorer.platform.Services;

public final class SkinRestorerFabric implements ModInitializer {
    public static final boolean FABRIC_API_LOADED = Services.PLATFORM.isModLoaded("fabric-api");

    @Override
    public void onInitialize() {
        SkinRestorer.onInitialize();

        if (SkinShuffleCompatibility.shouldApply())
            net.lionarius.skinrestorer.fabric.compat.skinshuffle.SkinShuffleCompatibility.initialize();
    }
}
