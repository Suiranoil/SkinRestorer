package net.lionarius.skinrestorer.fabric.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.lionarius.skinrestorer.platform.services.PlatformHelper;

import java.nio.file.Path;

public final class FabricPlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
