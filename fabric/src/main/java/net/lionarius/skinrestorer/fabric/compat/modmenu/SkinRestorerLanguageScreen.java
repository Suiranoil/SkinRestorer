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

        for (var code : LANGUAGES) {
            var marker = code.equals(this.initialLanguage) ? "✓ " : "";
            y = this.addButtonRow(x, y, Component.literal(marker + code), () -> {
                this.onSelect.accept(code);
                this.onClose();
            });
        }

        return y;
    }

    @Override
    protected void onSave() {}
}
