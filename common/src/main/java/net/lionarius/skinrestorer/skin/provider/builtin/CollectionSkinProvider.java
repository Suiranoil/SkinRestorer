package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.base.AbstractSkinProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CollectionSkinProvider extends AbstractSkinProvider<Integer> {
    public static final String PROVIDER_NAME = "collection";

    private final SkinSigner skinSigner;

    private List<Pair<URI, SkinVariant>> collectionSkins;
    private Map<String, Integer> namedSkins = Collections.emptyMap();

    public CollectionSkinProvider(SkinSigner skinSigner) {
        this.skinSigner = skinSigner;
    }

    @Override
    public void reload() {
        this.loadCollectionSkins();
        this.createSkinCache();
    }

    private void loadCollectionSkins() {
        List<Pair<URI, SkinVariant>> skins = new ArrayList<>();
        Map<String, Integer> names = new LinkedHashMap<>();

        var config = SkinRestorer.getConfig().providers().collection();

        for (CollectionSkinSource source : config.sources()) {
            var uri = source.uri();
            if (uri == null) continue;

            skins.add(Pair.of(uri, source.variant()));

            var name = source.name();
            if (name != null && names.putIfAbsent(name, skins.size() - 1) != null)
                SkinRestorer.LOGGER.warn("Duplicate collection skin name: {}", name);
        }

        this.collectionSkins = skins;
        this.namedSkins = names;
    }

    @Override
    public String getProviderName() {
        return CollectionSkinProvider.PROVIDER_NAME;
    }

    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.USERNAME;
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
    protected CacheConfig getCacheConfig() {
        return SkinRestorer.getConfig().providers().collection().cache();
    }

    @Override
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);
        if (this.collectionSkins.isEmpty()) throw new IllegalStateException("No collection skins configured");
    }

    @Override
    public Collection<String> getArgumentSuggestions() {
        return this.namedSkins.keySet();
    }

    @Override
    protected Integer getCacheKey(String argument, SkinVariant variant) {
        var named = this.namedSkins.get(argument);
        if (named != null) return named;

        return Math.floorMod(argument.hashCode(), this.collectionSkins.size());
    }

    @Override
    protected Optional<Property> loadSkin(Integer key) throws Exception {
        var skinEntry = this.collectionSkins.get(key);
        return this.skinSigner.signSkin(skinEntry.first(), skinEntry.second());
    }
}
