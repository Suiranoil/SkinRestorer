package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public final class SkinRestorerJoinScreen extends AbstractConfigScreen {
    private boolean refreshSkin;
    private int applyDelay;
    private String skipRefreshProviders;

    public SkinRestorerJoinScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.join.title"), parent);

        var join = SkinRestorer.getConfig().join();
        this.refreshSkin = join.refreshSkin();
        this.applyDelay = join.applyDelay();
        this.skipRefreshProviders = String.join(", ", join.skipRefreshProviders());
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;

        y = this.addToggleRow(
                x, y, "skinrestorer.config.join.refresh_skin", this.refreshSkin, value -> this.refreshSkin = value);
        y = this.addNumberRow(
                x,
                y,
                "skinrestorer.config.join.apply_delay",
                Integer.toString(this.applyDelay),
                value -> this.applyDelay = SkinRestorerJoinScreen.parseIntOrDefault(value, this.applyDelay));
        y = this.addTextRow(
                x,
                y,
                "skinrestorer.config.join.skip_refresh_providers",
                this.skipRefreshProviders,
                value -> this.skipRefreshProviders = value);
        y = this.addButtonRow(
                x,
                y,
                "skinrestorer.config.join.auto_fetch.title",
                () -> this.minecraft.setScreen(new SkinRestorerAutoFetchScreen(this)));

        return y;
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static List<String> parseList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    protected void onSave() {
        var join = SkinRestorer.getConfig().join();
        join.refreshSkin(this.refreshSkin);
        join.applyDelay(this.applyDelay);
        join.skipRefreshProviders(SkinRestorerJoinScreen.parseList(this.skipRefreshProviders));
    }
}
