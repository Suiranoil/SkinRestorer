package net.lionarius.skinrestorer.config.provider.collection;

import net.lionarius.skinrestorer.skin.SkinVariant;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public interface CollectionSkinSource {
    @Nullable
    URI uri();

    SkinVariant variant();

    @Nullable
    String name();
}
