package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.ElyBySkinProvider;
import net.lionarius.skinrestorer.skin.provider.MineskinSkinProvider;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;

public final class ProvidersConfig {
    public static final ProvidersConfig DEFAULT = new ProvidersConfig(
            new BuiltInProviderConfig(true, MojangSkinProvider.PROVIDER_NAME, new CacheConfig(true, 60)),
            new BuiltInProviderConfig(true, ElyBySkinProvider.PROVIDER_NAME, new CacheConfig(true, 60)),
            new BuiltInProviderConfig(true, MineskinSkinProvider.PROVIDER_NAME, new CacheConfig(true, 300))
    );
    
    private BuiltInProviderConfig mojang;
    private BuiltInProviderConfig ely_by;
    private BuiltInProviderConfig mineskin;
    
    public ProvidersConfig(BuiltInProviderConfig mojang, BuiltInProviderConfig ely_by, BuiltInProviderConfig mineskin) {
        this.mojang = mojang;
        this.ely_by = ely_by;
        this.mineskin = mineskin;
    }
    
    public boolean isValid() {
        if (this == ProvidersConfig.DEFAULT)
            return true;
        
        return (this.mojang != null && this.mojang.isValid())
               && (this.ely_by != null && this.ely_by.isValid())
               && (this.mineskin != null && this.mineskin.isValid());
    }
    
    public void fix() {
        if (this == ProvidersConfig.DEFAULT)
            return;
        
        if (this.mojang == null || !this.mojang.isValid()) {
            SkinRestorer.LOGGER.warn("Mojang provider config is invalid, using default");
            this.mojang = ProvidersConfig.DEFAULT.mojang();
        }
        
        if (this.ely_by == null || !this.ely_by.isValid()) {
            SkinRestorer.LOGGER.warn("Ely.By provider config is invalid, using default");
            this.ely_by = ProvidersConfig.DEFAULT.ely_by();
        }
        
        if (this.mineskin == null || !this.mineskin.isValid()) {
            SkinRestorer.LOGGER.warn("Mineskin provider config is invalid, using default");
            this.mineskin = ProvidersConfig.DEFAULT.mineskin();
        }
    }
    
    public BuiltInProviderConfig mojang() {
        return this.mojang;
    }
    
    public BuiltInProviderConfig ely_by() {
        return this.ely_by;
    }
    
    public BuiltInProviderConfig mineskin() {
        return this.mineskin;
    }
}
