package net.lionarius.skinrestorer.config.provider.collection;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Files;

public final class CollectionSkinFile implements CollectionSkinSource, GsonPostProcessable {
    private String path = "";
    private SkinVariant variant = SkinVariant.CLASSIC;
    
    @Override
    public @Nullable URI uri() {
        if (this.path.isEmpty())
            return null;
        
        try {
            var filePath = SkinRestorer.getConfigDir().resolve(this.path);
            
            if (!Files.exists(filePath)) {
                SkinRestorer.LOGGER.warn("Skin file does not exist: {}", this.path);
                return null;
            }
            
            if (!Files.isRegularFile(filePath)) {
                SkinRestorer.LOGGER.warn("Skin path is not a file: {}", this.path);
                return null;
            }
            
            if (!this.path.toLowerCase().endsWith(".png")) {
                SkinRestorer.LOGGER.warn("Skin file is not a PNG file: {}", this.path);
                return null;
            }
            
            return filePath.toUri();
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Invalid file path: {}", this.path, e);
            return null;
        }
    }
    
    @Override
    public SkinVariant variant() {
        return this.variant;
    }
    
    @Override
    public void gsonPostProcess() {
        if (this.path == null) {
            this.path = "";
        }
        if (this.variant == null) {
            this.variant = SkinVariant.CLASSIC;
        }
    }
}
