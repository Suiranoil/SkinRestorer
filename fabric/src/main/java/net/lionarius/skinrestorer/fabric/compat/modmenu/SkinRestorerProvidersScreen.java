package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkinRestorerProvidersScreen extends AbstractConfigScreen {
    public SkinRestorerProvidersScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.title"), parent);
    }

    @Override
    protected void buildRows(int x) {
        this.addButtonRow(
                x, "skinrestorer.config.providers.mojang.title", () -> this.minecraft.setScreen(
                        new SkinRestorerMojangProviderScreen(this)));
        this.addButtonRow(
                x, "skinrestorer.config.providers.ely_by.title", () -> this.minecraft.setScreen(
                        new SkinRestorerElyByProviderScreen(this)));
        this.addButtonRow(
                x, "skinrestorer.config.providers.mineskin.title", () -> this.minecraft.setScreen(
                        new SkinRestorerMineskinProviderScreen(this)));
        this.addButtonRow(
                x, "skinrestorer.config.providers.collection.title", () -> this.minecraft.setScreen(
                        new SkinRestorerCollectionProviderScreen(this)));
        this.addButtonRow(
                x, "skinrestorer.config.providers.custom.title", () -> this.minecraft.setScreen(
                        new SkinRestorerCustomProvidersScreen(this)));
    }

    @Override
    protected void onSave() {}
}
