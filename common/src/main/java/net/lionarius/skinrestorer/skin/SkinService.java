package net.lionarius.skinrestorer.skin;

import com.google.common.base.Throwables;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.SkinExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SkinService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkinService.class);

    private SkinService() {}

    public static Collection<SkinTarget> applySkin(
            MinecraftServer server, Iterable<SkinTarget> targets, SkinValue value, boolean save) {
        var acceptedTargets = new HashSet<SkinTarget>();

        for (var target : targets) {
            var player = server.getPlayerList().getPlayer(target.id());

            if (player == null) {
                if (save && SkinService.saveOfflineSkin(target.id(), value)) acceptedTargets.add(target);

                continue;
            }

            if (SkinService.applySkin(player, value, save)) acceptedTargets.add(target);
        }

        return acceptedTargets;
    }

    public static boolean applySkin(ServerPlayer player, SkinValue value, boolean save) {
        var profile = player.getGameProfile();
        var skin = PlayerUtils.getPlayerSkin(profile);
        var playerValue = value;

        if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId()))
            playerValue = playerValue.setOriginalValue(skin);

        if (PlayerUtils.areSkinPropertiesEquals(playerValue.value(), skin)) return false;

        if (save) SkinRestorer.getSkinStorage().setSkin(profile.getId(), playerValue);

        PlayerUtils.applyRestoredSkin(profile, playerValue.value());

        if (player.connection == null) return false;

        PlayerUtils.refreshPlayer(player);
        SkinRestorer.getTickedScheduler().cancel(player.getUUID());

        return true;
    }

    private static boolean saveOfflineSkin(UUID id, SkinValue value) {
        return SkinRestorer.getSkinStorage().setSkin(id, value, false);
    }

    public static Collection<SkinTarget> applySkin(
            MinecraftServer server, Iterable<SkinTarget> targets, SkinValue value) {
        return SkinService.applySkin(server, targets, value, true);
    }

    public static CompletableFuture<Result<Collection<SkinTarget>, String>> setSkinAsync(
            MinecraftServer server, Collection<SkinTarget> targets, SkinProviderContext context, boolean save) {
        return CompletableFuture.supplyAsync(
                        () -> SkinRestorer.getProvider(context.name())
                                .map(provider -> provider.fetchSkin(context.argument(), context.variant())),
                        SkinExecutor.FETCH_EXECUTOR)
                .thenApplyAsync(
                        result -> {
                            if (result.isEmpty())
                                return Result.<Collection<SkinTarget>, String>error(
                                        "provider '" + context.name() + "' is not registered");

                            var skinResult = result.get();
                            if (skinResult.isError())
                                throw new TransparentException(Throwables.getRootCause(skinResult.getErrorValue()));

                            var skinValue = SkinValue.fromProviderContextWithValue(
                                    context, skinResult.getSuccessValue().orElse(null));

                            var acceptedTargets = SkinService.applySkin(server, targets, skinValue, save);

                            return Result.<Collection<SkinTarget>, String>success(acceptedTargets);
                        },
                        server)
                .exceptionally(e -> {
                    SkinService.LOGGER.error("Failed to set skin '{}:{}'", context.name(), context.argument(), e);
                    return Result.error(e.getMessage());
                });
    }
}
