package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkinRestorerMineskinProviderScreen extends AbstractConfigScreen {
    private boolean enabled;
    private String name;
    private boolean cacheEnabled;
    private long cacheDuration;
    private String apiKey;
    private boolean proxyUrlUpload;

    public SkinRestorerMineskinProviderScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.mineskin.title"), parent);

        var provider = SkinRestorer.getConfig().providers().mineskin();
        this.enabled = provider.enabled();
        this.name = provider.name();
        this.cacheEnabled = provider.cache().enabled();
        this.cacheDuration = provider.cache().duration();
        this.apiKey = provider.apiKey();
        this.proxyUrlUpload = provider.proxyUrlUpload();
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
                        SkinRestorerMineskinProviderScreen.parseLongOrDefault(value, this.cacheDuration));
        this.addTextRow(
                x, "skinrestorer.config.providers.mineskin.api_key", this.apiKey, value -> this.apiKey = value);
        this.addToggleRow(
                x,
                "skinrestorer.config.providers.mineskin.proxy_url_upload",
                this.proxyUrlUpload,
                value -> this.proxyUrlUpload = value);
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void onSave() {
        var provider = SkinRestorer.getConfig().providers().mineskin();
        provider.enabled(this.enabled);
        provider.name(this.name);
        provider.cache().enabled(this.cacheEnabled);
        provider.cache().duration(this.cacheDuration);
        provider.apiKey(this.apiKey);
        provider.proxyUrlUpload(this.proxyUrlUpload);
    }
}
