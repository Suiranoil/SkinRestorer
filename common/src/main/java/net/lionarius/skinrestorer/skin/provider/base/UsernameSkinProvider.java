package net.lionarius.skinrestorer.skin.provider.base;

import net.lionarius.skinrestorer.skin.SkinVariant;

import java.util.Locale;

public abstract class UsernameSkinProvider extends UsernameArgumentSkinProvider<String> {
    @Override
    protected String getCacheKey(String argument, SkinVariant variant) {
        return argument.toLowerCase(Locale.ROOT);
    }
}
