package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared scaffolding for the mod's config screens: a vertically scrollable list of
 * label+control rows with a fixed header (title) and footer (Done/Cancel), a darkened
 * panel background behind the content, and a draggable scrollbar.
 */
abstract class AbstractConfigScreen extends Screen {
    protected static final int HEADER_HEIGHT = 24;
    protected static final int FOOTER_HEIGHT = 28;
    protected static final int ROW_HEIGHT = 24;
    protected static final int ROW_WIDTH = 320;
    protected static final int HALF_ROW_WIDTH = ROW_WIDTH / 2;

    protected static final int PAIR_GAP = 8;

    private static final int PANEL_WIDTH = ROW_WIDTH + 40;
    private static final int SCROLLBAR_GAP = 6;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int TOP_PADDING = 6;

    protected final Screen parent;

    private final List<ScrollEntry> entries = new ArrayList<>();
    private int scrollAmount = 0;
    private int contentHeight = 0;
    private int verticalOffset = 0;
    private boolean draggingScrollbar = false;

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

    /** Override to return {@code true} for short screens that should sit in the middle of the viewport. */
    protected boolean centerVertically() {
        return false;
    }

    /** Override to reserve space at the bottom of the panel for fixed (non-scrolling) rows. */
    protected int fixedBottomHeight() {
        return 0;
    }

    /** Adds fixed rows pinned to the bottom of the panel, below {@code y}. No-op unless overridden. */
    protected void buildFixedBottomRows(int x, int y) {}

    protected int centeredX(int width) {
        return (this.width - width) / 2;
    }

    @Override
    protected final void init() {
        this.entries.clear();

        var x = (this.width - ROW_WIDTH) / 2;
        this.contentHeight = this.buildRows(x);

        this.verticalOffset = this.centerVertically()
                ? Math.max(0, (this.viewportHeight() - this.contentHeight) / 2)
                : TOP_PADDING;
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, this.maxScroll()));
        this.updatePositions();

        if (this.fixedBottomHeight() > 0) this.buildFixedBottomRows(x, this.contentBottom());

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

    private int contentBottom() {
        return this.height - FOOTER_HEIGHT - this.fixedBottomHeight();
    }

    private int viewportHeight() {
        var padding = this.centerVertically() ? 0 : TOP_PADDING;
        return Math.max(0, this.contentBottom() - HEADER_HEIGHT - padding);
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - this.viewportHeight());
    }

    private void updatePositions() {
        var contentBottom = this.contentBottom();

        for (var entry : this.entries) {
            var widget = entry.widget();
            var y = HEADER_HEIGHT + this.verticalOffset + entry.relativeY() - this.scrollAmount;
            widget.setPosition(widget.getX(), y);
            // widgets that would poke out past the darkened panel are hidden instead of clipped
            widget.visible = y >= HEADER_HEIGHT && y + widget.getHeight() <= contentBottom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var previous = this.scrollAmount;
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount - (int) (scrollY * ROW_HEIGHT), this.maxScroll()));
        if (this.scrollAmount != previous) this.updatePositions();

        return true;
    }

    private int scrollbarTrackX() {
        return (this.width - ROW_WIDTH) / 2 + ROW_WIDTH + SCROLLBAR_GAP;
    }

    private int scrollbarThumbHeight(int trackHeight, int maxScroll) {
        return Math.max(12, trackHeight * trackHeight / (trackHeight + maxScroll));
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (this.maxScroll() <= 0) return false;

        var trackX = this.scrollbarTrackX();
        return mouseX >= trackX - 2
                && mouseX <= trackX + SCROLLBAR_WIDTH + 2
                && mouseY >= HEADER_HEIGHT
                && mouseY <= this.contentBottom();
    }

    private void scrollToMouseY(double mouseY) {
        var maxScroll = this.maxScroll();
        if (maxScroll <= 0) return;

        var trackTop = HEADER_HEIGHT;
        var trackHeight = this.contentBottom() - trackTop;
        var thumbHeight = this.scrollbarThumbHeight(trackHeight, maxScroll);
        var usableTrack = Math.max(1, trackHeight - thumbHeight);

        var fraction = (mouseY - trackTop - thumbHeight / 2.0) / usableTrack;
        fraction = Math.max(0, Math.min(1, fraction));

        this.scrollAmount = (int) Math.round(fraction * maxScroll);
        this.updatePositions();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isOverScrollbar(event.x(), event.y())) {
            this.draggingScrollbar = true;
            this.scrollToMouseY(event.y());
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            this.scrollToMouseY(event.y());
            return true;
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    /**
     * Rows are registered as interactive children only (not "renderable" widgets) - they're
     * drawn manually in extractRenderState within a scissor region clipped to the panel, while
     * footer/fixed-bottom widgets stay on the normal addRenderableWidget path and render
     * unclipped via the super call.
     */
    protected <T extends AbstractWidget> T addRow(T widget, int relativeY) {
        this.addWidget(widget);
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
        return this.addToggleRow(x, y, ROW_WIDTH, labelKey, initial, onChange);
    }

    protected int addToggleRow(
            int x, int y, int width, String labelKey, boolean initial, Consumer<Boolean> onChange) {
        this.addRow(
                CycleButton.onOffBuilder(initial)
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

    /**
     * A toggle on the left half and a labelled text field on the right half, sharing one row.
     * Halves are {@code HALF_ROW_WIDTH - 4} wide with an 8px gap, so the pair's total width
     * matches ROW_WIDTH exactly (the same trick the Done/Cancel footer buttons use) and both
     * controls share the same 18px height, so the two halves line up top and bottom.
     */
    protected int addToggleTextRow(
            int x,
            int y,
            String toggleLabelKey,
            boolean toggleInitial,
            Consumer<Boolean> onToggleChange,
            String textLabelKey,
            String textInitial,
            Consumer<String> onTextChange) {
        var halfWidth = HALF_ROW_WIDTH - 4;
        var rightX = x + HALF_ROW_WIDTH + 4;

        var toggle = CycleButton.onOffBuilder(toggleInitial)
                .create(
                        x,
                        y + 12,
                        halfWidth,
                        18,
                        Component.translatable(toggleLabelKey),
                        (button, value) -> onToggleChange.accept(value));
        this.addRow(toggle, y + 12);

        var label = Component.translatable(textLabelKey);
        this.addRow(new StringWidget(rightX, y, halfWidth, 12, label, this.font), y);

        var box = new EditBox(this.font, rightX, y + 12, halfWidth, 18, label);
        box.setValue(textInitial);
        box.setResponder(onTextChange::accept);
        this.addRow(box, y + 12);

        return y + ROW_HEIGHT + 12;
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

    /** Adds a button pinned at a fixed position within the panel, unaffected by scrolling. */
    protected void addFixedButtonRow(int x, int y, int width, String labelKey, Runnable onClick) {
        this.addRenderableWidget(Button.builder(Component.translatable(labelKey), button -> onClick.run())
                .bounds(x, y, width, 20)
                .build());
    }

    private void renderPanelBackground(GuiGraphicsExtractor graphics) {
        var panelX = this.centeredX(PANEL_WIDTH);
        graphics.fill(panelX, HEADER_HEIGHT, panelX + PANEL_WIDTH, this.height - FOOTER_HEIGHT, 0x60000000);
    }

    private void renderDivider(GuiGraphicsExtractor graphics) {
        if (this.fixedBottomHeight() <= 0) return;

        var panelX = this.centeredX(PANEL_WIDTH);
        var dividerY = this.contentBottom();
        graphics.fill(panelX + 10, dividerY, panelX + PANEL_WIDTH - 10, dividerY + 1, 0x80FFFFFF);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        var maxScroll = this.maxScroll();
        if (maxScroll <= 0) return;

        var trackX = this.scrollbarTrackX();
        var trackTop = HEADER_HEIGHT;
        var trackBottom = this.contentBottom();
        var trackHeight = trackBottom - trackTop;

        graphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom, 0x40FFFFFF);

        var thumbHeight = this.scrollbarThumbHeight(trackHeight, maxScroll);
        var thumbTop = trackTop + (trackHeight - thumbHeight) * this.scrollAmount / maxScroll;
        graphics.fill(trackX, thumbTop, trackX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xFFC0C0C0);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.renderPanelBackground(graphics);

        var panelX = this.centeredX(PANEL_WIDTH);
        graphics.enableScissor(panelX, HEADER_HEIGHT, panelX + PANEL_WIDTH, this.contentBottom());
        for (var entry : this.entries) {
            entry.widget().extractRenderState(graphics, mouseX, mouseY, delta);
        }
        graphics.disableScissor();

        // footer + fixed-bottom widgets go through addRenderableWidget, so this only renders those
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        var titleText = this.title.getString();
        graphics.text(this.font, titleText, (this.width - this.font.width(titleText)) / 2, 12, 0xFFFFFFFF, true);

        this.renderScrollbar(graphics);
        this.renderDivider(graphics);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.parent);
    }
}
