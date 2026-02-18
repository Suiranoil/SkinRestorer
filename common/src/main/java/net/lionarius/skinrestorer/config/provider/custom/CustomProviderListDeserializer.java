package net.lionarius.skinrestorer.config.provider.custom;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class CustomProviderListDeserializer implements JsonSerializer<List<CustomProviderConfig>>, JsonDeserializer<List<CustomProviderConfig>> {
    @Override
    public JsonElement serialize(List<CustomProviderConfig> src, Type typeOfT, JsonSerializationContext context) {
        return context.serialize(src, List.class);
    }
    
    @Override
    public List<CustomProviderConfig> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        var result = new ArrayList<CustomProviderConfig>();
        if (!json.isJsonArray())
            return result;
        
        for (var element : json.getAsJsonArray()) {
            if (!element.isJsonObject())
                continue;
            
            var object = element.getAsJsonObject();
            
            var providerType = this.extractProviderType(object, context);
            
            CustomProviderConfig provider = switch (providerType) {
                case YGGDRASIL -> context.deserialize(object, CustomYggdrasilProviderConfig.class);
                case AUTHLIB_INJECTOR -> context.deserialize(object, CustomAuthlibInjectorProviderConfig.class);
                default -> context.deserialize(object, UnknownCustomProviderConfig.class);
            };
            
            result.add(provider);
        }
        
        return result;
    }
    
    private CustomProviderType extractProviderType(JsonObject provider, JsonDeserializationContext context) {
        if (provider.has("type")) {
            try {
                CustomProviderType type = context.deserialize(provider.get("type"), CustomProviderType.class);
                if (type != null)
                    return type;
            } catch (Exception ignored) {
                return CustomProviderType.UNKNOWN;
            }
        }
        
        return CustomProviderType.UNKNOWN;
    }
}
