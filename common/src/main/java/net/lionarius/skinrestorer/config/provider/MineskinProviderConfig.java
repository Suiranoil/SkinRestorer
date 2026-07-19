package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.builtin.MineskinSkinProvider;

import java.util.List;
import java.util.Locale;

public final class MineskinProviderConfig extends BuiltInProviderConfig {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 300);

    private String apiKey;
    private boolean proxyUrlUpload;
    private List<String> allowedDomains;

    public MineskinProviderConfig() {
        super(MineskinSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);

        this.apiKey = "";
        this.proxyUrlUpload = false;
        this.allowedDomains = List.of();
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean proxyUrlUpload() {
        return this.proxyUrlUpload;
    }

    public List<String> allowedDomains() {
        return this.allowedDomains;
    }

    @Override
    public void gsonPostProcess() {
        super.validate(MineskinSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);

        if (this.apiKey == null) {
            SkinRestorer.LOGGER.warn("Mineskin API key is null, defaulting to an empty string");
            this.apiKey = "";
        }

        if (this.allowedDomains == null) {
            this.allowedDomains = List.of();
        } else {
            this.allowedDomains = this.allowedDomains.stream()
                    .filter(domain -> domain != null && !domain.isBlank())
                    .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                    .toList();
        }
    }
}
