package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.JsonUtils;

import java.nio.file.Path;

public final class Config {
    
    public static final String CONFIG_FILENAME = "config.json";
    
    
    private String language = "en_us";
    
    private boolean fetchSkinOnFirstJoin = true;
    
    private FirstJoinSkinProvider firstJoinSkinProvider = FirstJoinSkinProvider.MOJANG;
    
    private String proxy = "";
    
    private long requestTimeout = 10;
    
    public String getLanguage() {
        return this.language;
    }
    
    public boolean fetchSkinOnFirstJoin() {
        return this.fetchSkinOnFirstJoin;
    }
    
    public FirstJoinSkinProvider getFirstJoinSkinProvider() {
        return this.firstJoinSkinProvider;
    }
    
    public String getProxy() {
        return this.proxy;
    }
    
    public long getRequestTimeout() {
        return this.requestTimeout;
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
        
        FileUtils.writeFile(path.resolve(Config.CONFIG_FILENAME), JsonUtils.toJson(config));
        
        return config;
    }
}
