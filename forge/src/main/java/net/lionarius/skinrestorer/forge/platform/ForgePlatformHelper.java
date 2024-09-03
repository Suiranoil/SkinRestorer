package net.lionarius.skinrestorer.forge.platform;

import net.lionarius.skinrestorer.platform.services.PlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public final class ForgePlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "forge";
    }
    
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
    
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}
