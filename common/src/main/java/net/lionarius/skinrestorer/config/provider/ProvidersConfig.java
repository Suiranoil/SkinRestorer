package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public final class ProvidersConfig implements GsonPostProcessable {
    public static final ProvidersConfig DEFAULT = new ProvidersConfig(
            new MojangProviderConfig(),
            new ElyByProviderConfig(),
            new MineskinProviderConfig()
    );

    private MojangProviderConfig mojang;
    private ElyByProviderConfig ely_by;
    private MineskinProviderConfig mineskin;

    public ProvidersConfig(MojangProviderConfig mojang, ElyByProviderConfig ely_by, MineskinProviderConfig mineskin) {
        this.mojang = mojang;
        this.ely_by = ely_by;
        this.mineskin = mineskin;
    }

    public MojangProviderConfig mojang() {
        return this.mojang;
    }

    public ElyByProviderConfig ely_by() {
        return this.ely_by;
    }

    public MineskinProviderConfig mineskin() {
        return this.mineskin;
    }

    @Override
    public void gsonPostProcess() {
        if (this.mojang == null) {
            SkinRestorer.LOGGER.warn("Mojang provider config is null, using default");
            this.mojang = ProvidersConfig.DEFAULT.mojang();
        }

        if (this.ely_by == null) {
            SkinRestorer.LOGGER.warn("Ely.By provider config is null, using default");
            this.ely_by = ProvidersConfig.DEFAULT.ely_by();
        }

        if (this.mineskin == null) {
            SkinRestorer.LOGGER.warn("Mineskin provider config is null, using default");
            this.mineskin = ProvidersConfig.DEFAULT.mineskin();
        }
    }
}
