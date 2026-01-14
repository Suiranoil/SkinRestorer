package net.lionarius.skinrestorer.config.provider.collection;

import com.google.gson.annotations.JsonAdapter;
import net.lionarius.skinrestorer.config.provider.BuiltInProviderConfig;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.skin.provider.CollectionSkinProvider;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.util.ArrayList;
import java.util.List;

public final class CollectionProviderConfig extends BuiltInProviderConfig implements GsonPostProcessable {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 604800);
    
    @JsonAdapter(CollectionSkinSourceListDeserializer.class)
    private List<CollectionSkinSource> sources = new ArrayList<>();
    
    public CollectionProviderConfig() {
        super(CollectionSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE, false);
    }
    
    public List<CollectionSkinSource> sources() {
        return this.sources;
    }
    
    @Override
    public void gsonPostProcess() {
        if (this.sources == null) {
            this.sources = new ArrayList<>();
        }
    }
}
