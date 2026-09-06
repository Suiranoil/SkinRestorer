package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin {

    @Shadow
    private static GameProfileCache profileCache;

    @Inject(method = "updateGameprofile", at = @At("HEAD"), cancellable = true)
    private static void fetchProfileByName(
            GameProfile profile, Consumer<GameProfile> profileConsumer, CallbackInfo ci) {
        if (profileCache == null) return;

        if (profile == null || profile.isComplete() || profile.getName() == null) return;

        var profileOpt = Optional.<GameProfile>empty();
        var gameProfileInfo = ((GameProfileCacheAccessor) profileCache)
                .getProfilesByName()
                .get(profile.getName().toLowerCase(Locale.ROOT));

        if (gameProfileInfo != null) profileOpt = Optional.of(gameProfileInfo.getProfile());

        skinrestorer$replaceSkin(profileOpt, profileConsumer, ci);
    }

    @Unique
    private static void skinrestorer$replaceSkin(
            Optional<GameProfile> profileOpt, Consumer<GameProfile> profileConsumer, CallbackInfo ci) {
        if (SkinRestorer.getMinecraftServer() == null) return;

        if (profileOpt.isEmpty()) return;

        var profile = PlayerUtils.cloneGameProfile(profileOpt.get());

        if (SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId())) {
            var skin = SkinRestorer.getSkinStorage().getSkin(profile.getId(), false);
            PlayerUtils.applyRestoredSkin(profile, skin.value());

            profileConsumer.accept(profile);
            ci.cancel();
        }
    }
}
