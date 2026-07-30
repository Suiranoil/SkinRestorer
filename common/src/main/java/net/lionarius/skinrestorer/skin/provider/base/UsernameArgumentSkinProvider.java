package net.lionarius.skinrestorer.skin.provider.base;

import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.minecraft.util.StringUtil;

public abstract class UsernameArgumentSkinProvider<K> extends AbstractSkinProvider<K> {
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
        if (!StringUtil.isValidPlayerName(argument)) throw new IllegalArgumentException("invalid username");
    }
}
