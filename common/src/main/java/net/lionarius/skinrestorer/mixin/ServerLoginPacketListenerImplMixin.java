package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
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
    private GameProfile authenticatedProfile;
    @Shadow @Final
    MinecraftServer server;
    
    @Unique
    private CompletableFuture<Void> skinrestorer_pendingSkin;
    
    @Inject(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE",
                                                                     target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"),
            cancellable = true)
    public void waitForSkin(CallbackInfo ci) {
        if (skinrestorer_pendingSkin == null) {
            skinrestorer_pendingSkin = CompletableFuture.supplyAsync(() -> {
                assert authenticatedProfile != null;
                var originalSkin = PlayerUtils.getPlayerSkin(authenticatedProfile);
                
                if (SkinRestorer.getSkinStorage().hasSavedSkin(authenticatedProfile.getId())) {
                    if (originalSkin != null) { // update to the latest official skin
                        var value = SkinRestorer.getSkinStorage().getSkin(authenticatedProfile.getId());
                        SkinRestorer.getSkinStorage().setSkin(authenticatedProfile.getId(), value.setOriginalValue(originalSkin));
                    }
                    
                    return null;
                }
                
                if (originalSkin == null && SkinRestorer.getConfig().fetchSkinOnFirstJoin()) {
                    SkinRestorer.LOGGER.debug("Fetching {}'s skin", authenticatedProfile.getName());
                    
                    var context = new SkinProviderContext(MojangSkinProvider.PROVIDER_NAME, authenticatedProfile.getName(), null);
                    var result = SkinRestorer.getProvider(context.name()).map(
                            provider -> provider.getSkin(context.argument(), context.variant())
                    ).orElse(Result.ofNullable(null));
                    
                    if (!result.isError()) {
                        var value = SkinValue.fromProviderContextWithValue(context, result.getSuccessValue().orElse(null));
                        SkinRestorer.getSkinStorage().setSkin(authenticatedProfile.getId(), value);
                    } else {
                        SkinRestorer.LOGGER.warn("failed to fetch skin on first join", result.getErrorValue());
                    }
                }
                
                return null;
            });
        }
        
        if (!skinrestorer_pendingSkin.isDone())
            ci.cancel();
    }
}
