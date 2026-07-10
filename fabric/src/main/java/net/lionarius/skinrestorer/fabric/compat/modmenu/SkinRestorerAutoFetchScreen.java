package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public final class SkinRestorerAutoFetchScreen extends AbstractConfigScreen {
    private boolean enabled;
    private boolean overrideExisting;
    private String providers;

    public SkinRestorerAutoFetchScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.join.auto_fetch.title"), parent);

        var autoFetch = SkinRestorer.getConfig().join().autoFetchConfig();
        this.enabled = autoFetch.enabled();
        this.overrideExisting = autoFetch.overrideExisting();
        this.providers = String.join(", ", autoFetch.providers());
    }

    @Override
    protected void buildRows(int x) {
        this.addToggleRow(
                x, "skinrestorer.config.join.auto_fetch.enabled", this.enabled, value -> this.enabled = value);
        this.addToggleRow(
                x,
                "skinrestorer.config.join.auto_fetch.override_existing",
                this.overrideExisting,
                value -> this.overrideExisting = value);
        this.addTextRow(
                x,
                "skinrestorer.config.join.auto_fetch.providers",
                this.providers,
                value -> this.providers = value);
    }

    private static List<String> parseList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    protected void onSave() {
        var autoFetch = SkinRestorer.getConfig().join().autoFetchConfig();
        autoFetch.enabled(this.enabled);
        autoFetch.overrideExisting(this.overrideExisting);
        autoFetch.providers(SkinRestorerAutoFetchScreen.parseList(this.providers));
    }
}
