package net.lionarius.skinrestorer.config.provider.custom;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.custom.CustomAuthlibInjectorSkinProvider;
import net.lionarius.skinrestorer.util.Result;

public final class CustomAuthlibInjectorProviderConfig extends CustomProviderConfig {
    public static final CustomProviderType TYPE = CustomProviderType.AUTHLIB_INJECTOR;

    private String baseUrl = "";

    public CustomAuthlibInjectorProviderConfig() {
        super(CustomAuthlibInjectorProviderConfig.TYPE);
    }

    public String baseUrl() {
        return this.baseUrl;
    }

    public void baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Result<SkinProvider, String> createSkinProvider(SkinSigner skinSigner) {
        if (this.baseUrl.isEmpty()) return Result.error("baseUrl is not configured");

        return Result.success(new CustomAuthlibInjectorSkinProvider(this.name, skinSigner));
    }

    @Override
    public void gsonPostProcess() {
        super.gsonPostProcess();

        this.type = CustomAuthlibInjectorProviderConfig.TYPE;

        if (this.baseUrl == null) {
            SkinRestorer.LOGGER.warn("Custom provider baseUrl is null, defaulting to an empty string");
            this.baseUrl = "";
        }

        this.baseUrl = this.baseUrl.trim();
    }
}
