package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;

public final class EmptySkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "empty";
    
    @Override
    public String getProviderName() {
        return EmptySkinProvider.PROVIDER_NAME;
    }
    
    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.CUSTOM;
    }
    
    @Override
    public String getArgumentName() {
        return "placeholder";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant) {
        return this.fetchSkin();
    }
    
    public Result<Optional<Property>, Exception> fetchSkin() {
        return Result.ofNullable(null);
    }
}
