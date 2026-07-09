package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

import java.util.ArrayList;
import java.util.List;

public final class JoinConfig implements GsonPostProcessable {
    private boolean refreshSkin = true;

    private List<String> skipRefreshProviders = new ArrayList<>();

    private int applyDelay = 0;

    private AutoFetchConfig autoFetch = new AutoFetchConfig();

    public boolean refreshSkin() {
        return this.refreshSkin;
    }

    public void refreshSkin(boolean refreshSkin) {
        this.refreshSkin = refreshSkin;
    }

    public List<String> skipRefreshProviders() {
        return this.skipRefreshProviders;
    }

    public void skipRefreshProviders(List<String> skipRefreshProviders) {
        this.skipRefreshProviders = skipRefreshProviders;
    }

    public int applyDelay() {
        return this.applyDelay;
    }

    public void applyDelay(int applyDelay) {
        this.applyDelay = applyDelay;
    }

    public AutoFetchConfig autoFetchConfig() {
        return this.autoFetch;
    }

    @Override
    public void gsonPostProcess() {
        if (this.applyDelay < 0) {
            SkinRestorer.LOGGER.warn("ApplyDelay config is less than 0, defaulting to 0");
            this.applyDelay = 0;
        }

        if (this.skipRefreshProviders == null) {
            SkinRestorer.LOGGER.warn("SkipRefreshProviders config is null, using default");
            this.skipRefreshProviders = new ArrayList<>();
        }

        if (this.autoFetch == null) {
            SkinRestorer.LOGGER.warn("AutoFetch config is null, using default");
            this.autoFetch = new AutoFetchConfig();
        }
    }
}
