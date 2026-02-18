package net.lionarius.skinrestorer.config.provider.custom;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.custom.CustomYggdrasilSkinProvider;
import net.lionarius.skinrestorer.util.Result;

public final class CustomYggdrasilProviderConfig extends CustomProviderConfig {
    public static final CustomProviderType TYPE = CustomProviderType.YGGDRASIL;
    
    private String baseUrl = "";
    private String servicesUrl = "";
    private String sessionUrl = "";
    
    public CustomYggdrasilProviderConfig() {
        super(CustomYggdrasilProviderConfig.TYPE);
    }
    
    public String servicesUrl() {
        return this.servicesUrl;
    }
    
    public String sessionUrl() {
        return this.sessionUrl;
    }
    
    public boolean hasConfiguredUrls() {
        return !this.servicesUrl.isEmpty() && !this.sessionUrl.isEmpty();
    }
    
    @Override
    public Result<SkinProvider, String> createSkinProvider(SkinSigner skinSigner) {
        if (!this.hasConfiguredUrls())
            return Result.error("servicesUrl and sessionUrl are not fully configured");
        
        return Result.success(new CustomYggdrasilSkinProvider(this.name, skinSigner));
    }
    
    @Override
    public void gsonPostProcess() {
        super.gsonPostProcess();
        
        this.type = CustomYggdrasilProviderConfig.TYPE;
        
        if (this.name == null) {
            SkinRestorer.LOGGER.warn("Custom provider name is null, defaulting to an empty string");
            this.name = "";
        }
        
        if (this.baseUrl == null) {
            SkinRestorer.LOGGER.warn("Custom provider baseUrl is null, defaulting to an empty string");
            this.baseUrl = "";
        }
        
        if (this.servicesUrl == null) {
            SkinRestorer.LOGGER.warn("Custom provider servicesUrl is null, defaulting to an empty string");
            this.servicesUrl = "";
        }
        
        if (this.sessionUrl == null) {
            SkinRestorer.LOGGER.warn("Custom provider sessionUrl is null, defaulting to an empty string");
            this.sessionUrl = "";
        }
        
        this.baseUrl = this.baseUrl.trim();
        this.servicesUrl = this.servicesUrl.trim();
        this.sessionUrl = this.sessionUrl.trim();
        
        if (!this.baseUrl.isEmpty()) {
            if (this.servicesUrl.isEmpty())
                this.servicesUrl = this.baseUrl;
            
            if (this.sessionUrl.isEmpty())
                this.sessionUrl = this.baseUrl;
        }
    }
}
