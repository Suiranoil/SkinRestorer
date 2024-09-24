package net.lionarius.skinrestorer.platform.services;

import java.nio.file.Path;

public interface PlatformHelper {
    
    String getPlatformName();
    
    boolean isModLoaded(String modId);
    
    Path getConfigDirectory();
}
