package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.Config;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;
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
    
    public static void tryMigrateOldSkinDirectory(Path oldDirectory, Path newDirectory) {
        try {
            try (var stream = Files.list(oldDirectory)) {
                var files = stream.filter(file -> {
                    var name = file.getFileName().toString();
                    return Files.isRegularFile(file)
                           && !name.startsWith(Translation.LEGACY_TRANSLATION_FILENAME)
                           && !name.startsWith(Config.CONFIG_FILENAME)
                           && !name.startsWith(MojangSkinProvider.PROFILE_CACHE_FILENAME)
                           && name.endsWith(SkinIO.FILE_EXTENSION);
                }).toList();
                
                if (!files.isEmpty() && !Files.exists(newDirectory))
                    Files.createDirectories(newDirectory);
                
                for (var file : files) {
                    var newFile = newDirectory.resolve(file.getFileName());
                    if (!Files.exists(newFile))
                        Files.move(file, newFile, StandardCopyOption.ATOMIC_MOVE);
                    else
                        Files.delete(file);
                }
            }
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("could not migrate skin directory", e);
        }
    }
    
    public static String readResource(String name) {
        try (var stream = SkinRestorer.class.getResourceAsStream(name)) {
            if (stream == null)
                return null;
            
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return StringUtils.readString(reader);
            }
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("failed to read resource", e);
            return null;
        }
    }
    
    public static String readFile(Path file) {
        try {
            if (!Files.exists(file))
                return null;
            
            return Files.readString(file);
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("failed to read file", e);
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
            SkinRestorer.LOGGER.error("failed to write file", e);
        }
    }
    
    public static void deleteFile(Path file) {
        try {
            if (Files.exists(file))
                Files.delete(file);
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("failed to delete file", e);
        }
    }
}
