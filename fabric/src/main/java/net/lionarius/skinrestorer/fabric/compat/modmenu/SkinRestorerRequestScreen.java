package net.lionarius.skinrestorer.fabric.compat.modmenu;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SkinRestorerRequestScreen extends AbstractConfigScreen {
    private String proxy;
    private long timeout;
    private String userAgent;

    public SkinRestorerRequestScreen(Screen parent) {
        super(Component.translatable("skinrestorer.config.request.title"), parent);

        var request = SkinRestorer.getConfig().request();
        this.proxy = request.rawProxy();
        this.timeout = request.timeout();
        this.userAgent = request.userAgent();
    }

    @Override
    protected int buildRows(int x) {
        var y = 0;

        y = this.addTextRow(x, y, "skinrestorer.config.request.proxy", this.proxy, value -> this.proxy = value);
        y = this.addNumberRow(
                x,
                y,
                "skinrestorer.config.request.timeout",
                Long.toString(this.timeout),
                value -> this.timeout = SkinRestorerRequestScreen.parseLongOrDefault(value, this.timeout));
        y = this.addTextRow(
                x, y, "skinrestorer.config.request.user_agent", this.userAgent, value -> this.userAgent = value);

        return y;
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void onSave() {
        var request = SkinRestorer.getConfig().request();
        request.rawProxy(this.proxy);
        request.timeout(this.timeout);
        request.userAgent(this.userAgent);
    }
}
