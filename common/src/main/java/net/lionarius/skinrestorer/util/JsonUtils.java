package net.lionarius.skinrestorer.util;

import com.google.gson.*;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.SkinRestorer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JsonUtils {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
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
            JsonObject json = GSON.fromJson(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), JsonObject.class);
            if (json != null)
                json.remove("timestamp");
            
            return json;
        } catch (Exception e) {
            SkinRestorer.LOGGER.error(e.toString());
            return null;
        }
    }
}
