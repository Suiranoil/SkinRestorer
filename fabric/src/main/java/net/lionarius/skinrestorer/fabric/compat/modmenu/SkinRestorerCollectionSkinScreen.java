package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinFile;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinUrl;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerCollectionSkinScreen extends AbstractConfigScreen {
    private final Consumer<CollectionSkinSource> onCommit;
    private final Runnable onDelete;

    private String value;
    private SkinVariant variant;

    public SkinRestorerCollectionSkinScreen(
            Screen parent, CollectionSkinSource source, Consumer<CollectionSkinSource> onCommit, Runnable onDelete) {
        super(
                Component.translatable(
                        onDelete == null
                                ? "skinrestorer.config.providers.collection.skin.add_title"
                                : "skinrestorer.config.providers.collection.skin.title"),
                parent);
        this.onCommit = onCommit;
        this.onDelete = onDelete;

        this.value = SkinRestorerCollectionSkinScreen.valueOf(source);
        this.variant = source.variant();
    }

    private static String valueOf(CollectionSkinSource source) {
        if (source instanceof CollectionSkinFile file) return file.path();
        if (source instanceof CollectionSkinUrl url) return url.url();
        return "";
    }

    @Override
    protected void buildRows(int x) {
        this.addTextRow(
                x, "skinrestorer.config.providers.collection.skin.value", this.value, value -> this.value = value);
        this.addCycleRow(
                x,
                "skinrestorer.config.providers.collection.variant",
                SkinRestorerCollectionSkinScreen::variantName,
                List.of(SkinVariant.CLASSIC, SkinVariant.SLIM),
                this.variant,
                value -> this.variant = value);

        if (this.onDelete != null) {
            this.addButtonRow(x, "skinrestorer.config.providers.collection.delete", () -> {
                this.onDelete.run();
                this.onClose();
            });
        }
    }

    private static Component variantName(SkinVariant variant) {
        return Component.translatable(
                variant == SkinVariant.SLIM
                        ? "skinrestorer.config.providers.collection.variant.slim"
                        : "skinrestorer.config.providers.collection.variant.classic");
    }

    @Override
    protected void onSave() {
        CollectionSkinSource result;
        if (this.value.startsWith("http://") || this.value.startsWith("https://")) {
            var url = new CollectionSkinUrl();
            url.url(this.value);
            url.variant(this.variant);
            result = url;
        } else {
            var file = new CollectionSkinFile();
            file.path(this.value);
            file.variant(this.variant);
            result = file;
        }

        this.onCommit.accept(result);
    }
}
