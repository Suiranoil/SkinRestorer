package net.lionarius.skinrestorer.client;

import net.minecraft.util.ResourceLocation;

/** Client-only access to the existing player skin and SSB model state. */
public interface ClientPlayerAccess {

    ResourceLocation skinrestorer$getSkin();

    void skinrestorer$setSkin(ResourceLocation location);

    boolean skinrestorer$isSlim();

    void skinrestorer$setSlim(boolean slim);
}
