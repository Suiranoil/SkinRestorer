package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.StorageConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class SkinRestorerConfigScreen extends AbstractConfigScreen {
    private static final List<String> LANGUAGES = List.of(
            "cs_cz", "de_de", "en_us", "es_ar", "es_es", "es_mx", "fil_ph", "fr_ca", "fr_fr", "hi_in", "hu_hu",
            "id_id", "it_it", "ja_jp", "pl_pl", "pt_br", "pt_pt", "ru_ru", "tr_tr", "uk_ua", "vi_vn", "zh_cn",
            "zh_tw");

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

        var index = Math.max(0, LANGUAGES.indexOf(this.language));
        y = this.addCycleRow(
                x,
                y,
                "skinrestorer.config.language",
                Component::literal,
                LANGUAGES,
                LANGUAGES.get(index),
                value -> this.language = value);

        y = this.addCycleRow(
                x,
                y,
                "skinrestorer.config.storage.location",
                SkinRestorerConfigScreen::storageLocationName,
                List.of(StorageConfig.Location.values()),
                this.storageLocation,
                value -> this.storageLocation = value);

        y = this.addButtonRow(
                x, y, "skinrestorer.config.join.title", () -> this.minecraft.setScreen(new SkinRestorerJoinScreen(this)));

        y = this.addButtonRow(
                x,
                y,
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
