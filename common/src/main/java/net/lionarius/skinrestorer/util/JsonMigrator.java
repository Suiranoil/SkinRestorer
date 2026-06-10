package net.lionarius.skinrestorer.util;

import com.google.gson.JsonObject;
import net.lionarius.skinrestorer.SkinRestorer;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * Versioning starts from 1. JSON without a version key is assumed to be version 1.
 */
public final class JsonMigrator {
    public static final String VERSION_KEY = "version";

    private final String name;
    private final int currentVersion;
    private final NavigableMap<Integer, UnaryOperator<JsonObject>> migrations;

    private JsonMigrator(String name, int currentVersion, NavigableMap<Integer, UnaryOperator<JsonObject>> migrations) {
        this.name = name;
        this.currentVersion = currentVersion;
        this.migrations = Collections.unmodifiableNavigableMap(new TreeMap<>(migrations));
    }

    public int getCurrentVersion() {
        return this.currentVersion;
    }

    public int detectVersion(JsonObject json) {
        if (json.has(VERSION_KEY)) return json.get(VERSION_KEY).getAsInt();

        return 1;
    }

    public JsonObject migrateToLatest(JsonObject json) {
        int version = this.detectVersion(json);

        if (version > this.currentVersion)
            throw new IllegalStateException("%s version %d is newer than supported version %d"
                    .formatted(this.name, version, this.currentVersion));

        if (version < this.currentVersion)
            SkinRestorer.LOGGER.info("Migrating {} from version {} to {}", this.name, version, this.currentVersion);

        for (int v = version; v < this.currentVersion; v++) {
            var migration = this.migrations.get(v);
            if (migration == null)
                throw new IllegalStateException("No migration defined for %s from version %d".formatted(this.name, v));

            json = migration.apply(json);
        }

        return json;
    }

    public void stampVersion(JsonObject json) {
        json.addProperty(VERSION_KEY, this.currentVersion);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {

        private final String name;
        private final TreeMap<Integer, UnaryOperator<JsonObject>> migrations = new TreeMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder migration(int fromVersion, UnaryOperator<JsonObject> migration) {
            if (this.migrations.containsKey(fromVersion))
                throw new IllegalArgumentException("Duplicate migration for version " + fromVersion);

            this.migrations.put(fromVersion, migration);
            return this;
        }

        public JsonMigrator build() {
            int currentVersion = this.migrations.isEmpty() ? 1 : this.migrations.lastKey() + 1;

            // Validate no gaps between registered migrations
            if (!this.migrations.isEmpty()) {
                int first = this.migrations.firstKey();
                for (int v = first; v < currentVersion; v++) {
                    if (!this.migrations.containsKey(v))
                        throw new IllegalStateException("Missing migration from version " + v);
                }
            }

            return new JsonMigrator(this.name, currentVersion, this.migrations);
        }
    }
}
