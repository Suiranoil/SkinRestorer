package net.lionarius.skinrestorer.skin.provider;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;

public final class SkinShuffleSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "skinshuffle";
    
    @Override
    public String getProviderName() {
        return SkinShuffleSkinProvider.PROVIDER_NAME;
    }
    
    @Override
    public String getArgumentName() {
        return "unsupported";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant) {
        return Result.error(new UnsupportedOperationException("SkinShuffle Provider does not support fetching skins"));
    }
}
