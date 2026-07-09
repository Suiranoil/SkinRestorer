package net.lionarius.skinrestorer.config;

import com.google.gson.annotations.SerializedName;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public final class StorageConfig implements GsonPostProcessable {
    private Location location = Location.WORLD;

    public Location location() {
        return this.location;
    }

    public void location(Location location) {
        this.location = location;
    }

    @Override
    public void gsonPostProcess() {
        if (this.location == null) {
            SkinRestorer.LOGGER.warn("Storage location config is null, defaulting to 'world'");
            this.location = Location.WORLD;
        }
    }

    public enum Location {
        @SerializedName(
                value = "world",
                alternate = {"WORLD"})
        WORLD,
        @SerializedName(
                value = "global",
                alternate = {"GLOBAL"})
        GLOBAL
    }
}
