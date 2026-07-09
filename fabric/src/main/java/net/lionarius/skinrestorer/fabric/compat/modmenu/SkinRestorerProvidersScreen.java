package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkinRestorerProvidersScreen extends AbstractConfigScreen {
    public SkinRestorerProvidersScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.title"), parent);
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;

        y = this.addButtonRow(
                x, y, "skinrestorer.config.providers.mojang.title", () -> this.minecraft.setScreen(
                        new SkinRestorerMojangProviderScreen(this)));
        y = this.addButtonRow(
                x, y, "skinrestorer.config.providers.ely_by.title", () -> this.minecraft.setScreen(
                        new SkinRestorerElyByProviderScreen(this)));
        y = this.addButtonRow(
                x, y, "skinrestorer.config.providers.mineskin.title", () -> this.minecraft.setScreen(
                        new SkinRestorerMineskinProviderScreen(this)));
        y = this.addButtonRow(
                x, y, "skinrestorer.config.providers.collection.title", () -> this.minecraft.setScreen(
                        new SkinRestorerCollectionProviderScreen(this)));
        y = this.addButtonRow(
                x, y, "skinrestorer.config.providers.custom.title", () -> this.minecraft.setScreen(
                        new SkinRestorerCustomProvidersScreen(this)));

        return y;
    }

    @Override
    protected void onSave() {}
}
