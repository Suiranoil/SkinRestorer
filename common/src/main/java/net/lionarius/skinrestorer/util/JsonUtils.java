package net.lionarius.skinrestorer.util;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.PostProcessingEnabler;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class JsonUtils {
    
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new PostProcessingEnabler())
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
            .registerTypeAdapter(GameProfile.class, new GameProfileSerializer())
            .setPrettyPrinting()
            .create();
    
    private JsonUtils() {}
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    public static <T> T fromJson(JsonElement json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }
    
    public static <T> String toJson(T obj) {
        return GSON.toJson(obj);
    }
    
    public static JsonObject parseJson(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }
    
    public static JsonObject skinPropertyToJson(Property property) {
        try {
            JsonObject json = GSON.fromJson(new String(Base64.getDecoder().decode(property.getValue()), StandardCharsets.UTF_8), JsonObject.class);
            if (json != null)
                json.remove("timestamp");
            
            return json;
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Could not parse skin property", e);
            return null;
        }
    }
    
    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
        @Override
        public GameProfile deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject object = (JsonObject) json;
            final UUID id = object.has("id") ? context.deserialize(object.get("id"), UUID.class) : null;
            final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }
        
        @Override
        public JsonElement serialize(final GameProfile src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            if (src.getId() != null) {
                result.add("id", context.serialize(src.getId()));
            }
            if (src.getName() != null) {
                result.addProperty("name", src.getName());
            }
            return result;
        }
    }
}
