package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerLanguageScreen extends AbstractConfigScreen {
    static final List<String> LANGUAGES = List.of(
            "cs_cz", "de_de", "en_us", "es_ar", "es_es", "es_mx", "fil_ph", "fr_ca", "fr_fr", "hi_in", "hu_hu",
            "id_id", "it_it", "ja_jp", "pl_pl", "pt_br", "pt_pt", "ru_ru", "tr_tr", "uk_ua", "vi_vn", "zh_cn",
            "zh_tw");

    private static final int COLUMNS = 3;
    private static final int COLUMN_GAP = 8;

    private final String initialLanguage;
    private final Consumer<String> onSelect;

    public SkinRestorerLanguageScreen(Screen parent, String initialLanguage, Consumer<String> onSelect) {
        super(Component.translatable("skinrestorer.config.language"), parent);
        this.initialLanguage = initialLanguage;
        this.onSelect = onSelect;
    }

    @Override
    protected boolean showDoneButton() {
        return false;
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;
        var columnWidth = (ROW_WIDTH - (COLUMNS - 1) * COLUMN_GAP) / COLUMNS;

        for (var i = 0; i < LANGUAGES.size(); i += COLUMNS) {
            for (var column = 0; column < COLUMNS && i + column < LANGUAGES.size(); column++) {
                var code = LANGUAGES.get(i + column);
                var marker = code.equals(this.initialLanguage) ? "✓ " : "";
                var columnX = x + column * (columnWidth + COLUMN_GAP);

                this.addButtonRow(columnX, y, columnWidth, Component.literal(marker + code), () -> {
                    this.onSelect.accept(code);
                    this.onClose();
                });
            }

            y += ROW_HEIGHT;
        }

        return y;
    }

    @Override
    protected void onSave() {}
}
