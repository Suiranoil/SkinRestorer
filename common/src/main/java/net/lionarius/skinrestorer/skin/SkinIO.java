package net.lionarius.skinrestorer.skin;

import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class SkinIO {
    
    public static final String FILE_EXTENSION = ".json";
    
    private final Path savePath;
    
    public SkinIO(Path savePath) {
        this.savePath = savePath;
    }
    
    public boolean skinExists(UUID uuid) {
        return Files.exists(savePath.resolve(SkinIO.uuidToFilename(uuid)));
    }
    
    public SkinValue loadSkin(UUID uuid) {
        try {
            return SkinIO.loadSkin(savePath.resolve(SkinIO.uuidToFilename(uuid)));
        } catch (Exception e) {
            return SkinValue.EMPTY;
        }
    }
    
    private static SkinValue loadSkin(Path file) {
        var json = FileUtils.readFile(file);
        try {
            return JsonUtils.fromJson(json, SkinValue.class);
        } catch (Exception e) {
            return SkinValue.EMPTY;
        }
    }
    
    public void saveSkin(UUID uuid, SkinValue skin) {
        FileUtils.writeFile(savePath.resolve(SkinIO.uuidToFilename(uuid)), JsonUtils.toJson(skin));
    }
    
    public void deleteSkin(UUID uuid) {
        FileUtils.deleteFile(savePath.resolve(SkinIO.uuidToFilename(uuid)));
    }
    
    private static String uuidToFilename(UUID uuid) {
        return uuid + FILE_EXTENSION;
    }
}
