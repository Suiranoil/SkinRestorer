package net.lionarius.skinrestorer.skin.provider.base;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class ProfileSkinProvider extends AbstractSkinProvider<UUID> {
    private LoadingCache<String, UUID> uuidCache;

    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.USERNAME;
    }

    @Override
    public String getArgumentName() {
        return "username";
    }

    @Override
    public boolean hasVariantSupport() {
        return false;
    }

    @Override
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);
        if (!StringUtil.isValidPlayerName(argument)) throw new IllegalArgumentException("invalid username");
    }

    @Override
    protected void createSkinCache() {
        super.createSkinCache();

        var config = this.getCacheConfig();
        var time = config.enabled() ? config.duration() : 0;

        this.uuidCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull UUID load(@NotNull String username) throws Exception {
                        return ProfileSkinProvider.this.lookupUuid(username);
                    }
                });
    }

    @Override
    protected UUID getCacheKey(String argument, SkinVariant variant) throws Exception {
        return this.resolveUuid(argument.toLowerCase(Locale.ROOT));
    }

    protected UUID resolveUuid(String username) throws Exception {
        return this.uuidCache.get(username);
    }

    @Override
    protected Optional<Property> loadSkin(UUID uuid) throws Exception {
        GameProfile profile = this.fetchProfileWithProperties(uuid);
        return this.extractSkin(profile);
    }

    protected abstract UUID lookupUuid(String username) throws Exception;

    protected abstract GameProfile fetchProfileWithProperties(UUID uuid) throws Exception;

    protected Optional<Property> extractSkin(GameProfile profile) throws Exception {
        return Optional.ofNullable(PlayerUtils.getPlayerSkin(profile));
    }
}
