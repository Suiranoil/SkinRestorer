package net.lionarius.skinrestorer.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.lionarius.skinrestorer.command.SkinCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {

    @Final
    @Shadow
    private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(CommandManager.RegistrationEnvironment environment, CallbackInfo ci) {
        SkinCommand.register(dispatcher);
    }
}
