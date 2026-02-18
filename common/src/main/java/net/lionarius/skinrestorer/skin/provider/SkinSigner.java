package net.lionarius.skinrestorer.skin.provider;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;

import java.net.URI;
import java.util.Optional;

public interface SkinSigner {
    Optional<Property> signSkin(URI uri, SkinVariant variant) throws Exception;
    
    Optional<Property> signSkin(Property property) throws Exception;
}
