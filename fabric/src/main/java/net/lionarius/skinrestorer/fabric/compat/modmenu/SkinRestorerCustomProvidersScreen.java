package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.custom.CustomAuthlibInjectorProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomProviderType;
import net.lionarius.skinrestorer.config.provider.custom.CustomUsernameUrlProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomYggdrasilProviderConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SkinRestorerCustomProvidersScreen extends AbstractConfigScreen {
    private final List<CustomProviderConfig> providers;

    public SkinRestorerCustomProvidersScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.providers.custom.title"), parent);
        this.providers = new ArrayList<>(SkinRestorer.getConfig().providers().custom());
    }

    @Override
    protected void buildRows(int x) {
        for (var i = 0; i < this.providers.size(); i++) {
            var provider = this.providers.get(i);
            if (provider.type() == CustomProviderType.UNKNOWN) continue;

            var index = i;
            var label = Component.literal(provider.name() + " (" + provider.type() + ")");
            this.addButtonRow(x, label, () -> this.openEditor(index));
        }
    }

    @Override
    protected int fixedBottomHeight() {
        return BUTTON_HEIGHT;
    }

    @Override
    protected void buildFixedBottomRows(int x, int y) {
        var columnWidth = (ROW_WIDTH - 2 * PAIR_GAP) / 3;

        this.addFixedButtonRow(
                x,
                y,
                columnWidth,
                "skinrestorer.config.providers.custom.add_yggdrasil",
                () -> this.openNew(new CustomYggdrasilProviderConfig()));
        this.addFixedButtonRow(
                x + columnWidth + PAIR_GAP,
                y,
                columnWidth,
                "skinrestorer.config.providers.custom.add_authlib_injector",
                () -> this.openNew(new CustomAuthlibInjectorProviderConfig()));
        this.addFixedButtonRow(
                x + 2 * (columnWidth + PAIR_GAP),
                y,
                columnWidth,
                "skinrestorer.config.providers.custom.add_username_url",
                () -> this.openNew(new CustomUsernameUrlProviderConfig()));
    }

    private void openEditor(int index) {
        assert this.minecraft != null;

        var provider = this.providers.get(index);
        this.minecraft.setScreen(SkinRestorerCustomProvidersScreen.createEditor(
                this, provider, edited -> this.providers.set(index, edited), () -> this.providers.remove(index)));
    }

    private void openNew(CustomProviderConfig blank) {
        assert this.minecraft != null;

        this.minecraft.setScreen(
                SkinRestorerCustomProvidersScreen.createEditor(this, blank, this.providers::add, null));
    }

    private static Screen createEditor(
            Screen parent, CustomProviderConfig provider, Consumer<CustomProviderConfig> onCommit, Runnable onDelete) {
        return switch (provider.type()) {
            case YGGDRASIL -> new SkinRestorerCustomYggdrasilScreen(
                    parent, (CustomYggdrasilProviderConfig) provider, onCommit, onDelete);
            case AUTHLIB_INJECTOR -> new SkinRestorerCustomAuthlibInjectorScreen(
                    parent, (CustomAuthlibInjectorProviderConfig) provider, onCommit, onDelete);
            case USERNAME_URL -> new SkinRestorerCustomUsernameUrlScreen(
                    parent, (CustomUsernameUrlProviderConfig) provider, onCommit, onDelete);
            case UNKNOWN -> throw new IllegalStateException("cannot edit an unknown custom provider");
        };
    }

    @Override
    protected void onSave() {
        SkinRestorer.getConfig().providers().custom(this.providers);
    }
}
