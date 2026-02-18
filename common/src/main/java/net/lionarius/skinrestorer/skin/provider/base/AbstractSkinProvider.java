package net.lionarius.skinrestorer.skin.provider.base;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.util.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class AbstractSkinProvider<K> implements SkinProvider {
    
    private LoadingCache<K, Optional<Property>> skinCache;
    
    protected abstract CacheConfig getCacheConfig();
    
    protected abstract K getCacheKey(String argument, SkinVariant variant) throws Exception;
    
    protected abstract Optional<Property> loadSkin(K key) throws Exception;
    
    protected void validate(String argument, SkinVariant variant) throws Exception {
        if (this.skinCache == null)
            throw new IllegalStateException("Provider not initialized");
    }
    
    protected void createSkinCache() {
        var config = this.getCacheConfig();
        var time = config.enabled() ? config.duration() : 0;
        
        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull K key) throws Exception {
                        return AbstractSkinProvider.this.loadSkin(key);
                    }
                });
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant) {
        try {
            this.validate(argument, variant);
            K key = this.getCacheKey(argument, variant);
            return Result.success(this.skinCache.get(key));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
}
