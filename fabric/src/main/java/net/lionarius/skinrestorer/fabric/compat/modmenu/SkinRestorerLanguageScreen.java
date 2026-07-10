package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
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
    protected void buildRows(int x) {
        var columnWidth = (ROW_WIDTH - (COLUMNS - 1) * COLUMN_GAP) / COLUMNS;

        for (var i = 0; i < LANGUAGES.size(); i += COLUMNS) {
            var widgets = new ArrayList<AbstractWidget>();

            for (var column = 0; column < COLUMNS && i + column < LANGUAGES.size(); column++) {
                var code = LANGUAGES.get(i + column);
                var marker = code.equals(this.initialLanguage) ? "✓ " : "";
                var columnX = x + column * (columnWidth + COLUMN_GAP);

                widgets.add(Button.builder(Component.literal(marker + code), button -> {
                            this.onSelect.accept(code);
                            this.onClose();
                        })
                        .bounds(columnX, 0, columnWidth, 20)
                        .build());
            }

            this.addWidgetsRow(ROW_HEIGHT, widgets.toArray(new AbstractWidget[0]));
        }
    }

    @Override
    protected void onSave() {}
}
