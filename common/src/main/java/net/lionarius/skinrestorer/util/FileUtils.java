package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileUtils {
    private FileUtils() {}

    public static String readResource(String name) {
        try (var stream = SkinRestorer.class.getResourceAsStream(name)) {
            if (stream == null) return null;

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
            if (!Files.exists(file)) return null;

            return Files.readString(file);
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Failed to read file", e);
            return null;
        }
    }

    public static void writeFile(Path file, String content) {
        try {
            var parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            // write to a sibling temp file then atomically swap it in, so a crash mid-write
            // can't leave a half-written (and thus corrupt/unparseable) file behind
            var directory = parent != null ? parent : file.toAbsolutePath().getParent();
            var tmp = Files.createTempFile(directory, file.getFileName().toString(), ".tmp");
            try {
                Files.writeString(tmp, content);
                try {
                    Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("Failed to write file", e);
        }
    }

    public static void deleteFile(Path file) {
        try {
            if (Files.exists(file)) Files.delete(file);
        } catch (IOException e) {
            SkinRestorer.LOGGER.error("Failed to delete file", e);
        }
    }
}
