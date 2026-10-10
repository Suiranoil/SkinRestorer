package net.lionarius.skinrestorer.skin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

/** The exact signed textures property; validation never rewrites either signed string. */
public record SkinTexture(String value, String signature) {

    public static final int MAX_VALUE_LENGTH = 16384;
    public static final int MAX_SIGNATURE_LENGTH = 4096;
    private static final Set<String> TEXTURE_HOSTS = Set
        .of("textures.minecraft.net", "skins.minecraft.net", "textures.mojang.com");

    public SkinTexture {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(signature, "signature");
        if (value.isEmpty() || value.length() > MAX_VALUE_LENGTH
            || signature.isEmpty()
            || signature.length() > MAX_SIGNATURE_LENGTH) {
            throw new IllegalArgumentException("Textures value or signature exceeds its length limit or is empty");
        }
        byte[] payload = decodeBase64(value, "textures value");
        byte[] signedBytes = decodeBase64(signature, "textures signature");
        if (payload.length == 0 || signedBytes.length == 0) {
            throw new IllegalArgumentException("Textures value and signature must not decode to empty bytes");
        }
        JsonObject object = SkinJson.parseObject(SkinJson.decodeUtf8(payload));
        JsonElement textures = object.get("textures");
        if (textures == null || !textures.isJsonObject()) {
            throw new IllegalArgumentException("Textures payload has no textures object");
        }
        JsonElement skin = textures.getAsJsonObject()
            .get("SKIN");
        if (skin == null || !skin.isJsonObject()) {
            throw new IllegalArgumentException("Textures payload has no SKIN object");
        }
        validateTextureUrl(SkinJson.requireString(skin.getAsJsonObject(), "url"));
        // Authlib can also download CAPE from this property. Apply the same host boundary.
        JsonElement cape = textures.getAsJsonObject()
            .get("CAPE");
        if (cape != null) {
            if (!cape.isJsonObject()) {
                throw new IllegalArgumentException("Textures CAPE must be an object");
            }
            validateTextureUrl(SkinJson.requireString(cape.getAsJsonObject(), "url"));
        }
        JsonElement metadata = skin.getAsJsonObject()
            .get("metadata");
        if (metadata != null) {
            if (!metadata.isJsonObject()) {
                throw new IllegalArgumentException("Skin metadata must be an object");
            }
            JsonElement model = metadata.getAsJsonObject()
                .get("model");
            if (model != null && !Set.of("slim", "classic")
                .contains(SkinJson.requireString(metadata.getAsJsonObject(), "model"))) {
                throw new IllegalArgumentException("Unsupported skin model metadata");
            }
        }
    }

    public Property toProperty() {
        return new Property("textures", value, signature);
    }

    /** Reads only; never changes the live profile or any of its properties. */
    public static Optional<SkinTexture> fromProfile(GameProfile profile) {
        Objects.requireNonNull(profile, "profile");
        for (Property property : profile.getProperties()
            .get("textures")) {
            try {
                return Optional.of(new SkinTexture(property.getValue(), property.getSignature()));
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // An unsigned, malformed or cape-only property is not a usable signed skin.
            }
        }
        return Optional.empty();
    }

    /** Extracts exactly one signed skin from an official session-server profile response. */
    public static SkinTexture parseMojangProfile(JsonObject profile) {
        Objects.requireNonNull(profile, "profile");
        JsonElement properties = profile.get("properties");
        if (properties == null || !properties.isJsonArray()) {
            throw new IllegalArgumentException("Mojang profile has no properties array");
        }
        JsonArray array = properties.getAsJsonArray();
        if (array.size() > 32) {
            throw new IllegalArgumentException("Mojang profile has too many properties");
        }
        SkinTexture result = null;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Mojang profile property must be an object");
            }
            JsonObject property = element.getAsJsonObject();
            if (!"textures".equals(SkinJson.requireString(property, "name"))) {
                continue;
            }
            if (result != null) {
                throw new IllegalArgumentException("Mojang profile has multiple textures properties");
            }
            result = new SkinTexture(
                SkinJson.requireString(property, "value"),
                SkinJson.requireString(property, "signature"));
        }
        if (result == null) {
            throw new IllegalArgumentException("Mojang profile has no signed skin texture");
        }
        return result;
    }

    private static byte[] decodeBase64(String value, String field) {
        if (!value.matches("[A-Za-z0-9+/]+={0,2}")) {
            throw new IllegalArgumentException("Invalid Base64 " + field);
        }
        try {
            return Base64.getDecoder()
                .decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Base64 " + field, exception);
        }
    }

    private static void validateTextureUrl(String value) {
        if (value.length() > 2048) {
            throw new IllegalArgumentException("Texture URL exceeds its length limit");
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                || host == null
                || !TEXTURE_HOSTS.contains(host.toLowerCase(Locale.ROOT))
                || uri.getRawUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getRawPath() == null
                || !uri.getRawPath()
                    .matches("/texture/[a-fA-F0-9]{32,64}")) {
                throw new IllegalArgumentException("Texture URL must use an allowed Mojang texture host and path");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid texture URL", exception);
        }
    }
}
