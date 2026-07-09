package net.lionarius.skinrestorer.config.provider.custom;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.custom.CustomUsernameUrlSkinProvider;
import net.lionarius.skinrestorer.util.Result;

public final class CustomUsernameUrlProviderConfig extends CustomProviderConfig {
    public static final CustomProviderType TYPE = CustomProviderType.USERNAME_URL;

    private String urlTemplate = "";

    public CustomUsernameUrlProviderConfig() {
        super(CustomUsernameUrlProviderConfig.TYPE);
    }

    public String urlTemplate() {
        return this.urlTemplate;
    }

    public void urlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    @Override
    public Result<SkinProvider, String> createSkinProvider(SkinSigner skinSigner) {
        if (this.urlTemplate.isEmpty()) return Result.error("urlTemplate is not configured");

        if (!this.urlTemplate.contains(CustomUsernameUrlSkinProvider.USERNAME_PLACEHOLDER))
            return Result.error(
                    "urlTemplate must contain '" + CustomUsernameUrlSkinProvider.USERNAME_PLACEHOLDER + "'");

        if (!CustomUsernameUrlSkinProvider.isValidUrlTemplate(this.urlTemplate))
            return Result.error("urlTemplate is invalid or not an absolute URI");

        return Result.success(new CustomUsernameUrlSkinProvider(this.name, skinSigner));
    }

    @Override
    public void gsonPostProcess() {
        super.gsonPostProcess();

        this.type = CustomUsernameUrlProviderConfig.TYPE;

        if (this.urlTemplate == null) {
            SkinRestorer.LOGGER.warn("Custom provider urlTemplate is null, defaulting to an empty string");
            this.urlTemplate = "";
        }

        this.urlTemplate = this.urlTemplate.trim();
    }
}
