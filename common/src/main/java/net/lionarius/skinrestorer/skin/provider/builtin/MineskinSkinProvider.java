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
import java.util.List;
import java.util.Locale;
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
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);

        var uri = new URI(argument);
        var scheme = uri.getScheme();
        if ((!"http".equals(scheme) && !"https".equals(scheme)) || uri.getHost() == null)
            throw new IllegalArgumentException("only http(s) urls are allowed");

        var allowedDomains = SkinRestorer.getConfig().providers().mineskin().allowedDomains();
        if (allowedDomains.isEmpty()) return;

        if (!MineskinSkinProvider.isDomainAllowed(uri.getHost(), allowedDomains))
            throw new IllegalArgumentException("domain '" + uri.getHost() + "' is not allowed");
    }

    private static boolean isDomainAllowed(String host, List<String> allowedDomains) {
        var normalizedHost = host.toLowerCase(Locale.ROOT);

        for (var domain : allowedDomains) {
            if (domain.startsWith("*.")) {
                if (normalizedHost.endsWith(domain.substring(1))) return true;
            } else if (normalizedHost.equals(domain)) {
                return true;
            }
        }

        return false;
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
