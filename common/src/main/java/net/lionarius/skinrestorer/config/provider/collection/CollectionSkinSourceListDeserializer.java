package net.lionarius.skinrestorer.config.provider.collection;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CollectionSkinSourceListDeserializer
        implements JsonSerializer<List<CollectionSkinSource>>, JsonDeserializer<List<CollectionSkinSource>> {
    @Override
    public JsonElement serialize(List<CollectionSkinSource> src, Type typeOfT, JsonSerializationContext context) {
        return context.serialize(src, List.class);
    }

    @Override
    public List<CollectionSkinSource> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        List<CollectionSkinSource> sources = new ArrayList<>();

        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("url")) {
                        sources.add(context.deserialize(obj, CollectionSkinUrl.class));
                    } else if (obj.has("path")) {
                        sources.add(context.deserialize(obj, CollectionSkinFile.class));
                    }
                }
            }
        }

        return sources;
    }
}
