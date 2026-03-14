package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public final class JoinConfig implements GsonPostProcessable {
    
    private boolean refreshSkin = true;

    private int applyDelay = 0;

    private AutoFetchConfig autoFetch = new AutoFetchConfig();

    public boolean refreshSkin() {
        return this.refreshSkin;
    }

    public int applyDelay() {
        return this.applyDelay;
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

        if (this.autoFetch == null) {
            SkinRestorer.LOGGER.warn("AutoFetch config is null, using default");
            this.autoFetch = new AutoFetchConfig();
        }
    }
}
