package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.StorageConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class SkinRestorerConfigScreen extends AbstractConfigScreen {
    private String language;
    private StorageConfig.Location storageLocation;

    public SkinRestorerConfigScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.title"), parent);

        var config = SkinRestorer.getConfig();
        this.language = config.language();
        this.storageLocation = config.storage().location();
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;
        var halfX = this.centeredX(HALF_ROW_WIDTH);

        y = this.addButtonRow(
                halfX,
                y,
                HALF_ROW_WIDTH,
                Component.translatable("skinrestorer.config.language_button", this.language),
                () -> this.minecraft.setScreen(
                        new SkinRestorerLanguageScreen(this, this.language, value -> this.language = value)));

        y = this.addCycleRow(
                halfX,
                y,
                HALF_ROW_WIDTH,
                "skinrestorer.config.storage.location",
                SkinRestorerConfigScreen::storageLocationName,
                List.of(StorageConfig.Location.values()),
                this.storageLocation,
                value -> this.storageLocation = value);

        y = this.addButtonRow(
                halfX,
                y,
                HALF_ROW_WIDTH,
                "skinrestorer.config.join.title",
                () -> this.minecraft.setScreen(new SkinRestorerJoinScreen(this)));

        y = this.addButtonRow(
                halfX,
                y,
                HALF_ROW_WIDTH,
                "skinrestorer.config.request.title",
                () -> this.minecraft.setScreen(new SkinRestorerRequestScreen(this)));

        return y;
    }

    private static Component storageLocationName(StorageConfig.Location location) {
        return switch (location) {
            case WORLD -> Component.translatable("skinrestorer.config.storage.location.world");
            case GLOBAL -> Component.translatable("skinrestorer.config.storage.location.global");
        };
    }

    @Override
    protected void onSave() {
        var config = SkinRestorer.getConfig();
        config.language(this.language);
        config.storage().location(this.storageLocation);

        config.save(SkinRestorer.getConfigDir());
        SkinRestorer.reloadConfig();
    }
}
