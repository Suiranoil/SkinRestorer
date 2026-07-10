package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.config.provider.custom.CustomProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomUsernameUrlProviderConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class SkinRestorerCustomUsernameUrlScreen extends AbstractConfigScreen {
    private final CustomUsernameUrlProviderConfig provider;
    private final Consumer<CustomProviderConfig> onCommit;
    private final Runnable onDelete;

    private boolean enabled;
    private String name;
    private boolean cacheEnabled;
    private long cacheDuration;
    private boolean useProviderSignature;
    private String urlTemplate;

    public SkinRestorerCustomUsernameUrlScreen(
            Screen parent,
            CustomUsernameUrlProviderConfig provider,
            Consumer<CustomProviderConfig> onCommit,
            Runnable onDelete) {
        super(Component.translatable("skinrestorer.config.providers.custom.username_url.title"), parent);
        this.provider = provider;
        this.onCommit = onCommit;
        this.onDelete = onDelete;

        this.enabled = provider.enabled();
        this.name = provider.name();
        this.cacheEnabled = provider.cache().enabled();
        this.cacheDuration = provider.cache().duration();
        this.useProviderSignature = provider.useProviderSignature();
        this.urlTemplate = provider.urlTemplate();
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
                        SkinRestorerCustomUsernameUrlScreen.parseLongOrDefault(value, this.cacheDuration));
        y = this.addToggleRow(
                x,
                y,
                "skinrestorer.config.providers.custom.use_provider_signature",
                this.useProviderSignature,
                value -> this.useProviderSignature = value);
        y = this.addTextRow(
                x,
                y,
                "skinrestorer.config.providers.custom.username_url.url_template",
                this.urlTemplate,
                value -> this.urlTemplate = value);

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
        this.provider.urlTemplate(this.urlTemplate);

        this.onCommit.accept(this.provider);
    }
}
