package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.SkinStorage;
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

	@Shadow @Final
	static Logger LOGGER;
	private CompletableFuture<Property> skinrestorer_pendingSkin;

	@Inject(method = "tickVerify", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"), cancellable = true)
	public void waitForSkin(CallbackInfo ci) {
		if (skinrestorer_pendingSkin == null) {
			skinrestorer_pendingSkin = CompletableFuture.supplyAsync(() -> {
				LOGGER.debug("Fetching {}'s skin", profile.getName());
				if (SkinRestorer.getSkinStorage().getSkin(profile.getId()) == SkinStorage.DEFAULT_SKIN)
					SkinRestorer.getSkinStorage().setSkin(profile.getId(), MojangSkinProvider.getSkin(profile.getName()));

				return SkinRestorer.getSkinStorage().getSkin(profile.getId());
			});
		}

		if (!skinrestorer_pendingSkin.isDone()) {
			ci.cancel();
		}
	}

	@Inject(method = "sendSuccessPacket", at = @At("HEAD"))
	public void applyRestoredSkinHook(GameProfile profile, CallbackInfo ci) {
		if (skinrestorer_pendingSkin != null)
			SkinRestorer.applyRestoredSkin(profile, skinrestorer_pendingSkin.getNow(SkinStorage.DEFAULT_SKIN));
	}
}
