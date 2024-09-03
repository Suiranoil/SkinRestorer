package net.lionarius.skinrestorer.platform;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.platform.services.PlatformHelper;

import java.util.ServiceLoader;

public final class Services {
    
    private Services() {}

    public final static PlatformHelper PLATFORM = load(PlatformHelper.class);
    
    private static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        SkinRestorer.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
