package net.lionarius.skinrestorer.skin;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonMigrator;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
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

    public SkinValue loadSkin(UUID uuid) {
        var file = this.savePath.resolve(SkinIO.uuidToFilename(uuid));
        return SkinIO.loadSkin(file);
    }

    private static SkinValue loadSkin(Path file) {
        try {
            var json = FileUtils.readFile(file);
            var jsonObject = JsonUtils.parseJson(json);

            var migrated = MIGRATOR.migrateToLatest(jsonObject);
            return JsonUtils.fromJson(migrated, SkinValue.class);
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Failed to parse or migrate skin data from {}", file, e);
            return SkinValue.EMPTY;
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
