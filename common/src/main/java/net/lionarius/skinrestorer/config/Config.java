package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.ProvidersConfig;
import net.lionarius.skinrestorer.skin.provider.builtin.CollectionSkinProvider;
import net.lionarius.skinrestorer.skin.provider.builtin.ElyBySkinProvider;
import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class Config implements GsonPostProcessable {
    
    public static final String CONFIG_FILENAME = "config.json";
    
    
    private String language = "en_us";
    
    private boolean refreshSkinOnJoin = true;
    
    private int skinApplyDelayOnJoin = 0;
    
    private boolean fetchSkinOnFirstJoin = true;
    
    private boolean forceFirstJoinSkinFetch = false;
    
    private String firstJoinSkinProvider = MojangSkinProvider.PROVIDER_NAME;
    
    private String proxy = "";
    private transient Proxy parsedProxy = null;
    
    private long requestTimeout = 10;
    
    private ProvidersConfig providers = ProvidersConfig.DEFAULT;
    
    public String language() {
        return this.language;
    }
    
    public boolean refreshSkinOnJoin() {
        return this.refreshSkinOnJoin;
    }
    
    public int skinApplyDelayOnJoin() {
        return this.skinApplyDelayOnJoin;
    }
    
    public boolean fetchSkinOnFirstJoin() {
        return this.fetchSkinOnFirstJoin;
    }
    
    public boolean forceFirstJoinSkinFetch() {
        return this.forceFirstJoinSkinFetch;
    }
    
    public String firstJoinSkinProvider() {
        return this.firstJoinSkinProvider;
    }
    
    public Optional<Proxy> proxy() {
        return Optional.ofNullable(this.parsedProxy);
    }
    
    public long requestTimeout() {
        return this.requestTimeout;
    }
    
    public ProvidersConfig providersConfig() {
        return this.providers;
    }
    
    public static Config load(Path path) {
        var configFile = path.resolve(Config.CONFIG_FILENAME);

        Config config = null;
        try {
            var json = FileUtils.readFile(configFile);
            var jsonObject = JsonUtils.parseJson(json);

            var migrated = ConfigMigrator.migrateToLatest(jsonObject);
            config = JsonUtils.fromJson(migrated, Config.class);
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Could not load config", e);
        }

        if (config == null)
            config = new Config();

        var jsonObject = JsonUtils.toJsonObject(config);
        ConfigMigrator.stampVersion(jsonObject);
        FileUtils.writeFile(configFile, JsonUtils.toJson(jsonObject));

        return config;
    }
    
    @Override
    public void gsonPostProcess() {
        if (this.language == null || this.language.isEmpty()) {
            SkinRestorer.LOGGER.warn("Language config is null or empty, defaulting to 'en_us'");
            this.language = "en_us";
        }
        
        if (this.skinApplyDelayOnJoin < 0) {
            SkinRestorer.LOGGER.warn("SkinApplyDelayOnJoin config is less than 0, defaulting to 0");
            this.skinApplyDelayOnJoin = 0;
        }
        
        if (this.firstJoinSkinProvider == null) {
            SkinRestorer.LOGGER.warn("FirstJoinSkinProvider config is null, defaulting to MOJANG");
            this.firstJoinSkinProvider = MojangSkinProvider.PROVIDER_NAME;
        } else if (this.firstJoinSkinProvider.isBlank()) {
            SkinRestorer.LOGGER.warn("FirstJoinSkinProvider config is empty, defaulting to MOJANG");
            this.firstJoinSkinProvider = MojangSkinProvider.PROVIDER_NAME;
        } else {
            this.firstJoinSkinProvider = Config.normalizeFirstJoinSkinProvider(this.firstJoinSkinProvider);
            
            if (this.firstJoinSkinProvider.isEmpty()) {
                SkinRestorer.LOGGER.warn("FirstJoinSkinProvider config is empty after normalization, defaulting to MOJANG");
                this.firstJoinSkinProvider = MojangSkinProvider.PROVIDER_NAME;
            }
        }
        
        if (this.proxy == null) {
            SkinRestorer.LOGGER.warn("Proxy config is null, defaulting to an empty string");
            this.proxy = "";
        }
        
        if (!this.proxy.isEmpty()) {
            try {
                this.parsedProxy = Proxy.parse(this.proxy);
            } catch (Exception e) {
                SkinRestorer.LOGGER.warn("Could not parse proxy config: {}", e.getMessage());
                this.parsedProxy = null;
            }
        }
        
        if (this.requestTimeout <= 0) {
            SkinRestorer.LOGGER.warn("Request timeout config is less than or equal to 0, defaulting to 10");
            this.requestTimeout = 10;
        }
        
        if (this.providers == null) {
            SkinRestorer.LOGGER.warn("Providers config is null, using default");
            this.providers = ProvidersConfig.DEFAULT;
        }
    }
    
    private static String normalizeFirstJoinSkinProvider(String value) {
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        
        return switch (normalized) {
            case "mojang" -> MojangSkinProvider.PROVIDER_NAME;
            case "ely.by", "ely_by" -> ElyBySkinProvider.PROVIDER_NAME;
            case "collection" -> CollectionSkinProvider.PROVIDER_NAME;
            default -> value.trim();
        };
    }
}
