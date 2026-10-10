package net.lionarius.skinrestorer.skin;

import java.util.Objects;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * World-local state. Empty playerName means a known UUID with unavailable name metadata.
 * Null selectedName means the current login name; texture is optional fallback.
 */
public record SkinState(UUID id, String playerName, String selectedName, SkinTexture texture, long revision) {

    public static final int SCHEMA_VERSION = 1;

    public SkinState {
        Objects.requireNonNull(id, "id");
        if (!"".equals(playerName)) {
            validateName(playerName, "playerName");
        }
        if (selectedName != null) {
            validateName(selectedName, "selectedName");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("schemaVersion", SCHEMA_VERSION);
        object.addProperty("id", id.toString());
        object.addProperty("playerName", playerName);
        if (selectedName == null) {
            object.add("selectedName", JsonNull.INSTANCE);
        } else {
            object.addProperty("selectedName", selectedName);
        }
        if (texture == null) {
            object.add("texture", JsonNull.INSTANCE);
        } else {
            JsonObject property = new JsonObject();
            property.addProperty("value", texture.value());
            property.addProperty("signature", texture.signature());
            object.add("texture", property);
        }
        object.addProperty("revision", revision);
        return object;
    }

    public static SkinState fromJson(JsonObject object) {
        Objects.requireNonNull(object, "object");
        if (SkinJson.requireLong(object, "schemaVersion") != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported skin-state schema version");
        }
        String rawId = SkinJson.requireString(object, "id");
        UUID id = UUID.fromString(rawId);
        if (!id.toString()
            .equals(rawId)) {
            throw new IllegalArgumentException("Skin-state UUID must use its canonical lowercase form");
        }
        JsonElement selected = object.get("selectedName");
        JsonElement property = object.get("texture");
        if (selected == null || property == null) {
            throw new IllegalArgumentException("Skin-state selectedName and texture fields are required");
        }
        String selectedName = selected.isJsonNull() ? null : SkinJson.requireString(object, "selectedName");
        SkinTexture texture = null;
        if (!property.isJsonNull()) {
            if (!property.isJsonObject()) {
                throw new IllegalArgumentException("Skin-state texture must be an object or null");
            }
            texture = new SkinTexture(
                SkinJson.requireString(property.getAsJsonObject(), "value"),
                SkinJson.requireString(property.getAsJsonObject(), "signature"));
        }
        return new SkinState(
            id,
            SkinJson.requireString(object, "playerName"),
            selectedName,
            texture,
            SkinJson.requireLong(object, "revision"));
    }

    private static void validateName(String name, String field) {
        if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException(
                field + " must be a Minecraft username of 1-16 ASCII letters, digits or underscores");
        }
    }
}
