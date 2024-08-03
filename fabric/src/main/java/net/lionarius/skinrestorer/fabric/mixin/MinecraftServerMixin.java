package net.lionarius.skinrestorer.fabric.mixin;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    
    @Inject(method = "runServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J", ordinal = 0))
    private void onServerStarted(CallbackInfo ci) {
        SkinRestorer.onServerStarted((MinecraftServer) (Object) this);
    }
}
