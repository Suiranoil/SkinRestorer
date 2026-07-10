package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.config.provider.custom.CustomProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomYggdrasilProviderConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class SkinRestorerCustomYggdrasilScreen extends AbstractConfigScreen {
    private final CustomYggdrasilProviderConfig provider;
    private final Consumer<CustomProviderConfig> onCommit;
    private final Runnable onDelete;

    private boolean enabled;
    private String name;
    private boolean cacheEnabled;
    private long cacheDuration;
    private boolean useProviderSignature;
    private String baseUrl;

    public SkinRestorerCustomYggdrasilScreen(
            Screen parent,
            CustomYggdrasilProviderConfig provider,
            Consumer<CustomProviderConfig> onCommit,
            Runnable onDelete) {
        super(Component.translatable("skinrestorer.config.providers.custom.yggdrasil.title"), parent);
        this.provider = provider;
        this.onCommit = onCommit;
        this.onDelete = onDelete;

        this.enabled = provider.enabled();
        this.name = provider.name();
        this.cacheEnabled = provider.cache().enabled();
        this.cacheDuration = provider.cache().duration();
        this.useProviderSignature = provider.useProviderSignature();
        this.baseUrl = provider.baseUrl();
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;

        y = this.addToggleTextRow(
                x,
                y,
                "skinrestorer.config.providers.enabled",
                this.enabled,
                value -> this.enabled = value,
                "skinrestorer.config.providers.name",
                this.name,
                value -> this.name = value);
        y = this.addToggleTextRow(
                x,
                y,
                "skinrestorer.config.providers.cache.enabled",
                this.cacheEnabled,
                value -> this.cacheEnabled = value,
                "skinrestorer.config.providers.cache.duration",
                Long.toString(this.cacheDuration),
                value -> this.cacheDuration =
                        SkinRestorerCustomYggdrasilScreen.parseLongOrDefault(value, this.cacheDuration));
        y = this.addToggleRow(
                x,
                y,
                "skinrestorer.config.providers.custom.use_provider_signature",
                this.useProviderSignature,
                value -> this.useProviderSignature = value);
        y = this.addTextRow(
                x,
                y,
                "skinrestorer.config.providers.custom.yggdrasil.base_url",
                this.baseUrl,
                value -> this.baseUrl = value);

        if (this.onDelete != null) {
            y = this.addButtonRow(x, y, "skinrestorer.config.providers.custom.delete", () -> {
                this.onDelete.run();
                this.onClose();
            });
        }

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
        this.provider.enabled(this.enabled);
        this.provider.name(this.name);
        this.provider.cache().enabled(this.cacheEnabled);
        this.provider.cache().duration(this.cacheDuration);
        this.provider.useProviderSignature(this.useProviderSignature);
        this.provider.baseUrl(this.baseUrl);

        this.onCommit.accept(this.provider);
    }
}
