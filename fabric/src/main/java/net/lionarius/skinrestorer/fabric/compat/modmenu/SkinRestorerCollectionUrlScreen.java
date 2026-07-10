package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinUrl;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerCollectionUrlScreen extends AbstractConfigScreen {
    private final CollectionSkinUrl source;
    private final Consumer<CollectionSkinSource> onCommit;
    private final Runnable onDelete;

    private String url;
    private SkinVariant variant;

    public SkinRestorerCollectionUrlScreen(
            Screen parent, CollectionSkinUrl source, Consumer<CollectionSkinSource> onCommit, Runnable onDelete) {
        super(
                Component.translatable(
                        onDelete == null
                                ? "skinrestorer.config.providers.collection.url.add_title"
                                : "skinrestorer.config.providers.collection.url.title"),
                parent);
        this.source = source;
        this.onCommit = onCommit;
        this.onDelete = onDelete;

        this.url = source.url();
        this.variant = source.variant();
    }

    @Override
    protected void buildRows(int x) {
        this.addTextRow(x, "skinrestorer.config.providers.collection.url.value", this.url, value -> this.url = value);
        this.addCycleRow(
                x,
                "skinrestorer.config.providers.collection.variant",
                SkinRestorerCollectionUrlScreen::variantName,
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
        this.source.url(this.url);
        this.source.variant(this.variant);

        this.onCommit.accept(this.source);
    }
}
