package net.lionarius.skinrestorer.mixin;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    
    @Inject(method = "remove", at = @At("TAIL"))
    private void remove(ServerPlayer player, CallbackInfo ci) {
        SkinRestorer.Events.onPlayerDisconnect(player);
    }
    
    @Inject(method = "removeAll", at = @At("HEAD"))
    private void removeAll(CallbackInfo ci) {
        for (var player : ((PlayerList) (Object) this).getPlayers()) {
            SkinRestorer.Events.onPlayerDisconnect(player);
        }
    }
    
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        var server = ((PlayerList) (Object) this).getServer();
        var delay = SkinRestorer.getConfig().skinApplyDelayOnJoin();
        
        if (delay <= 0) {
            skinrestorer$tryApplySkin(server, player);
        } else {
            var uuid = player.getUUID();
            SkinRestorer.getTickedScheduler().schedule(() -> {
                var actualPlayer = server.getPlayerList().getPlayer(uuid);
                if (actualPlayer != null)
                    skinrestorer$tryApplySkin(server, actualPlayer);
            }, delay, uuid);
        }
    }
    
    @Unique
    private static void skinrestorer$tryApplySkin(MinecraftServer server, ServerPlayer player) {
        if (SkinRestorer.getSkinStorage().hasSavedSkin(player.getUUID()))
            SkinRestorer.applySkin(server, Collections.singleton(player.getGameProfile()), SkinRestorer.getSkinStorage().getSkin(player.getUUID()));
    }
}
