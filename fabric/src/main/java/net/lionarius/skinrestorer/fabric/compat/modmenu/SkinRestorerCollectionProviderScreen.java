package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinFile;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinUrl;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkinRestorerCollectionProviderScreen extends AbstractConfigScreen {
    private boolean enabled;
    private String name;
    private boolean cacheEnabled;
    private long cacheDuration;
    private String sources;

    public SkinRestorerCollectionProviderScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.collection.title"), parent);

        var provider = SkinRestorer.getConfig().providers().collection();
        this.enabled = provider.enabled();
        this.name = provider.name();
        this.cacheEnabled = provider.cache().enabled();
        this.cacheDuration = provider.cache().duration();
        this.sources = SkinRestorerCollectionProviderScreen.encode(provider.sources());
    }

    @Override
    protected void buildRows(int x) {
        this.addToggleTextRow(
                x,
                "skinrestorer.config.providers.enabled",
                this.enabled,
                value -> this.enabled = value,
                "skinrestorer.config.providers.name",
                this.name,
                value -> this.name = value);
        this.addToggleTextRow(
                x,
                "skinrestorer.config.providers.cache.enabled",
                this.cacheEnabled,
                value -> this.cacheEnabled = value,
                "skinrestorer.config.providers.cache.duration",
                Long.toString(this.cacheDuration),
                value -> this.cacheDuration =
                        SkinRestorerCollectionProviderScreen.parseLongOrDefault(value, this.cacheDuration));
        this.addTextRow(
                x,
                "skinrestorer.config.providers.collection.sources",
                this.sources,
                value -> this.sources = value);
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String encode(List<CollectionSkinSource> sources) {
        var parts = new ArrayList<String>();
        for (var source : sources) {
            var variant = source.variant() == SkinVariant.SLIM ? "slim" : "classic";
            if (source instanceof CollectionSkinFile file) parts.add(variant + ":" + file.path());
            else if (source instanceof CollectionSkinUrl url) parts.add(variant + ":" + url.url());
        }

        return String.join("; ", parts);
    }

    private static List<CollectionSkinSource> decode(String raw) {
        var result = new ArrayList<CollectionSkinSource>();

        for (var part : raw.split(";")) {
            var trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            var variant = SkinVariant.CLASSIC;
            var value = trimmed;

            var colon = trimmed.indexOf(':');
            if (colon > 0) {
                var prefix = trimmed.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                if (prefix.equals("classic") || prefix.equals("slim")) {
                    variant = prefix.equals("slim") ? SkinVariant.SLIM : SkinVariant.CLASSIC;
                    value = trimmed.substring(colon + 1).trim();
                }
            }

            if (value.isEmpty()) continue;

            if (value.startsWith("http://") || value.startsWith("https://")) {
                var url = new CollectionSkinUrl();
                url.url(value);
                url.variant(variant);
                result.add(url);
            } else {
                var file = new CollectionSkinFile();
                file.path(value);
                file.variant(variant);
                result.add(file);
            }
        }

        return result;
    }

    @Override
    protected void onSave() {
        var provider = SkinRestorer.getConfig().providers().collection();
        provider.enabled(this.enabled);
        provider.name(this.name);
        provider.cache().enabled(this.cacheEnabled);
        provider.cache().duration(this.cacheDuration);
        provider.sources(SkinRestorerCollectionProviderScreen.decode(this.sources));
    }
}
