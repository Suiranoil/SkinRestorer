package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.nio.file.Path;
import java.util.Optional;

public final class Config {
    
    public static final String CONFIG_FILENAME = "config.json";
    
    
    private String language = "en_us";
    
    private boolean refreshSkinOnJoin = false;
    
    private boolean fetchSkinOnFirstJoin = true;
    
    private FirstJoinSkinProvider firstJoinSkinProvider = FirstJoinSkinProvider.MOJANG;
    
    private String proxy = "";
    private transient Proxy parsedProxy = null;
    
    private long requestTimeout = 10;
    
    private long mojangCacheDuration = 60;
    private long elybyCacheDuration = 60;
    private long mineskinCacheDuration = 300;
    
    public String getLanguage() {
        return this.language;
    }
    
    public boolean refreshSkinOnJoin() {
        return this.refreshSkinOnJoin;
    }
    
    public boolean fetchSkinOnFirstJoin() {
        return this.fetchSkinOnFirstJoin;
    }
    
    public FirstJoinSkinProvider getFirstJoinSkinProvider() {
        return this.firstJoinSkinProvider;
    }
    
    public Optional<Proxy> getProxy() {
        return Optional.ofNullable(this.parsedProxy);
    }
    
    public long getRequestTimeout() {
        return this.requestTimeout;
    }
    
    public long getMojangCacheDuration() {
        return this.mojangCacheDuration;
    }
    
    public long getElybyCacheDuration() {
        return this.elybyCacheDuration;
    }
    
    public long getMineskinCacheDuration() {
        return this.mineskinCacheDuration;
    }
    
    public static Config load(Path path) {
        var configFile = path.resolve(Config.CONFIG_FILENAME);
        
        Config config = null;
        try {
            config = JsonUtils.fromJson(FileUtils.readFile(configFile), Config.class);
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Could not load config", e);
        }
        
        if (config == null)
            config = new Config();
        
        config.verifyAndFix();
        
        FileUtils.writeFile(path.resolve(Config.CONFIG_FILENAME), JsonUtils.toJson(config));
        
        return config;
    }
    
    private void verifyAndFix() {
        if (this.language == null || this.language.isEmpty())
            this.language = "en_us";
        
        if (this.firstJoinSkinProvider == null)
            this.firstJoinSkinProvider = FirstJoinSkinProvider.MOJANG;
        
        if (this.proxy == null)
            this.proxy = "";
        
        if (!this.proxy.isEmpty()) {
            try {
                this.parsedProxy = Proxy.parse(this.proxy);
            } catch (Exception e) {
                SkinRestorer.LOGGER.warn("Could not parse proxy config", e);
                this.parsedProxy = null;
            }
        }
        
        if (this.requestTimeout <= 0)
            this.requestTimeout = 10;
        
        if (this.mojangCacheDuration < 0)
            this.mojangCacheDuration = 60;
        
        if (this.elybyCacheDuration < 0)
            this.elybyCacheDuration = 60;
        
        if (this.mineskinCacheDuration < 0)
            this.mineskinCacheDuration = 300;
    }
}
