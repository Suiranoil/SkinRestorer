package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.server.Services;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin {
    
    @Inject(method = "loadProfile", at = @At("HEAD"),
            cancellable = true)
    private static void fetchProfileByName(String name, Services services, BooleanSupplier hasCache, CallbackInfoReturnable<CompletableFuture<Optional<GameProfile>>> cir) {
        if (name == null)
            return;
        
        var profileOpt = services.profileCache().get(name);
        
        skinrestorer$replaceSkin(profileOpt, cir);
    }
    
    @Unique
    private static void skinrestorer$replaceSkin(Optional<GameProfile> profileOpt, CallbackInfoReturnable<CompletableFuture<Optional<GameProfile>>> cir) {
        if (profileOpt.isEmpty())
            return;
        
        var profile = PlayerUtils.cloneGameProfile(profileOpt.get());
        
        if (SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId())) {
            cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
                var skin = SkinRestorer.getSkinStorage().getSkin(profile.getId(), false);
                PlayerUtils.applyRestoredSkin(profile, skin.value());
                
                return Optional.of(profile);
            }));
        }
    }
}
