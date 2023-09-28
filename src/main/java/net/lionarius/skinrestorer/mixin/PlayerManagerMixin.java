package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.SkinStorage;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow
    public abstract List<ServerPlayerEntity> getPlayerList();

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final
    static Logger LOGGER;

    private CompletableFuture<Property> skinrestorer_pendingSkin;

    @Inject(method = "remove", at = @At("TAIL"))
    private void remove(ServerPlayerEntity player, CallbackInfo ci) {
        SkinRestorer.getSkinStorage().removeSkin(player.getUuid());
    }

    @Inject(method = "disconnectAllPlayers", at = @At("HEAD"))
    private void disconnectAllPlayers(CallbackInfo ci) {
        for (ServerPlayerEntity player : getPlayerList()) {
            SkinRestorer.getSkinStorage().removeSkin(player.getUuid());
        }
    }

    @Inject(method = "checkCanJoin", at = @At(value = "HEAD"), cancellable = true)
    public void waitForSkin(SocketAddress address, GameProfile profile, CallbackInfoReturnable ci) {
        if (skinrestorer_pendingSkin == null) {
            skinrestorer_pendingSkin = CompletableFuture.supplyAsync(() -> {
                LOGGER.debug("Fetching {}'s skin", profile.getName());
                if (SkinRestorer.getSkinStorage().getSkin(profile.getId()) == SkinStorage.DEFAULT_SKIN)
                    SkinRestorer.getSkinStorage().setSkin(profile.getId(), MojangSkinProvider.getSkin(profile.getName()));

                return SkinRestorer.getSkinStorage().getSkin(profile.getId());
            });
        }

        //if (!skinrestorer_pendingSkin.isDone()) {
        //    ci.cancel();
        //}
    }

    private static void applyRestoredSkin(ServerPlayerEntity playerEntity, Property skin) {
        playerEntity.getGameProfile().getProperties().removeAll("textures");
        playerEntity.getGameProfile().getProperties().put("textures", skin);
    }


    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void onPlayerConnected(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        if (skinrestorer_pendingSkin != null)
            applyRestoredSkin(player, skinrestorer_pendingSkin.getNow(SkinStorage.DEFAULT_SKIN));

        if (player.getClass() != ServerPlayerEntity.class) // if the player isn't a server player entity, it must be someone's fake player
            SkinRestorer.setSkinAsync(server, Collections.singleton(player.getGameProfile()), () -> SkinRestorer.getSkinStorage().getSkin(player.getUuid()));
    }
}
