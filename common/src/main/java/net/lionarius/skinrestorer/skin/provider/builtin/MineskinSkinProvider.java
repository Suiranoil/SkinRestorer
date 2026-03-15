package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.mineskin.MineskinService;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.skin.provider.base.AbstractSkinProvider;

import java.net.URI;
import java.util.Optional;

public final class MineskinSkinProvider extends AbstractSkinProvider<Pair<URI, SkinVariant>> {

    public static final String PROVIDER_NAME = "web";

    private final MineskinService skinSigner;

    public MineskinSkinProvider(MineskinService skinSigner) {
        this.skinSigner = skinSigner;
    }

    @Override
    public void reload() {
        this.createSkinCache();
    }

    @Override
    public String getProviderName() {
        return MineskinSkinProvider.PROVIDER_NAME;
    }

    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.CUSTOM;
    }

    @Override
    public String getArgumentName() {
        return "url";
    }

    @Override
    public boolean hasVariantSupport() {
        return true;
    }

    @Override
    protected CacheConfig getCacheConfig() {
        return SkinRestorer.getConfig().providers().mineskin().cache();
    }

    @Override
    protected Pair<URI, SkinVariant> getCacheKey(String argument, SkinVariant variant) throws Exception {
        return Pair.of(new URI(argument), variant);
    }

    @Override
    protected Optional<Property> loadSkin(Pair<URI, SkinVariant> key) throws Exception {
        return this.skinSigner.signSkin(key.first(), key.second());
    }
}
