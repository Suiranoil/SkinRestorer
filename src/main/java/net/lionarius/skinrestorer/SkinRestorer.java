package net.lionarius.skinrestorer;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class SkinRestorer implements DedicatedServerModInitializer {

    private static SkinStorage skinStorage;

    public static SkinStorage getSkinStorage() {
        return skinStorage;
    }

    @Override
    public void onInitializeServer() {
        skinStorage = new SkinStorage(new SkinIO(FabricLoader.getInstance().getConfigDir().resolve("skinrestorer")));
    }
}
