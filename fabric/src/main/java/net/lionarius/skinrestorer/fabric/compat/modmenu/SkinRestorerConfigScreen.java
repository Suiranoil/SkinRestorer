package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.StorageConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerConfigScreen extends Screen {
    private static final List<String> LANGUAGES = List.of(
            "cs_cz", "de_de", "en_us", "es_ar", "es_es", "es_mx", "fil_ph", "fr_ca", "fr_fr", "hi_in", "hu_hu",
            "id_id", "it_it", "ja_jp", "pl_pl", "pt_br", "pt_pt", "ru_ru", "tr_tr", "uk_ua", "vi_vn", "zh_cn",
            "zh_tw");

    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDTH = 320;

    private final Screen parent;

    private String language;
    private StorageConfig.Location storageLocation;
    private boolean joinRefreshSkin;
    private int joinApplyDelay;
    private boolean autoFetchEnabled;
    private boolean autoFetchOverrideExisting;
    private String requestProxy;
    private long requestTimeout;
    private String requestUserAgent;

    public SkinRestorerConfigScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.title"));
        this.parent = parent;

        var config = SkinRestorer.getConfig();
        this.language = config.language();
        this.storageLocation = config.storage().location();
        this.joinRefreshSkin = config.join().refreshSkin();
        this.joinApplyDelay = config.join().applyDelay();
        this.autoFetchEnabled = config.join().autoFetchConfig().enabled();
        this.autoFetchOverrideExisting = config.join().autoFetchConfig().overrideExisting();
        this.requestProxy = config.request().rawProxy();
        this.requestTimeout = config.request().timeout();
        this.requestUserAgent = config.request().userAgent();
    }

    @Override
    protected void init() {
        var x = (this.width - ROW_WIDTH) / 2;
        var y = Math.max(32, this.height / 6);

        y = this.addLanguageRow(x, y);
        y = this.addStorageLocationRow(x, y);
        y = this.addToggleRow(
                x, y, "skinrestorer.config.join.refresh_skin", this.joinRefreshSkin, value -> this.joinRefreshSkin =
                        value);
        y = this.addNumberRow(
                x,
                y,
                "skinrestorer.config.join.apply_delay",
                Integer.toString(this.joinApplyDelay),
                value -> this.joinApplyDelay = SkinRestorerConfigScreen.parseIntOrDefault(value, this.joinApplyDelay));
        y = this.addToggleRow(
                x,
                y,
                "skinrestorer.config.join.auto_fetch.enabled",
                this.autoFetchEnabled,
                value -> this.autoFetchEnabled = value);
        y = this.addToggleRow(
                x,
                y,
                "skinrestorer.config.join.auto_fetch.override_existing",
                this.autoFetchOverrideExisting,
                value -> this.autoFetchOverrideExisting = value);
        y = this.addTextRow(
                x, y, "skinrestorer.config.request.proxy", this.requestProxy, value -> this.requestProxy = value);
        y = this.addNumberRow(
                x,
                y,
                "skinrestorer.config.request.timeout",
                Long.toString(this.requestTimeout),
                value -> this.requestTimeout =
                        SkinRestorerConfigScreen.parseLongOrDefault(value, this.requestTimeout));
        y = this.addTextRow(
                x,
                y,
                "skinrestorer.config.request.user_agent",
                this.requestUserAgent,
                value -> this.requestUserAgent = value);

        y += 8;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onSave())
                .bounds(x, y, ROW_WIDTH / 2 - 4, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(x + ROW_WIDTH / 2 + 4, y, ROW_WIDTH / 2 - 4, 20)
                .build());
    }

    private int addLanguageRow(int x, int y) {
        var index = Math.max(0, LANGUAGES.indexOf(this.language));
        this.addRenderableWidget(CycleButton.builder((String code) -> Component.literal(code))
                .withValues(LANGUAGES)
                .withInitialValue(LANGUAGES.get(index))
                .create(
                        x,
                        y,
                        ROW_WIDTH,
                        20,
                        Component.translatable("skinrestorer.config.language"),
                        (button, value) -> this.language = value));

        return y + ROW_HEIGHT;
    }

    private int addStorageLocationRow(int x, int y) {
        this.addRenderableWidget(CycleButton.builder(SkinRestorerConfigScreen::storageLocationName)
                .withValues(StorageConfig.Location.values())
                .withInitialValue(this.storageLocation)
                .create(
                        x,
                        y,
                        ROW_WIDTH,
                        20,
                        Component.translatable("skinrestorer.config.storage.location"),
                        (button, value) -> this.storageLocation = value));

        return y + ROW_HEIGHT;
    }

    private int addToggleRow(int x, int y, String labelKey, boolean initial, Consumer<Boolean> onChange) {
        this.addRenderableWidget(CycleButton.onOffBuilder(initial)
                .create(x, y, ROW_WIDTH, 20, Component.translatable(labelKey), (button, value) -> onChange.accept(value)));

        return y + ROW_HEIGHT;
    }

    private int addTextRow(int x, int y, String labelKey, String initial, Consumer<String> onChange) {
        var label = Component.translatable(labelKey);
        this.addRenderableWidget(new StringWidget(x, y, ROW_WIDTH, 12, label, this.font));

        var box = new EditBox(this.font, x, y + 12, ROW_WIDTH, 18, label);
        box.setValue(initial);
        box.setResponder(onChange::accept);
        this.addRenderableWidget(box);

        return y + ROW_HEIGHT + 12;
    }

    private int addNumberRow(int x, int y, String labelKey, String initial, Consumer<String> onChange) {
        var label = Component.translatable(labelKey);
        this.addRenderableWidget(new StringWidget(x, y, ROW_WIDTH, 12, label, this.font));

        var box = new EditBox(this.font, x, y + 12, ROW_WIDTH, 18, label);
        box.setFilter(value -> value.matches("\\d*"));
        box.setValue(initial);
        box.setResponder(onChange::accept);
        this.addRenderableWidget(box);

        return y + ROW_HEIGHT + 12;
    }

    private static Component storageLocationName(StorageConfig.Location location) {
        return switch (location) {
            case WORLD -> Component.translatable("skinrestorer.config.storage.location.world");
            case GLOBAL -> Component.translatable("skinrestorer.config.storage.location.global");
        };
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void onSave() {
        var config = SkinRestorer.getConfig();
        config.language(this.language);
        config.storage().location(this.storageLocation);
        config.join().refreshSkin(this.joinRefreshSkin);
        config.join().applyDelay(this.joinApplyDelay);
        config.join().autoFetchConfig().enabled(this.autoFetchEnabled);
        config.join().autoFetchConfig().overrideExisting(this.autoFetchOverrideExisting);
        config.request().rawProxy(this.requestProxy);
        config.request().timeout(this.requestTimeout);
        config.request().userAgent(this.requestUserAgent);

        config.save(SkinRestorer.getConfigDir());
        SkinRestorer.reloadConfig();

        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.parent);
    }
}
