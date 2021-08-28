package net.lionarius.skinrestorer;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

public class MojangSkinProvider {

    private static final String API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_SERVER = "https://sessionserver.mojang.com/session/minecraft/profile/";

    public static Property getSkin(String name) {
        try {
            UUID uuid = getUUID(name);
            JsonObject texture = JsonUtils.parseJson(WebUtils.GETRequest(new URL(SESSION_SERVER + uuid + "?unsigned=false")))
                    .getAsJsonArray("properties").get(0).getAsJsonObject();

            return new Property("textures", texture.get("value").getAsString(), texture.get("signature").getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private static UUID getUUID(String name) throws IOException {
        return UUID.fromString(JsonUtils.parseJson(WebUtils.GETRequest(new URL(API + name))).get("id").getAsString()
                .replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }
}
