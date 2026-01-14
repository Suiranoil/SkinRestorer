package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class CollectionSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "collection";
    
    private static LoadingCache<Integer, Optional<Property>> SKIN_CACHE;
    
    private static List<Pair<URI, SkinVariant>> COLLECTION_SKINS;
    
    public static void reload() {
        COLLECTION_SKINS = loadCollectionSkins();
        
        createCache();
    }
    
    private static List<Pair<URI, SkinVariant>> loadCollectionSkins() {
        List<Pair<URI, SkinVariant>> skins = new ArrayList<>();
        
        var config = SkinRestorer.getConfig().providersConfig().collection();
        
        for (CollectionSkinSource source : config.sources()) {
            var uri = source.uri();
            if (uri != null) {
                skins.add(Pair.of(uri, source.variant()));
            }
        }
        
        return skins;
    }
    
    private static void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().collection();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        SKIN_CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull Integer key) throws Exception {
                        var skinEntry = COLLECTION_SKINS.get(key);
                        return MineskinSkinProvider.loadSkin(skinEntry.first(), skinEntry.second());
                    }
                });
    }
    
    @Override
    public String getArgumentName() {
        return "seed";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant) {
        if (COLLECTION_SKINS.isEmpty()) {
            return Result.error(new IllegalStateException("No collection skins configured"));
        }
        
        var skinIndex = Math.abs(argument.hashCode()) % COLLECTION_SKINS.size();
        
        try {
            return Result.success(SKIN_CACHE.get(skinIndex));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
}
