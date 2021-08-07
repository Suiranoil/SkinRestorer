package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    private static void applySkin(ServerPlayerEntity playerEntity, Property skin) {
        playerEntity.getGameProfile().getProperties().removeAll("textures");
        playerEntity.getGameProfile().getProperties().put("textures", skin);
    }

    @Shadow
    public abstract List<ServerPlayerEntity> getPlayerList();

    @Inject(method = "onPlayerConnect", at = @At(value = "HEAD"))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        Property skin = SkinRestorer.getSkinStorage().getSkin(player.getUuid());
        if (Objects.equals(skin.getValue(), "")) {
            skin = MojangSkinProvider.getSkin(player.getGameProfile().getName());
        }
        applySkin(player, skin);
    }

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
}
