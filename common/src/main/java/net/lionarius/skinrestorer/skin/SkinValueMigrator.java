package net.lionarius.skinrestorer.skin;

import com.google.gson.JsonObject;
import net.lionarius.skinrestorer.SkinRestorer;

public final class SkinValueMigrator {
    
    private static final int VERSION_1 = 1; // initial version
    
    public static final int CURRENT_VERSION = VERSION_1;
    public static final String VERSION_KEY = "version";

    private SkinValueMigrator() {}

    public static int detectVersion(JsonObject json) {
        if (json.has(VERSION_KEY))
            return json.get(VERSION_KEY).getAsInt();
        
        return VERSION_1;
    }

    public static JsonObject migrateToLatest(JsonObject json) {
        int version = detectVersion(json);

        if (version > CURRENT_VERSION)
            throw new IllegalStateException("Skin data version %d is newer than supported version %d".formatted(version, CURRENT_VERSION));

        if (version < CURRENT_VERSION)
            SkinRestorer.LOGGER.info("Migrating skin data from version {} to {}", version, CURRENT_VERSION);

        for (int v = version; v < CURRENT_VERSION; v++) {
            json = applyMigration(json, v);
        }

        return json;
    }

    public static void stampVersion(JsonObject json) {
        json.addProperty(VERSION_KEY, CURRENT_VERSION);
    }

    // Applies a single migration step from fromVersion to fromVersion + 1.
    private static JsonObject applyMigration(JsonObject json, int fromVersion) {
        return switch (fromVersion) {
            case CURRENT_VERSION -> json; // temporary (TODO: remove when there is an actual new version)
            default -> throw new IllegalStateException("No migration defined from version " + fromVersion);
        };
    }
}
