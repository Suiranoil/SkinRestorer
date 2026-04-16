package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.builtin.CollectionSkinProvider;
import net.lionarius.skinrestorer.skin.provider.builtin.ElyBySkinProvider;
import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.util.List;
import java.util.Locale;

public final class AutoFetchConfig implements GsonPostProcessable {
    private boolean enabled = true;

    private boolean overrideExisting = false;

    private List<String> providers = List.of(MojangSkinProvider.PROVIDER_NAME);

    public boolean enabled() {
        return this.enabled;
    }

    public boolean overrideExisting() {
        return this.overrideExisting;
    }

    public List<String> providers() {
        return this.providers;
    }

    @Override
    public void gsonPostProcess() {
        if (this.providers == null || this.providers.isEmpty()) {
            SkinRestorer.LOGGER.warn("AutoFetch providers config is null/empty, defaulting to MOJANG");
            this.providers = List.of(MojangSkinProvider.PROVIDER_NAME);
            return;
        }

        var normalized = this.providers.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(AutoFetchConfig::normalizeProvider)
                .filter(p -> !p.isEmpty())
                .toList();

        if (normalized.isEmpty()) {
            SkinRestorer.LOGGER.warn(
                    "AutoFetch providers config is empty after normalization, defaulting to MOJANG");
            this.providers = List.of(MojangSkinProvider.PROVIDER_NAME);
        } else {
            this.providers = normalized;
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
