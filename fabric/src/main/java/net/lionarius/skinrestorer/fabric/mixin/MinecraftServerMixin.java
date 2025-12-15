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
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getNanos()J", ordinal = 0))
    private void onServerStarted(CallbackInfo ci) {
        SkinRestorer.Events.onServerStarted((MinecraftServer) (Object) this);
    }
    
    @Inject(method = "runServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;onServerExit()V"))
    private void onServerStopped(CallbackInfo ci) {
        SkinRestorer.Events.onServerStopped((MinecraftServer) (Object) this);
    }
}
