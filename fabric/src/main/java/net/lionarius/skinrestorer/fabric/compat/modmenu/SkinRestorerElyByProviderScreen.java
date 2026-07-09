package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkinRestorerElyByProviderScreen extends AbstractConfigScreen {
    private boolean enabled;
    private String name;
    private boolean cacheEnabled;
    private long cacheDuration;

    public SkinRestorerElyByProviderScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.ely_by.title"), parent);

        var provider = SkinRestorer.getConfig().providers().ely_by();
        this.enabled = provider.enabled();
        this.name = provider.name();
        this.cacheEnabled = provider.cache().enabled();
        this.cacheDuration = provider.cache().duration();
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;

        y = this.addToggleRow(x, y, "skinrestorer.config.providers.enabled", this.enabled, value -> this.enabled = value);
        y = this.addTextRow(x, y, "skinrestorer.config.providers.name", this.name, value -> this.name = value);
        y = this.addToggleRow(
                x, y, "skinrestorer.config.providers.cache.enabled", this.cacheEnabled, value -> this.cacheEnabled =
                        value);
        y = this.addNumberRow(
                x,
                y,
                "skinrestorer.config.providers.cache.duration",
                Long.toString(this.cacheDuration),
                value -> this.cacheDuration =
                        SkinRestorerElyByProviderScreen.parseLongOrDefault(value, this.cacheDuration));

        return y;
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
        var provider = SkinRestorer.getConfig().providers().ely_by();
        provider.enabled(this.enabled);
        provider.name(this.name);
        provider.cache().enabled(this.cacheEnabled);
        provider.cache().duration(this.cacheDuration);
    }
}
