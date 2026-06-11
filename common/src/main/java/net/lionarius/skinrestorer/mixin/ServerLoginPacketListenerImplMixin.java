package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.SkinExecutor;
import net.minecraft.network.chat.Component;
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
    @Shadow
    @Nullable
    private GameProfile gameProfile;

    @Shadow
    protected abstract GameProfile createFakeProfile(GameProfile original);

    @Unique
    private CompletableFuture<Void> skinrestorer$pendingSkin;

    @Inject(method = "handleAcceptedLogin", at = @At(value = "HEAD"), cancellable = true)
    public void waitForSkin(CallbackInfo ci) {
        if (skinrestorer$pendingSkin == null) {
            skinrestorer$pendingSkin = CompletableFuture.<Void>supplyAsync(
                            () -> {
                                var profile = this.gameProfile;
                                assert profile != null;

                                profile = skinrestorer$effectiveProfile(profile);

                                var originalSkin = PlayerUtils.getPlayerSkin(profile);

                                if (SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId())) {
                                    if (originalSkin != null) { // update to the latest official skin
                                        var value =
                                                SkinRestorer.getSkinStorage().getSkin(profile.getId());
                                        SkinRestorer.getSkinStorage()
                                                .setSkin(profile.getId(), value.setOriginalValue(originalSkin));
                                    }

                                    if (SkinRestorer.getConfig().join().refreshSkin()) {
                                        var currentSkin =
                                                SkinRestorer.getSkinStorage().getSkin(profile.getId());

                                        if (!SkinRestorer.getConfig()
                                                .join()
                                                .skipRefreshProviders()
                                                .contains(currentSkin.provider())) {
                                            var context = currentSkin.toProviderContext();
                                            skinrestorer$fetchSkin(profile, context);
                                        }
                                    }

                                    return null;
                                }

                                var autoFetchConfig =
                                        SkinRestorer.getConfig().join().autoFetchConfig();
                                var providerNames = autoFetchConfig.providers();

                                var shouldFetch = (originalSkin == null && autoFetchConfig.enabled())
                                        || (originalSkin != null && autoFetchConfig.overrideExisting());

                                if (!shouldFetch) return null;

                                for (String providerName : providerNames) {
                                    var provider = SkinRestorer.getProvider(providerName)
                                            .orElse(null);

                                    if (provider == null
                                            || provider.getParameterType() != SkinProviderParameterType.USERNAME) {
                                        SkinRestorer.LOGGER.warn(
                                                "Skipping first join skin fetch for '{}': provider '{}' does not accept username parameter",
                                                profile.getName(),
                                                providerName);

                                        continue;
                                    }

                                    var context = new SkinProviderContext(providerName, profile.getName(), null);
                                    var skinFetched = skinrestorer$fetchSkin(profile, context);

                                    if (!skinFetched) {
                                        continue;
                                    }

                                    break;
                                }

                                return null;
                            },
                            SkinExecutor.FETCH_EXECUTOR)
                    .exceptionally(exception -> {
                        var profile = this.gameProfile;
                        SkinRestorer.LOGGER.error(
                                "Unexpected error while preparing {}'s skin during login",
                                profile != null ? profile.getName() : "<unknown>",
                                exception);
                        return null;
                    });
        }

        if (!skinrestorer$pendingSkin.isDone()) ci.cancel();
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(Component reason, CallbackInfo ci) {
        var pendingSkin = this.skinrestorer$pendingSkin;
        var profile = this.gameProfile;
        if (pendingSkin == null || profile == null) return;

        var profileId = skinrestorer$effectiveProfile(profile).getId();

        // the player never reached the PlayerList, so the disconnect cleanup there won't run;
        // evict after the pending fetch completes so its setSkin can't resurrect the entry
        pendingSkin.whenComplete((ignored, ignoredError) -> {
            var storage = SkinRestorer.getSkinStorage();
            if (storage != null) storage.removeSkin(profileId);
        });
    }

    @Unique
    private GameProfile skinrestorer$effectiveProfile(GameProfile profile) {
        return profile.isComplete() ? profile : this.createFakeProfile(profile);
    }

    @Unique
    private static boolean skinrestorer$fetchSkin(GameProfile profile, SkinProviderContext context) {
        SkinRestorer.LOGGER.debug("Fetching {}'s skin", profile.getName());

        var result = SkinRestorer.getProvider(context.name())
                .map(provider -> provider.fetchSkin(context.argument(), context.variant()))
                .orElseGet(() -> Result.error(
                        new IllegalArgumentException("Skin provider is not registered: " + context.name())));

        if (!result.isError()) {
            var value = SkinValue.fromProviderContextWithValue(
                    context, result.getSuccessValue().orElse(null));
            SkinRestorer.getSkinStorage().setSkin(profile.getId(), value);
        } else {
            SkinRestorer.LOGGER.warn(
                    "Failed to fetch skin '{}:{}'", context.name(), context.argument(), result.getErrorValue());
        }

        return !result.isError();
    }
}
