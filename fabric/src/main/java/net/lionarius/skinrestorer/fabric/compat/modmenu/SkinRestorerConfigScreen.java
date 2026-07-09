package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.StorageConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerConfigScreen extends Screen {
    private static final List<String> LANGUAGES = List.of(
            "cs_cz", "de_de", "en_us", "es_ar", "es_es", "es_mx", "fil_ph", "fr_ca", "fr_fr", "hi_in", "hu_hu",
            "id_id", "it_it", "ja_jp", "pl_pl", "pt_br", "pt_pt", "ru_ru", "tr_tr", "uk_ua", "vi_vn", "zh_cn",
            "zh_tw");

    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 28;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDTH = 320;

    private final Screen parent;
    private final List<ScrollEntry> entries = new ArrayList<>();
    private int scrollAmount = 0;
    private int contentHeight = 0;

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

    private record ScrollEntry(AbstractWidget widget, int relativeY) {}

    @Override
    protected void init() {
        this.entries.clear();

        var x = (this.width - ROW_WIDTH) / 2;
        var y = 0;

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

        this.contentHeight = y;
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll()));
        this.updatePositions();

        var footerY = this.height - FOOTER_HEIGHT + 4;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onSave())
                .bounds(x, footerY, ROW_WIDTH / 2 - 4, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(x + ROW_WIDTH / 2 + 4, footerY, ROW_WIDTH / 2 - 4, 20)
                .build());
    }

    private int maxScroll() {
        var viewport = Math.max(0, this.height - HEADER_HEIGHT - FOOTER_HEIGHT);
        return Math.max(0, this.contentHeight - viewport);
    }

    private void updatePositions() {
        for (var entry : this.entries) {
            entry.widget().setPosition(entry.widget().getX(), HEADER_HEIGHT + entry.relativeY() - this.scrollAmount);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var previous = this.scrollAmount;
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount - (int) (scrollY * ROW_HEIGHT), this.maxScroll()));
        if (this.scrollAmount != previous) this.updatePositions();

        return true;
    }

    private <T extends AbstractWidget> T addRow(T widget, int relativeY) {
        this.addRenderableWidget(widget);
        this.entries.add(new ScrollEntry(widget, relativeY));

        return widget;
    }

    private int addLanguageRow(int x, int y) {
        var index = Math.max(0, LANGUAGES.indexOf(this.language));
        this.addRow(
                CycleButton.builder((String code) -> Component.literal(code), LANGUAGES.get(index))
                        .withValues(LANGUAGES)
                        .create(
                                x,
                                y,
                                ROW_WIDTH,
                                20,
                                Component.translatable("skinrestorer.config.language"),
                                (button, value) -> this.language = value),
                y);

        return y + ROW_HEIGHT;
    }

    private int addStorageLocationRow(int x, int y) {
        this.addRow(
                CycleButton.builder(SkinRestorerConfigScreen::storageLocationName, this.storageLocation)
                        .withValues(StorageConfig.Location.values())
                        .create(
                                x,
                                y,
                                ROW_WIDTH,
                                20,
                                Component.translatable("skinrestorer.config.storage.location"),
                                (button, value) -> this.storageLocation = value),
                y);

        return y + ROW_HEIGHT;
    }

    private int addToggleRow(int x, int y, String labelKey, boolean initial, Consumer<Boolean> onChange) {
        this.addRow(
                CycleButton.onOffBuilder(initial)
                        .create(
                                x,
                                y,
                                ROW_WIDTH,
                                20,
                                Component.translatable(labelKey),
                                (button, value) -> onChange.accept(value)),
                y);

        return y + ROW_HEIGHT;
    }

    private int addTextRow(int x, int y, String labelKey, String initial, Consumer<String> onChange) {
        var label = Component.translatable(labelKey);
        this.addRow(new StringWidget(x, y, ROW_WIDTH, 12, label, this.font), y);

        var box = new EditBox(this.font, x, y + 12, ROW_WIDTH, 18, label);
        box.setValue(initial);
        box.setResponder(onChange::accept);
        this.addRow(box, y + 12);

        return y + ROW_HEIGHT + 12;
    }

    private int addNumberRow(int x, int y, String labelKey, String initial, Consumer<String> onChange) {
        var label = Component.translatable(labelKey);
        this.addRow(new StringWidget(x, y, ROW_WIDTH, 12, label, this.font), y);

        var box = new EditBox(this.font, x, y + 12, ROW_WIDTH, 18, label);
        box.setValue(initial);
        // non-digit input is simply ignored on save (parseIntOrDefault/parseLongOrDefault fall back
        // to the previous value); EditBox has no input filter in this Minecraft version
        box.setResponder(onChange::accept);
        this.addRow(box, y + 12);

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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        var titleText = this.title.getString();
        graphics.text(this.font, titleText, (this.width - this.font.width(titleText)) / 2, 12, 0xFFFFFFFF, true);

        this.renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        var maxScroll = this.maxScroll();
        if (maxScroll <= 0) return;

        var trackX = (this.width - ROW_WIDTH) / 2 + ROW_WIDTH + 6;
        var trackTop = HEADER_HEIGHT;
        var trackBottom = this.height - FOOTER_HEIGHT;
        var trackHeight = trackBottom - trackTop;

        graphics.fill(trackX, trackTop, trackX + 4, trackBottom, 0x40FFFFFF);

        var thumbHeight = Math.max(12, trackHeight * trackHeight / (trackHeight + maxScroll));
        var thumbTop = trackTop + (trackHeight - thumbHeight) * this.scrollAmount / maxScroll;
        graphics.fill(trackX, thumbTop, trackX + 4, thumbTop + thumbHeight, 0xFFC0C0C0);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.parent);
    }
}
