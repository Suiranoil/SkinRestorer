package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.Config;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;
import net.lionarius.skinrestorer.translation.Translation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileUtils {
    
    private FileUtils() {}
    
    public static String readResource(String name) {
        try (var stream = SkinRestorer.class.getResourceAsStream(name)) {
            if (stream == null)
                return null;
            
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return StringUtils.readString(reader);
            }
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("Failed to read resource", e);
            return null;
        }
    }
    
    public static String readFile(Path file) {
        try {
            if (!Files.exists(file))
                return null;
            
            return Files.readString(file);
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Failed to read file", e);
            return null;
        }
    }
    
    public static void writeFile(Path file, String content) {
        try {
            var parent = file.getParent();
            if (parent != null)
                Files.createDirectories(parent);
            
            if (!Files.exists(file))
                Files.createFile(file);
            
            Files.writeString(file, content);
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("Failed to write file", e);
        }
    }
    
    public static void deleteFile(Path file) {
        try {
            if (Files.exists(file))
                Files.delete(file);
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("Failed to delete file", e);
        }
    }
}
