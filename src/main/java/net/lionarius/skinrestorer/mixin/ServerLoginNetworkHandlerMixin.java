package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.SkinStorage;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
	@Shadow @Nullable GameProfile profile;

	@Shadow protected abstract GameProfile toOfflineProfile(GameProfile profile);

	@Shadow @Final
	static Logger LOGGER;
	private CompletableFuture<Property> skinrestorer_pendingSkin;
	@Inject(method = "onHello", at = @At("RETURN"))
	public void onHelloReturn(LoginHelloC2SPacket packet, CallbackInfo ci) {
		assert profile != null;
		GameProfile profile1;
		if (!profile.isComplete()) {
			profile1 = profile = toOfflineProfile(this.profile);
		} else {
			profile1 = profile;
		}

		skinrestorer_pendingSkin = CompletableFuture.supplyAsync(() -> {
			LOGGER.debug("Fetching {}'s skin", profile1.getName());
			if (SkinRestorer.getSkinStorage().getSkin(profile1.getId()) == SkinStorage.DEFAULT_SKIN)
				SkinRestorer.getSkinStorage().setSkin(profile1.getId(), MojangSkinProvider.getSkin(profile1.getName()));

			return SkinRestorer.getSkinStorage().getSkin(profile1.getId());
		});
	}

	@Inject(method = "acceptPlayer", at = @At("HEAD"), cancellable = true)
	public void waitForSkin(CallbackInfo ci) {
		if (skinrestorer_pendingSkin != null && !skinrestorer_pendingSkin.isDone()) {
			ci.cancel();
		}
	}

	private static void applyRestoredSkin(ServerPlayerEntity playerEntity, Property skin) {
		playerEntity.getGameProfile().getProperties().removeAll("textures");
		playerEntity.getGameProfile().getProperties().put("textures", skin);
	}

	@Inject(method = "addToServer", at = @At("HEAD"))
	public void applyRestoredSkinHook(ServerPlayerEntity player, CallbackInfo ci) {
		if (skinrestorer_pendingSkin != null) {
			applyRestoredSkin(player, skinrestorer_pendingSkin.getNow(SkinStorage.DEFAULT_SKIN));
		}
	}
}
