package net.lionarius.skinrestorer.skin.provider;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.mineskin.MineskinService;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.builtin.*;
import net.lionarius.skinrestorer.util.Result;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public interface SkinProvider {
    EmptySkinProvider EMPTY = new EmptySkinProvider();
    MojangSkinProvider MOJANG = new MojangSkinProvider();
    ElyBySkinProvider ELY_BY = new ElyBySkinProvider();
    MineskinSkinProvider MINESKIN = new MineskinSkinProvider(MineskinService.INSTANCE);
    CollectionSkinProvider COLLECTION = new CollectionSkinProvider(MineskinService.INSTANCE);
    SkinShuffleSkinProvider SKIN_SHUFFLE = new SkinShuffleSkinProvider();

    Set<String> BUILTIN_PROVIDER_NAMES = ImmutableSet.of(
            SkinProvider.EMPTY.getProviderName(),
            SkinProvider.MOJANG.getProviderName(),
            SkinProvider.ELY_BY.getProviderName(),
            SkinProvider.MINESKIN.getProviderName(),
            SkinProvider.COLLECTION.getProviderName(),
            SkinProvider.SKIN_SHUFFLE.getProviderName());

    String getProviderName();

    SkinProviderParameterType getParameterType();

    String getArgumentName();

    boolean hasVariantSupport();

    // called on tab-complete, must be fast and non-blocking
    default Collection<String> getArgumentSuggestions() {
        return Collections.emptyList();
    }

    Result<Optional<Property>, Exception> fetchSkin(String argument, SkinVariant variant);

    default void reload() {}
}
