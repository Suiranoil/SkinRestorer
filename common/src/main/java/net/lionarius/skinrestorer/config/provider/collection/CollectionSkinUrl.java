package net.lionarius.skinrestorer.config.provider.collection;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public final class CollectionSkinUrl implements CollectionSkinSource, GsonPostProcessable {
    private String url = "";
    private SkinVariant variant = SkinVariant.CLASSIC;

    @Override
    public @Nullable URI uri() {
        try {
            if (this.url.isEmpty()) return null;

            return new URI(this.url);
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Invalid URI: {}", this.url, e);
            return null;
        }
    }

    @Override
    public SkinVariant variant() {
        return this.variant;
    }

    public String url() {
        return this.url;
    }

    public void url(String url) {
        this.url = url;
    }

    public void variant(SkinVariant variant) {
        this.variant = variant;
    }

    @Override
    public void gsonPostProcess() {
        if (this.url == null) {
            this.url = "";
        }
        if (this.variant == null) {
            this.variant = SkinVariant.CLASSIC;
        }
    }
}
