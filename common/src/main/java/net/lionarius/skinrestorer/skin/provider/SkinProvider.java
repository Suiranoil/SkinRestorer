package net.lionarius.skinrestorer.skin.provider;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.Result;

import java.util.Optional;

public interface SkinProvider {
    EmptySkinProvider EMPTY = new EmptySkinProvider();
    MojangSkinProvider MOJANG = new MojangSkinProvider();
    ElyBySkinProvider ELY_BY = new ElyBySkinProvider();
    MineskinSkinProvider MINESKIN = new MineskinSkinProvider();
    
    String getArgumentName();
    
    boolean hasVariantSupport();
    
    Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant);
}
