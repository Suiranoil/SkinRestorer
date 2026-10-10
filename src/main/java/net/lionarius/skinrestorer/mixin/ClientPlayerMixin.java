package net.lionarius.skinrestorer.mixin;

import net.lionarius.skinrestorer.client.ClientBridge;
import net.lionarius.skinrestorer.client.ClientPlayerAccess;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;

@Mixin(value = AbstractClientPlayer.class, priority = 900)
public abstract class ClientPlayerMixin implements ClientPlayerAccess {

    @Shadow
    private ResourceLocation locationSkin;

    @Unique
    private boolean skinrestorer$slim;

    @Inject(method = "func_152121_a", at = @At("HEAD"), cancellable = true)
    private void skinrestorer$acceptSkin(MinecraftProfileTexture.Type type, ResourceLocation location,
        CallbackInfo callback) {
        if (type == MinecraftProfileTexture.Type.SKIN
            && !ClientBridge.acceptCallback((AbstractClientPlayer) (Object) this, location)) {
            callback.cancel();
        }
    }

    /*
     * SSB adds IArmsState to EntityPlayer. These intentional subclass overrides
     * keep its renderer and interface dispatch, without linking SSB at compile
     * time. Both its download-buffer callback and SkinManager TAIL write here.
     * No @Unique: the public SSB names must remain available for virtual dispatch.
     */
    public boolean ssb$isSlim() {
        return skinrestorer$slim;
    }

    public void ssb$setSlim(boolean slim) {
        if (ClientBridge.acceptArmChange((AbstractClientPlayer) (Object) this, slim)) {
            skinrestorer$slim = slim;
        }
    }

    @Override
    public ResourceLocation skinrestorer$getSkin() {
        return locationSkin;
    }

    @Override
    public void skinrestorer$setSkin(ResourceLocation location) {
        locationSkin = location;
    }

    @Override
    public boolean skinrestorer$isSlim() {
        return ssb$isSlim();
    }

    @Override
    public void skinrestorer$setSlim(boolean slim) {
        ssb$setSlim(slim);
    }
}
