package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared scaffolding for the mod's config screens: rows are appended to a vanilla
 * {@link ContainerObjectSelectionList} (the same widget vanilla itself uses for scrollable
 * option/resource-pack/mod lists), so the darkened background, scrollbar and row clipping all
 * match vanilla exactly instead of being hand-rolled. A fixed header (title) and footer
 * (Done/Cancel) sit outside the list.
 */
abstract class AbstractConfigScreen extends Screen {
    protected static final int ROW_HEIGHT = 24;
    protected static final int ROW_WIDTH = 320;
    protected static final int HALF_ROW_WIDTH = ROW_WIDTH / 2;
    protected static final int PAIR_GAP = 8;

    protected static final int BUTTON_HEIGHT = 20;

    private static final int HEADER_HEIGHT = 32;
    // matches the gap a plain button row already has below it (ROW_HEIGHT is taller than the
    // BUTTON_HEIGHT widget it holds), so the first row's spacing looks the same as the rest
    private static final int INNER_TOP_PADDING = ROW_HEIGHT - BUTTON_HEIGHT;
    private static final int PANEL_GAP = 6;
    private static final int BUTTON_GAP = 6;
    private static final int BOTTOM_MARGIN = 8;

    protected final Screen parent;

    private RowList rowList;
    private int contentHeight;

    protected AbstractConfigScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Appends the screen's rows to the list. */
    protected abstract void buildRows(int x);

    /** Applies the screen's edited fields to the live config. */
    protected abstract void onSave();

    /** Override to return {@code false} for picker-style screens that apply and close per-row. */
    protected boolean showDoneButton() {
        return true;
    }

    /** Override to return {@code true} for short screens that should sit in the middle of the list. */
    protected boolean centerVertically() {
        return false;
    }

    /** Override to reserve space below the list for fixed (non-scrolling) rows, e.g. {@code BUTTON_HEIGHT}. */
    protected int fixedBottomHeight() {
        return 0;
    }

    /** Adds fixed rows pinned below the list, starting at {@code y}. No-op unless overridden. */
    protected void buildFixedBottomRows(int x, int y) {}

    protected int centeredX(int width) {
        return (this.width - width) / 2;
    }

    @Override
    protected final void init() {
        var x = (this.width - ROW_WIDTH) / 2;

        // laid out bottom-up so every gap is an explicit, small constant instead of
        // whatever slack happens to be left over above the footer
        var footerY = this.height - BOTTOM_MARGIN - BUTTON_HEIGHT;
        var fixedBottomHeight = this.fixedBottomHeight();
        var listBottom = fixedBottomHeight > 0
                ? footerY - BUTTON_GAP - fixedBottomHeight - PANEL_GAP
                : footerY - PANEL_GAP;
        var listHeight = listBottom - HEADER_HEIGHT;

        assert this.minecraft != null;
        this.rowList = new RowList(
                this.minecraft, this.width, listHeight, HEADER_HEIGHT, ROW_HEIGHT, this.centerVertically());
        this.addRenderableWidget(this.rowList);

        // top-aligned screens get a small invisible spacer so their first row doesn't
        // touch the panel's top edge; centered screens are balanced manually below instead
        if (!this.centerVertically()) this.addWidgetsRow(INNER_TOP_PADDING);

        this.buildRows(x);

        if (this.centerVertically()) {
            // ContainerObjectSelectionList's own centerListVertically field turned out to only
            // affect scroll-to-entry behaviour, not the resting layout of a short list - so the
            // actual centering is done here, using the real total height of what buildRows just
            // added, retroactively inserted as a blank spacer above everything via addEntryToTop
            var topPad = (listHeight - this.contentHeight) / 2;
            if (topPad > 0) this.rowList.addRowToTop(new RowEntry(List.of(), new int[0]), topPad);
        }

        if (fixedBottomHeight > 0) this.buildFixedBottomRows(x, listBottom + PANEL_GAP);

        if (this.showDoneButton()) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> {
                        this.onSave();
                        this.onClose();
                    })
                    .bounds(x, footerY, HALF_ROW_WIDTH - 4, BUTTON_HEIGHT)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                    .bounds(x + HALF_ROW_WIDTH + 4, footerY, HALF_ROW_WIDTH - 4, BUTTON_HEIGHT)
                    .build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                    .bounds(x, footerY, ROW_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private static final class RowEntry extends ContainerObjectSelectionList.Entry<RowEntry> {
        private final List<AbstractWidget> widgets;
        private final int[] offsetsY;

        RowEntry(List<AbstractWidget> widgets, int[] offsetsY) {
            this.widgets = widgets;
            this.offsetsY = offsetsY;
        }

        @Override
        public void extractContent(
                GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            for (var i = 0; i < this.widgets.size(); i++) {
                var widget = this.widgets.get(i);
                widget.setPosition(widget.getX(), this.getY() + this.offsetsY[i]);
                widget.extractRenderState(graphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            this.widgets.forEach(consumer);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.widgets;
        }
    }

    private static final class RowList extends ContainerObjectSelectionList<RowEntry> {
        RowList(Minecraft minecraft, int width, int height, int top, int itemHeight, boolean centerVertically) {
            super(minecraft, width, height, top, itemHeight);
            this.centerListVertically = centerVertically;
        }

        @Override
        protected boolean entriesCanBeSelected() {
            return false;
        }

        // without this, the row content column (and therefore the scrollbar, which vanilla
        // positions just to the right of it) defaults to a width unrelated to our own ROW_WIDTH,
        // leaving the scrollbar sitting awkwardly close to the middle of the screen
        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        // bridges the protected addEntry(E, int)/addEntryToTop(E, int) from AbstractSelectionList,
        // which AbstractConfigScreen can't call directly since it isn't a subclass of that vanilla type
        void addRow(RowEntry entry, int height) {
            this.addEntry(entry, height);
        }

        void addRowToTop(RowEntry entry, int height) {
            this.addEntryToTop(entry, height);
        }
    }

    private void addEntry(int height, List<AbstractWidget> widgets, int[] offsetsY) {
        this.contentHeight += height;
        this.rowList.addRow(new RowEntry(widgets, offsetsY), height);
    }

    /** Adds one or more widgets sharing a single row, all at the same vertical offset. */
    protected void addWidgetsRow(int height, AbstractWidget... widgets) {
        this.addEntry(height, List.of(widgets), new int[widgets.length]);
    }

    protected <T> void addCycleRow(
            int x, String labelKey, Function<T, Component> valueToText, List<T> values, T initial, Consumer<T> onChange) {
        this.addCycleRow(x, ROW_WIDTH, labelKey, valueToText, values, initial, onChange);
    }

    protected <T> void addCycleRow(
            int x,
            int width,
            String labelKey,
            Function<T, Component> valueToText,
            List<T> values,
            T initial,
            Consumer<T> onChange) {
        var widget = CycleButton.builder(valueToText, initial)
                .withValues(values)
                .create(
                        x,
                        0,
                        width,
                        20,
                        Component.translatable(labelKey),
                        (button, value) -> onChange.accept(value));
        this.addWidgetsRow(ROW_HEIGHT, widget);
    }

    protected void addToggleRow(int x, String labelKey, boolean initial, Consumer<Boolean> onChange) {
        this.addToggleRow(x, ROW_WIDTH, labelKey, initial, onChange);
    }

    protected void addToggleRow(int x, int width, String labelKey, boolean initial, Consumer<Boolean> onChange) {
        var widget = CycleButton.onOffBuilder(initial)
                .create(
                        x,
                        0,
                        width,
                        20,
                        Component.translatable(labelKey),
                        (button, value) -> onChange.accept(value));
        this.addWidgetsRow(ROW_HEIGHT, widget);
    }

    protected void addTextRow(int x, String labelKey, String initial, Consumer<String> onChange) {
        var label = Component.translatable(labelKey);
        var labelWidget = new StringWidget(x, 0, ROW_WIDTH, 12, label, this.font);

        var box = new EditBox(this.font, x, 0, ROW_WIDTH, 18, label);
        box.setValue(initial);
        box.setResponder(onChange::accept);

        this.addEntry(ROW_HEIGHT + 12, List.of(labelWidget, box), new int[] {0, 12});
    }

    protected void addNumberRow(int x, String labelKey, String initial, Consumer<String> onChange) {
        // non-digit input is simply ignored on save (callers fall back to the previous value);
        // EditBox has no input filter in this Minecraft version
        this.addTextRow(x, labelKey, initial, onChange);
    }

    /**
     * A toggle on the left half and a labelled text field on the right half, sharing one row.
     * Halves are {@code HALF_ROW_WIDTH - 4} wide with an 8px gap, so the pair's total width
     * matches ROW_WIDTH exactly (the same trick the Done/Cancel footer buttons use) and both
     * controls share the same 18px height, so the two halves line up top and bottom.
     */
    protected void addToggleTextRow(
            int x,
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
                        0,
                        halfWidth,
                        18,
                        Component.translatable(toggleLabelKey),
                        (button, value) -> onToggleChange.accept(value));

        var label = Component.translatable(textLabelKey);
        var labelWidget = new StringWidget(rightX, 0, halfWidth, 12, label, this.font);

        var box = new EditBox(this.font, rightX, 0, halfWidth, 18, label);
        box.setValue(textInitial);
        box.setResponder(onTextChange::accept);

        this.addEntry(ROW_HEIGHT + 12, List.of(toggle, labelWidget, box), new int[] {12, 0, 12});
    }

    protected void addButtonRow(int x, String labelKey, Runnable onClick) {
        this.addButtonRow(x, ROW_WIDTH, Component.translatable(labelKey), onClick);
    }

    protected void addButtonRow(int x, int width, String labelKey, Runnable onClick) {
        this.addButtonRow(x, width, Component.translatable(labelKey), onClick);
    }

    protected void addButtonRow(int x, Component label, Runnable onClick) {
        this.addButtonRow(x, ROW_WIDTH, label, onClick);
    }

    protected void addButtonRow(int x, int width, Component label, Runnable onClick) {
        var widget = Button.builder(label, button -> onClick.run())
                .bounds(x, 0, width, 20)
                .build();
        this.addWidgetsRow(ROW_HEIGHT, widget);
    }

    /** Adds a button pinned at a fixed position below the list, unaffected by scrolling. */
    protected void addFixedButtonRow(int x, int y, int width, String labelKey, Runnable onClick) {
        this.addRenderableWidget(Button.builder(Component.translatable(labelKey), button -> onClick.run())
                .bounds(x, y, width, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        var titleText = this.title.getString();
        // centered against the panel's own real top (not just the HEADER_HEIGHT constant we
        // asked for), so it stays centered even if the list ends up positioned slightly differently
        var titleY = (this.rowList.getY() - this.font.lineHeight) / 2;
        graphics.text(this.font, titleText, (this.width - this.font.width(titleText)) / 2, titleY, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(this.parent);
    }
}
