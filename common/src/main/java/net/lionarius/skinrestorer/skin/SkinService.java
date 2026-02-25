package net.lionarius.skinrestorer.skin;

import com.google.common.base.Throwables;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.mixin.PlayerAccessor;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public final class SkinService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkinService.class);
    
    private SkinService() {}
    
    public static Collection<ServerPlayer> applySkin(MinecraftServer server, Iterable<ServerPlayer> targets, SkinValue value, boolean save) {
        var acceptedPlayers = new HashSet<ServerPlayer>();
        
        for (var player : targets) {
            var profile = player.getGameProfile();
            var skin = PlayerUtils.getPlayerSkin(profile);
            var playerValue = value;
            
            if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.id()))
                playerValue = playerValue.setOriginalValue(skin);
            
            if (PlayerUtils.areSkinPropertiesEquals(playerValue.value(), skin))
                continue;
            
            if (save)
                SkinRestorer.getSkinStorage().setSkin(profile.id(), playerValue);
            
            var newProfile = PlayerUtils.applyRestoredSkin(profile, playerValue.value());
            ((PlayerAccessor) player).setGameProfile(newProfile);
            
            if (player.connection == null)
                continue;
            
            PlayerUtils.refreshPlayer(player);
            acceptedPlayers.add(player);
            
            SkinRestorer.getTickedScheduler().cancel(player.getUUID());
        }
        
        return acceptedPlayers;
    }
    
    public static Collection<ServerPlayer> applySkin(MinecraftServer server, Iterable<ServerPlayer> targets, SkinValue value) {
        return SkinService.applySkin(server, targets, value, true);
    }
    
    public static CompletableFuture<Result<Collection<ServerPlayer>, String>> setSkinAsync(
            MinecraftServer server,
            Collection<ServerPlayer> targets,
            SkinProviderContext context,
            boolean save
    ) {
        return CompletableFuture.supplyAsync(
                        () -> SkinRestorer.getProvider(context.name()).map(provider -> provider.fetchSkin(context.argument(), context.variant()))
                )
                .thenApplyAsync(result -> {
                    if (result.isEmpty())
                        return Result.<Collection<ServerPlayer>, String>error("provider '" + context.name() + "' is not registered");
                    
                    var skinResult = result.get();
                    if (skinResult.isError())
                        throw new TransparentException(Throwables.getRootCause(skinResult.getErrorValue()));
                    
                    var skinValue = SkinValue.fromProviderContextWithValue(context, skinResult.getSuccessValue().orElse(null));
                    
                    var acceptedPlayers = SkinService.applySkin(server, targets, skinValue, save);
                    
                    return Result.<Collection<ServerPlayer>, String>success(acceptedPlayers);
                }, server)
                .exceptionally(e -> {
                    SkinService.LOGGER.error("Failed to set skin '{}:{}'", context.name(), context.argument(), e);
                    return Result.error(e.getMessage());
                });
    }
}
