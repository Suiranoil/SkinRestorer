package net.lionarius.skinrestorer.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.ProvidersConfig;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonMigrator;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.nio.file.Path;

public final class Config implements GsonPostProcessable {
    public static final String CONFIG_FILENAME = "config.json";

    private static final JsonMigrator MIGRATOR = JsonMigrator.builder("config")
            .migration(1, Config::migrateV1ToV2)
            .migration(2, Config::migrateV2ToV3)
            .build();

    private String language = "en_us";

    private JoinConfig join = new JoinConfig();

    private RequestConfig request = new RequestConfig();

    private ProvidersConfig providers = ProvidersConfig.DEFAULT;

    public String language() {
        return this.language;
    }

    public JoinConfig join() {
        return this.join;
    }

    public RequestConfig request() {
        return this.request;
    }

    public ProvidersConfig providers() {
        return this.providers;
    }

    public static Config load(Path path) {
        var configFile = path.resolve(Config.CONFIG_FILENAME);

        Config config = null;
        try {
            var json = FileUtils.readFile(configFile);
            var jsonObject = JsonUtils.parseJson(json);

            var migrated = MIGRATOR.migrateToLatest(jsonObject);
            config = JsonUtils.fromJson(migrated, Config.class);
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Could not load config", e);
        }

        if (config == null) config = new Config();

        var jsonObject = JsonUtils.toJsonObject(config);
        MIGRATOR.stampVersion(jsonObject);
        FileUtils.writeFile(configFile, JsonUtils.toJson(jsonObject));

        return config;
    }

    @Override
    public void gsonPostProcess() {
        if (this.language == null || this.language.isEmpty()) {
            SkinRestorer.LOGGER.warn("Language config is null or empty, defaulting to 'en_us'");
            this.language = "en_us";
        }

        if (this.join == null) {
            SkinRestorer.LOGGER.warn("Join config is null, using default");
            this.join = new JoinConfig();
        }

        if (this.request == null) {
            SkinRestorer.LOGGER.warn("Request config is null, using default");
            this.request = new RequestConfig();
        }

        if (this.providers == null) {
            SkinRestorer.LOGGER.warn("Providers config is null, using default");
            this.providers = ProvidersConfig.DEFAULT;
        }
    }

    private static JsonObject migrateV1ToV2(JsonObject json) {
        var autoFetchObject = new JsonObject();
        JsonUtils.moveProperty(json, autoFetchObject, "fetchSkinOnFirstJoin", "enabled");
        JsonUtils.moveProperty(json, autoFetchObject, "forceFirstJoinSkinFetch", "overrideExisting");
        JsonUtils.moveProperty(json, autoFetchObject, "firstJoinSkinProvider", "provider");

        var joinObject = new JsonObject();
        JsonUtils.moveProperty(json, joinObject, "refreshSkinOnJoin", "refreshSkin");
        JsonUtils.moveProperty(json, joinObject, "skinApplyDelayOnJoin", "applyDelay");
        joinObject.add("autoFetch", autoFetchObject);
        json.add("join", joinObject);

        var requestObject = new JsonObject();
        JsonUtils.moveProperty(json, requestObject, "proxy");
        JsonUtils.moveProperty(json, requestObject, "requestTimeout", "timeout");
        json.add("request", requestObject);

        return json;
    }

    private static JsonObject migrateV2ToV3(JsonObject json) {
        if (!json.has("join") || !json.get("join").isJsonObject()) return json;
        var joinObject = json.getAsJsonObject("join");

        if (!joinObject.has("autoFetch") || !joinObject.get("autoFetch").isJsonObject()) return json;
        var autoFetchObject = joinObject.getAsJsonObject("autoFetch");

        if (!autoFetchObject.has("provider")) return json;

        var provider = autoFetchObject.remove("provider");
        var providers = new JsonArray();
        providers.add(provider);
        autoFetchObject.add("providers", providers);

        return json;
    }
}
