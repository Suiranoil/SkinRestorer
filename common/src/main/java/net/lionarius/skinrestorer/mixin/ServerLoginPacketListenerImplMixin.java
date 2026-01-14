package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.FirstJoinSkinProvider;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {
    
    @Shadow @Nullable
    private GameProfile gameProfile;
    
    @Shadow
    protected abstract GameProfile createFakeProfile(GameProfile original);
    
    @Unique
    private CompletableFuture<Void> skinrestorer$pendingSkin;
    
    @Inject(method = "handleAcceptedLogin", at = @At(value = "HEAD"), cancellable = true)
    public void waitForSkin(CallbackInfo ci) {
        if (skinrestorer$pendingSkin == null) {
            skinrestorer$pendingSkin = CompletableFuture.supplyAsync(() -> {
                var profile = gameProfile;
                assert profile != null;
                
                if (!profile.isComplete())
                    profile = createFakeProfile(profile);
                
                var originalSkin = PlayerUtils.getPlayerSkin(profile);
                
                if (SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId())) {
                    if (originalSkin != null) { // update to the latest official skin
                        var value = SkinRestorer.getSkinStorage().getSkin(profile.getId());
                        SkinRestorer.getSkinStorage().setSkin(profile.getId(), value.setOriginalValue(originalSkin));
                    }
                    
                    if (SkinRestorer.getConfig().refreshSkinOnJoin()) {
                        var currentSkin = SkinRestorer.getSkinStorage().getSkin(profile.getId());
                        var context = currentSkin.toProviderContext();
                        
                        skinrestorer$fetchSkin(profile, context);
                    }
                    
                    return null;
                }
                
                var config = SkinRestorer.getConfig();
                var provider = config.firstJoinSkinProvider();
                
                var shouldFetch = (originalSkin == null && config.fetchSkinOnFirstJoin()) ||
                                      (originalSkin != null && config.forceFirstJoinSkinFetch() && provider != FirstJoinSkinProvider.MOJANG);
                
                if (shouldFetch) {
                    var context = new SkinProviderContext(
                            provider.getName(),
                            profile.getName(),
                            null
                    );
                    skinrestorer$fetchSkin(profile, context);
                }
                
                return null;
            });
        }
        
        if (!skinrestorer$pendingSkin.isDone())
            ci.cancel();
    }
    
    @Unique
    private static void skinrestorer$fetchSkin(GameProfile profile, SkinProviderContext context) {
        SkinRestorer.LOGGER.debug("Fetching {}'s skin", profile.getName());
        
        var result = SkinRestorer.getProvider(context.name()).map(
                provider -> provider.fetchSkin(context.argument(), context.variant())
        ).orElseGet(() -> Result.error(new IllegalArgumentException("Skin provider is not registered: " + context.name())));
        
        if (!result.isError()) {
            var value = SkinValue.fromProviderContextWithValue(context, result.getSuccessValue().orElse(null));
            SkinRestorer.getSkinStorage().setSkin(profile.getId(), value);
        } else {
            SkinRestorer.LOGGER.warn("Failed to fetch skin '{}:{}'", context.name(), context.argument(), result.getErrorValue());
        }
    }
}
