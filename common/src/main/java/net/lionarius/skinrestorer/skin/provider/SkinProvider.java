package net.lionarius.skinrestorer.skin.provider;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;
import java.util.Set;

public interface SkinProvider {
    EmptySkinProvider EMPTY = new EmptySkinProvider();
    MojangSkinProvider MOJANG = new MojangSkinProvider();
    ElyBySkinProvider ELY_BY = new ElyBySkinProvider();
    MineskinSkinProvider MINESKIN = new MineskinSkinProvider();
    SkinShuffleSkinProvider SKIN_SHUFFLE = new SkinShuffleSkinProvider();
    
    Set<String> BUILTIN_PROVIDER_NAMES = Set.of(
            EmptySkinProvider.PROVIDER_NAME,
            MojangSkinProvider.PROVIDER_NAME,
            ElyBySkinProvider.PROVIDER_NAME,
            MineskinSkinProvider.PROVIDER_NAME,
            SkinShuffleSkinProvider.PROVIDER_NAME
    );
    
    String getArgumentName();
    
    boolean hasVariantSupport();
    
    Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant);
}
