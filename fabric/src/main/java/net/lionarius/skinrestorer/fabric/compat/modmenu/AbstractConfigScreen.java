package net.lionarius.skinrestorer.fabric.compat.modmenu;

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
import java.util.function.Function;

/**
 * Shared scaffolding for the mod's config screens: a vertically scrollable list of
 * label+control rows with a fixed header (title) and footer (Done/Cancel).
 */
abstract class AbstractConfigScreen extends Screen {
    protected static final int HEADER_HEIGHT = 24;
    protected static final int FOOTER_HEIGHT = 28;
    protected static final int ROW_HEIGHT = 24;
    protected static final int ROW_WIDTH = 320;
    protected static final int HALF_ROW_WIDTH = ROW_WIDTH / 2;

    protected final Screen parent;

    private final List<ScrollEntry> entries = new ArrayList<>();
    private int scrollAmount = 0;
    private int contentHeight = 0;

    protected AbstractConfigScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    private record ScrollEntry(AbstractWidget widget, int relativeY) {}

    /** Adds the screen's rows starting at relative y {@code 0} and returns the total content height. */
    protected abstract int buildRows(int x);

    /** Applies the screen's edited fields to the live config. */
    protected abstract void onSave();

    /** Override to return {@code false} for picker-style screens that apply and close per-row. */
    protected boolean showDoneButton() {
        return true;
    }

    protected int centeredX(int width) {
        return (this.width - width) / 2;
    }

    @Override
    protected final void init() {
        this.entries.clear();

        var x = (this.width - ROW_WIDTH) / 2;
        this.contentHeight = this.buildRows(x);
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll()));
        this.updatePositions();

        var footerY = this.height - FOOTER_HEIGHT + 4;
        if (this.showDoneButton()) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
                        this.onSave();
                        this.onClose();
                    })
                    .bounds(x, footerY, HALF_ROW_WIDTH - 4, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                    .bounds(x + HALF_ROW_WIDTH + 4, footerY, HALF_ROW_WIDTH - 4, 20)
                    .build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                    .bounds(x, footerY, ROW_WIDTH, 20)
                    .build());
        }
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

    protected <T extends AbstractWidget> T addRow(T widget, int relativeY) {
        this.addRenderableWidget(widget);
        this.entries.add(new ScrollEntry(widget, relativeY));

        return widget;
    }

    protected <T> int addCycleRow(
            int x,
            int y,
            String labelKey,
            Function<T, Component> valueToText,
            List<T> values,
            T initial,
            Consumer<T> onChange) {
        return this.addCycleRow(x, y, ROW_WIDTH, labelKey, valueToText, values, initial, onChange);
    }

    protected <T> int addCycleRow(
            int x,
            int y,
            int width,
            String labelKey,
            Function<T, Component> valueToText,
            List<T> values,
            T initial,
            Consumer<T> onChange) {
        this.addRow(
                CycleButton.builder(valueToText, initial)
                        .withValues(values)
                        .create(
                                x,
                                y,
                                width,
                                20,
                                Component.translatable(labelKey),
                                (button, value) -> onChange.accept(value)),
                y);

        return y + ROW_HEIGHT;
    }

    protected int addToggleRow(int x, int y, String labelKey, boolean initial, Consumer<Boolean> onChange) {
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

    protected int addTextRow(int x, int y, String labelKey, String initial, Consumer<String> onChange) {
        var label = Component.translatable(labelKey);
        this.addRow(new StringWidget(x, y, ROW_WIDTH, 12, label, this.font), y);

        var box = new EditBox(this.font, x, y + 12, ROW_WIDTH, 18, label);
        box.setValue(initial);
        box.setResponder(onChange::accept);
        this.addRow(box, y + 12);

        return y + ROW_HEIGHT + 12;
    }

    protected int addNumberRow(int x, int y, String labelKey, String initial, Consumer<String> onChange) {
        // non-digit input is simply ignored on save (callers fall back to the previous value);
        // EditBox has no input filter in this Minecraft version
        return this.addTextRow(x, y, labelKey, initial, onChange);
    }

    protected int addButtonRow(int x, int y, String labelKey, Runnable onClick) {
        return this.addButtonRow(x, y, ROW_WIDTH, Component.translatable(labelKey), onClick);
    }

    protected int addButtonRow(int x, int y, int width, String labelKey, Runnable onClick) {
        return this.addButtonRow(x, y, width, Component.translatable(labelKey), onClick);
    }

    protected int addButtonRow(int x, int y, Component label, Runnable onClick) {
        return this.addButtonRow(x, y, ROW_WIDTH, label, onClick);
    }

    protected int addButtonRow(int x, int y, int width, Component label, Runnable onClick) {
        this.addRow(
                Button.builder(label, button -> onClick.run()).bounds(x, y, width, 20).build(), y);

        return y + ROW_HEIGHT;
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        var titleText = this.title.getString();
        graphics.text(this.font, titleText, (this.width - this.font.width(titleText)) / 2, 12, 0xFFFFFFFF, true);

        this.renderScrollbar(graphics);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.parent);
    }
}
