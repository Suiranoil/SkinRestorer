package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.StorageConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
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
    protected boolean centerVertically() {
        return true;
    }

    @Override
    protected void buildRows(int x) {
        var leftX = this.centeredX(HALF_ROW_WIDTH * 2 + PAIR_GAP);
        var rightX = leftX + HALF_ROW_WIDTH + PAIR_GAP;

        var languageButton = Button.builder(
                        Component.translatable("skinrestorer.config.language_button", this.language),
                        button -> this.minecraft.setScreen(new SkinRestorerLanguageScreen(
                                this, this.language, value -> this.language = value)))
                .bounds(leftX, 0, HALF_ROW_WIDTH, 20)
                .build();
        var storageLocationButton = CycleButton.builder(SkinRestorerConfigScreen::storageLocationName, this.storageLocation)
                .withValues(List.of(StorageConfig.Location.values()))
                .create(
                        rightX,
                        0,
                        HALF_ROW_WIDTH,
                        20,
                        Component.translatable("skinrestorer.config.storage.location"),
                        (button, value) -> this.storageLocation = value);
        this.addWidgetsRow(ROW_HEIGHT, languageButton, storageLocationButton);

        var joinButton = Button.builder(
                        Component.translatable("skinrestorer.config.join.title"),
                        button -> this.minecraft.setScreen(new SkinRestorerJoinScreen(this)))
                .bounds(leftX, 0, HALF_ROW_WIDTH, 20)
                .build();
        var requestButton = Button.builder(
                        Component.translatable("skinrestorer.config.request.title"),
                        button -> this.minecraft.setScreen(new SkinRestorerRequestScreen(this)))
                .bounds(rightX, 0, HALF_ROW_WIDTH, 20)
                .build();
        this.addWidgetsRow(ROW_HEIGHT, joinButton, requestButton);

        this.addButtonRow(
                this.centeredX(HALF_ROW_WIDTH),
                HALF_ROW_WIDTH,
                "skinrestorer.config.providers.title",
                () -> this.minecraft.setScreen(new SkinRestorerProvidersScreen(this)));
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
