package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.collection.CollectionProviderConfig;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public final class ProvidersConfig implements GsonPostProcessable {
    public static final ProvidersConfig DEFAULT = new ProvidersConfig(
            new MojangProviderConfig(),
            new ElyByProviderConfig(),
            new MineskinProviderConfig(),
            new DraslProviderConfig(),
            new CollectionProviderConfig()
    );
    
    private MojangProviderConfig mojang;
    private ElyByProviderConfig ely_by;
    private MineskinProviderConfig mineskin;
    private DraslProviderConfig drasl;
    private CollectionProviderConfig collection;
    
    public ProvidersConfig(MojangProviderConfig mojang, ElyByProviderConfig ely_by, MineskinProviderConfig mineskin, DraslProviderConfig drasl, CollectionProviderConfig collection) {
        this.mojang = mojang;
        this.ely_by = ely_by;
        this.mineskin = mineskin;
        this.drasl = drasl;
        this.collection = collection;
    }
    
    public MojangProviderConfig mojang() {
        return this.mojang;
    }
    
    public ElyByProviderConfig ely_by() {
        return this.ely_by;
    }
    
    public MineskinProviderConfig mineskin() {
        return this.mineskin;
    }
    
    public DraslProviderConfig drasl() {
        return this.drasl;
    }

    public CollectionProviderConfig collection() {
        return this.collection;
    }
    
    @Override
    public void gsonPostProcess() {
        if (this.mojang == null) {
            SkinRestorer.LOGGER.warn("Mojang provider config is null, using default");
            this.mojang = ProvidersConfig.DEFAULT.mojang();
        }
        
        if (this.ely_by == null) {
            SkinRestorer.LOGGER.warn("Ely.By provider config is null, using default");
            this.ely_by = ProvidersConfig.DEFAULT.ely_by();
        }
        
        if (this.mineskin == null) {
            SkinRestorer.LOGGER.warn("Mineskin provider config is null, using default");
            this.mineskin = ProvidersConfig.DEFAULT.mineskin();
        }
        
        if (this.drasl == null) {
            SkinRestorer.LOGGER.warn("Drasl provider config is null, using default");
            this.drasl = ProvidersConfig.DEFAULT.drasl();
        }

        if (this.collection == null) {
            SkinRestorer.LOGGER.warn("Collection provider config is null, using default");
            this.collection = ProvidersConfig.DEFAULT.collection();
        }
    }
}
