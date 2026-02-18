package net.lionarius.skinrestorer.skin.provider;

import net.lionarius.skinrestorer.skin.SkinVariant;
import net.minecraft.util.StringUtil;

import java.util.Locale;

public abstract class UsernameSkinProvider extends AbstractSkinProvider<String> {

    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.USERNAME;
    }

    @Override
    public String getArgumentName() {
        return "username";
    }

    @Override
    public boolean hasVariantSupport() {
        return false;
    }

    @Override
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);
        if (!StringUtil.isValidPlayerName(argument))
            throw new IllegalArgumentException("invalid username");
    }

    @Override
    protected String getCacheKey(String argument, SkinVariant variant) {
        return argument.toLowerCase(Locale.ROOT);
    }
}
