package net.lionarius.skinrestorer.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.lionarius.skinrestorer.SkinRestorer;

public final class SkinRestorerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SkinRestorer.onInitialize();
    }
}
