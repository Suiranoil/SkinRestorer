package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.util.PlayerUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SkinResigner {

    private final SkinSigner skinSigner;
    private boolean useProviderSignature;
    private Cache<Integer, Property> signatureCache;

    public SkinResigner(SkinSigner skinSigner) {
        this.skinSigner = skinSigner;
    }

    public void reload(boolean useProviderSignature) {
        this.useProviderSignature = useProviderSignature;

        if (useProviderSignature) {
            this.signatureCache = null;
        } else {
            this.signatureCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(24, TimeUnit.HOURS)
                    .maximumSize(1000)
                    .build();
        }
    }

    public Optional<Property> extractSkin(GameProfile profile) throws Exception {
        if (this.useProviderSignature) return Optional.ofNullable(PlayerUtils.getPlayerSkin(profile));

        var skin = PlayerUtils.getPlayerSkin(profile);
        if (skin == null) return Optional.empty();

        if (PlayerUtils.getSkinUrl(skin) == null) return Optional.empty();

        var propertyHash = skin.value().hashCode();
        var cached = this.signatureCache != null ? this.signatureCache.getIfPresent(propertyHash) : null;
        if (cached != null) return Optional.of(cached);

        var signed = this.skinSigner.signSkin(skin);
        signed.ifPresent(property -> {
            if (this.signatureCache != null) this.signatureCache.put(propertyHash, property);
        });

        return signed;
    }
}
