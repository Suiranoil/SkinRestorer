package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinFile;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerCollectionFileScreen extends AbstractConfigScreen {
    private final CollectionSkinFile source;
    private final Consumer<CollectionSkinSource> onCommit;
    private final Runnable onDelete;

    private String path;
    private SkinVariant variant;

    public SkinRestorerCollectionFileScreen(
            Screen parent, CollectionSkinFile source, Consumer<CollectionSkinSource> onCommit, Runnable onDelete) {
        super(
                Component.translatable(
                        onDelete == null
                                ? "skinrestorer.config.providers.collection.file.add_title"
                                : "skinrestorer.config.providers.collection.file.title"),
                parent);
        this.source = source;
        this.onCommit = onCommit;
        this.onDelete = onDelete;

        this.path = source.path();
        this.variant = source.variant();
    }

    @Override
    protected void buildRows(int x) {
        this.addTextRow(x, "skinrestorer.config.providers.collection.file.path", this.path, value -> this.path = value);
        this.addCycleRow(
                x,
                "skinrestorer.config.providers.collection.variant",
                SkinRestorerCollectionFileScreen::variantName,
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
        this.source.path(this.path);
        this.source.variant(this.variant);

        this.onCommit.accept(this.source);
    }
}
