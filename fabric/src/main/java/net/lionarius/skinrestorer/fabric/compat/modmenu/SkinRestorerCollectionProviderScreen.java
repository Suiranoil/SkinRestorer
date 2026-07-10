package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinFile;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinSource;
import net.lionarius.skinrestorer.config.provider.collection.CollectionSkinUrl;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerCollectionProviderScreen extends AbstractConfigScreen {
    private final List<CollectionSkinSource> sources;

    private boolean enabled;
    private String name;
    private boolean cacheEnabled;
    private long cacheDuration;

    public SkinRestorerCollectionProviderScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.collection.title"), parent);

        var provider = SkinRestorer.getConfig().providers().collection();
        this.enabled = provider.enabled();
        this.name = provider.name();
        this.cacheEnabled = provider.cache().enabled();
        this.cacheDuration = provider.cache().duration();
        this.sources = new ArrayList<>(provider.sources());
    }

    @Override
    protected void buildRows(int x) {
        this.addToggleTextRow(
                x,
                "skinrestorer.config.providers.enabled",
                this.enabled,
                value -> this.enabled = value,
                "skinrestorer.config.providers.name",
                this.name,
                value -> this.name = value);
        this.addToggleTextRow(
                x,
                "skinrestorer.config.providers.cache.enabled",
                this.cacheEnabled,
                value -> this.cacheEnabled = value,
                "skinrestorer.config.providers.cache.duration",
                Long.toString(this.cacheDuration),
                value -> this.cacheDuration =
                        SkinRestorerCollectionProviderScreen.parseLongOrDefault(value, this.cacheDuration));

        for (var i = 0; i < this.sources.size(); i++) {
            var index = i;
            this.addButtonRow(x, SkinRestorerCollectionProviderScreen.describe(this.sources.get(i)), () -> this.openEditor(index));
        }
    }

    @Override
    protected int fixedBottomHeight() {
        return BUTTON_HEIGHT;
    }

    @Override
    protected void buildFixedBottomRows(int x, int y) {
        var halfWidth = HALF_ROW_WIDTH - 4;
        var rightX = x + HALF_ROW_WIDTH + 4;

        this.addFixedButtonRow(
                x, y, halfWidth, "skinrestorer.config.providers.collection.add_file", () -> this.openNew(new CollectionSkinFile()));
        this.addFixedButtonRow(
                rightX, y, halfWidth, "skinrestorer.config.providers.collection.add_url", () -> this.openNew(new CollectionSkinUrl()));
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Component describe(CollectionSkinSource source) {
        var variant = Component.translatable(
                source.variant() == SkinVariant.SLIM
                        ? "skinrestorer.config.providers.collection.variant.slim"
                        : "skinrestorer.config.providers.collection.variant.classic");

        if (source instanceof CollectionSkinFile file) {
            return Component.translatable("skinrestorer.config.providers.collection.entry.file", variant, file.path());
        }
        if (source instanceof CollectionSkinUrl url) {
            return Component.translatable("skinrestorer.config.providers.collection.entry.url", variant, url.url());
        }
        return Component.empty();
    }

    private void openEditor(int index) {
        assert this.minecraft != null;

        var source = this.sources.get(index);
        this.minecraft.setScreen(SkinRestorerCollectionProviderScreen.createEditor(
                this, source, edited -> this.sources.set(index, edited), () -> this.sources.remove(index)));
    }

    private void openNew(CollectionSkinSource blank) {
        assert this.minecraft != null;

        this.minecraft.setScreen(SkinRestorerCollectionProviderScreen.createEditor(this, blank, this.sources::add, null));
    }

    private static Screen createEditor(
            Screen parent, CollectionSkinSource source, Consumer<CollectionSkinSource> onCommit, Runnable onDelete) {
        if (source instanceof CollectionSkinFile file) return new SkinRestorerCollectionFileScreen(parent, file, onCommit, onDelete);
        if (source instanceof CollectionSkinUrl url) return new SkinRestorerCollectionUrlScreen(parent, url, onCommit, onDelete);
        throw new IllegalStateException("unknown collection skin source type: " + source.getClass());
    }

    @Override
    protected void onSave() {
        var provider = SkinRestorer.getConfig().providers().collection();
        provider.enabled(this.enabled);
        provider.name(this.name);
        provider.cache().enabled(this.cacheEnabled);
        provider.cache().duration(this.cacheDuration);
        provider.sources(this.sources);
    }
}
