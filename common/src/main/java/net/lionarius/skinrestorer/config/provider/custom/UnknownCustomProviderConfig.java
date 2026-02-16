package net.lionarius.skinrestorer.config.provider.custom;

import net.lionarius.skinrestorer.skin.provider.MineskinSkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.util.Result;

public final class UnknownCustomProviderConfig extends CustomProviderConfig {
    public UnknownCustomProviderConfig() {
        super(CustomProviderType.UNKNOWN);
    }

    @Override
    public Result<SkinProvider, String> createSkinProvider(MineskinSkinProvider mineskinProvider) {
        return Result.error("type '" + this.type + "' is not supported");
    }
}
