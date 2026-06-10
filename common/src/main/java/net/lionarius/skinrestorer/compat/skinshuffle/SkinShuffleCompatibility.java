package net.lionarius.skinrestorer.compat.skinshuffle;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.platform.Services;
import net.lionarius.skinrestorer.skin.SkinService;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.builtin.SkinShuffleSkinProvider;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;

public class SkinShuffleCompatibility {
    public static final String MOD_ID = "skinshuffle";

    private static final boolean SHOULD_APPLY = !Services.PLATFORM.isModLoaded(SkinShuffleCompatibility.MOD_ID);

    private SkinShuffleCompatibility() {}

    public static boolean shouldApply() {
        return SkinShuffleCompatibility.SHOULD_APPLY;
    }

    public static Identifier resourceLocation(String name) {
        return Identifier.fromNamespaceAndPath(SkinShuffleCompatibility.MOD_ID, name);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        Services.COMPATIBILITY.skinShuffle_sendHandshake(player);
    }

    public static void handleSkinRefresh(
            MinecraftServer server, ServerPlayer player, SkinShuffleSkinRefreshPayload payload) {
        var property = payload.textureProperty();

        if (!property.name().equals(PlayerUtils.TEXTURES_KEY)) return;

        if (!property.hasSignature()) return;

        server.execute(() -> {
            SkinService.applySkin(
                    server,
                    Collections.singleton(player),
                    new SkinValue(SkinShuffleSkinProvider.PROVIDER_NAME, null, null, property),
                    !server.usesAuthentication());

            if (server.usesAuthentication() && SkinRestorer.getSkinStorage().hasSavedSkin(player.getUUID()))
                SkinRestorer.getSkinStorage().deleteSkin(player.getUUID());
        });
    }
}
