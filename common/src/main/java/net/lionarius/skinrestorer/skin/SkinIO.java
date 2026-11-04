package net.lionarius.skinrestorer.skin;

import com.google.gson.JsonSyntaxException;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonMigrator;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class SkinIO {
    public static final String FILE_EXTENSION = ".json";

    private static final JsonMigrator MIGRATOR =
            JsonMigrator.builder("skin data").build();

    private final Path savePath;

    public SkinIO(Path savePath) {
        this.savePath = savePath;
    }

    public boolean skinExists(UUID uuid) {
        return Files.exists(savePath.resolve(SkinIO.uuidToFilename(uuid)));
    }

    public Optional<SkinValue> findSkin(UUID uuid) {
        var file = this.savePath.resolve(SkinIO.uuidToFilename(uuid));
        return SkinIO.findSkin(file);
    }

    private static Optional<SkinValue> findSkin(Path file) {
        var json = FileUtils.readFile(file);
        if (json == null) return Optional.empty();

        try {
            var jsonObject = JsonUtils.parseJson(json);
            if (jsonObject == null) throw new JsonSyntaxException("skin data is not a JSON object");

            var migrated = MIGRATOR.migrateToLatest(jsonObject);
            return Optional.of(JsonUtils.fromJson(migrated, SkinValue.class));
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Failed to parse or migrate skin data from {}", file, e);
            return Optional.of(SkinValue.EMPTY);
        }
    }

    public void saveSkin(UUID uuid, SkinValue skin) {
        var jsonObject = JsonUtils.toJsonObject(skin);
        MIGRATOR.stampVersion(jsonObject);
        FileUtils.writeFile(savePath.resolve(SkinIO.uuidToFilename(uuid)), JsonUtils.toJson(jsonObject));
    }

    public void deleteSkin(UUID uuid) {
        FileUtils.deleteFile(savePath.resolve(SkinIO.uuidToFilename(uuid)));
    }

    private static String uuidToFilename(UUID uuid) {
        return uuid + FILE_EXTENSION;
    }
}
