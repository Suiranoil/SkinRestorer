package net.lionarius.skinrestorer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    
    public static JsonObject toJsonObject(Object obj) {
        return GSON.toJsonTree(obj).getAsJsonObject();
    }
    
    public static JsonObject parseJson(String json) {
        return GSON.fromJson(json, JsonObject.class);
    }
    
    public static JsonObject skinPropertyToJson(Property property) {
        try {
            JsonObject json = GSON.fromJson(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), JsonObject.class);
            if (json != null)
                json.remove("timestamp");
            
            return json;
        } catch (Exception e) {
            SkinRestorer.LOGGER.error("Could not parse skin property", e);
            return null;
        }
    }
}
