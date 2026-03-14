package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.builtin.CollectionSkinProvider;
import net.lionarius.skinrestorer.skin.provider.builtin.ElyBySkinProvider;
import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.util.Locale;

public final class AutoFetchConfig implements GsonPostProcessable {

    private boolean enabled = true;

    private boolean overrideExisting = false;

    private String provider = MojangSkinProvider.PROVIDER_NAME;

    public boolean enabled() {
        return this.enabled;
    }

    public boolean overrideExisting() {
        return this.overrideExisting;
    }

    public String provider() {
        return this.provider;
    }

    @Override
    public void gsonPostProcess() {
        if (this.provider == null) {
            SkinRestorer.LOGGER.warn("AutoFetch provider config is null, defaulting to MOJANG");
            this.provider = MojangSkinProvider.PROVIDER_NAME;
        } else if (this.provider.isBlank()) {
            SkinRestorer.LOGGER.warn("AutoFetch provider config is empty, defaulting to MOJANG");
            this.provider = MojangSkinProvider.PROVIDER_NAME;
        } else {
            this.provider = AutoFetchConfig.normalizeProvider(this.provider);

            if (this.provider.isEmpty()) {
                SkinRestorer.LOGGER.warn("AutoFetch provider config is empty after normalization, defaulting to MOJANG");
                this.provider = MojangSkinProvider.PROVIDER_NAME;
            }
        }
    }

    private static String normalizeProvider(String value) {
        var normalized = value.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "mojang" -> MojangSkinProvider.PROVIDER_NAME;
            case "ely.by", "ely_by" -> ElyBySkinProvider.PROVIDER_NAME;
            case "collection" -> CollectionSkinProvider.PROVIDER_NAME;
            default -> value.trim();
        };
    }
}
