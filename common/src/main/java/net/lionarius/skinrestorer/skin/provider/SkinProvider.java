package net.lionarius.skinrestorer.skin.provider;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.builtin.*;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;
import java.util.Set;

public interface SkinProvider {
    EmptySkinProvider EMPTY = new EmptySkinProvider();
    MojangSkinProvider MOJANG = new MojangSkinProvider();
    ElyBySkinProvider ELY_BY = new ElyBySkinProvider();
    MineskinSkinProvider MINESKIN = new MineskinSkinProvider();
    DraslSkinProvider DRASL = new DraslSkinProvider(SkinProvider.MINESKIN);
    CollectionSkinProvider COLLECTION = new CollectionSkinProvider(SkinProvider.MINESKIN);
    SkinShuffleSkinProvider SKIN_SHUFFLE = new SkinShuffleSkinProvider();
    
    Set<String> BUILTIN_PROVIDER_NAMES = ImmutableSet.of(
            SkinProvider.EMPTY.getProviderName(),
            SkinProvider.MOJANG.getProviderName(),
            SkinProvider.ELY_BY.getProviderName(),
            SkinProvider.MINESKIN.getProviderName(),
            SkinProvider.DRASL.getProviderName(),
            SkinProvider.COLLECTION.getProviderName(),
            SkinProvider.SKIN_SHUFFLE.getProviderName()
    );
    
    String getProviderName();
    
    SkinProviderParameterType getParameterType();
    
    String getArgumentName();
    
    boolean hasVariantSupport();
    
    Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant);
    
    default void reload() {}
}
