package net.lionarius.skinrestorer;

import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.enums.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URL;

public class MineskinSkinProvider {

    private static final String API = "https://api.mineskin.org/generate/url";
    private static final String USER_AGENT = "SkinRestorer";
    private static final String TYPE = "application/json";

    public static Property getSkin(String url, SkinVariant variant) {
        try {
            String input = ("{\"variant\":\"%s\",\"name\":\"%s\",\"visibility\":%d,\"url\":\"%s\"}")
                    .formatted(variant.toString(), "none", 1, url);

            JsonObject texture = JsonUtils.parseJson(WebUtils.POSTRequest(new URL(API), USER_AGENT, TYPE, TYPE, input))
                    .getAsJsonObject("data").getAsJsonObject("texture");

            return new Property("textures", texture.get("value").getAsString(), texture.get("signature").getAsString());
        } catch (IOException e) {
            return null;
        }
    }
}
