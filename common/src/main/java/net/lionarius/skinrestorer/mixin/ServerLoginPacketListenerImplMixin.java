package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
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
    
    @Unique
    private CompletableFuture<Void> skinrestorer$pendingSkin;
    
    @Inject(method = "handleAcceptedLogin", at = @At(value = "INVOKE",
                                                                     target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"),
            cancellable = true)
    public void waitForSkin(CallbackInfo ci) {
        if (skinrestorer$pendingSkin == null) {
            skinrestorer$pendingSkin = CompletableFuture.supplyAsync(() -> {
                final var profile = gameProfile;
                
                assert profile != null;
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
                
                if (originalSkin == null && SkinRestorer.getConfig().fetchSkinOnFirstJoin()) {
                    var context = new SkinProviderContext(
                            SkinRestorer.getConfig().firstJoinSkinProvider().getName(),
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
        SkinRestorer.LOGGER.debug("fetching {}'s skin", profile.getName());
        
        var result = SkinRestorer.getProvider(context.name()).map(
                provider -> provider.fetchSkin(context.argument(), context.variant())
        ).orElseGet(() -> Result.error(new IllegalArgumentException("skin provider is not registered: " + context.name())));
        
        if (!result.isError()) {
            var value = SkinValue.fromProviderContextWithValue(context, result.getSuccessValue().orElse(null));
            SkinRestorer.getSkinStorage().setSkin(profile.getId(), value);
        } else {
            SkinRestorer.LOGGER.warn("failed to fetch skin", result.getErrorValue());
        }
    }
}
