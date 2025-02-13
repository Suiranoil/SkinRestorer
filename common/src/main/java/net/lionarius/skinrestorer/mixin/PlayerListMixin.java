package net.lionarius.skinrestorer.mixin;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    
    @Shadow
    public abstract List<ServerPlayer> getPlayers();
    
    @Shadow @Final
    private MinecraftServer server;
    
    @Inject(method = "remove", at = @At("TAIL"))
    private void remove(ServerPlayer player, CallbackInfo ci) {
        SkinRestorer.Events.onPlayerDisconnect(player);
    }
    
    @Inject(method = "removeAll", at = @At("HEAD"))
    private void removeAll(CallbackInfo ci) {
        for (var player : getPlayers()) {
            SkinRestorer.Events.onPlayerDisconnect(player);
        }
    }
    
    private void scheduleDelayedSkinApplication(ServerPlayer player, int tickDelay) {
        UUID playerUUID = player.getUUID();
        server.execute(() -> {
            // Store the current tick count
            long scheduledTick = server.getTickCount() + tickDelay;
            
            // Create a repeating check that will run until the desired tick
            new Thread(() -> {
                while (server.isRunning()) {
                    try {
                        Thread.sleep(50); // Wait 50ms (1 tick)
                        if (server.getTickCount() >= scheduledTick) {
                            server.execute(() -> {
                                ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(playerUUID);
                                if (onlinePlayer != null && SkinRestorer.getSkinStorage().hasSavedSkin(playerUUID)) {
                                    SkinRestorer.applySkin(server, Collections.singleton(onlinePlayer.getGameProfile()),
                                        SkinRestorer.getSkinStorage().getSkin(playerUUID));
                                }
                            });
                            break;
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        });
    }
    
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (SkinRestorer.getSkinStorage().hasSavedSkin(player.getUUID())) {
            // Try applying the skin multiple times over a period of time to ensure it gets applied
            for (int i = 1; i <= 3; i++) {
                scheduleDelayedSkinApplication(player, i * 20); // 20 ticks = 1 second
            }
        }
    }
}
