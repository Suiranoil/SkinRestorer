package net.lionarius.skinrestorer.skin;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record SkinTarget(@NotNull UUID id, @Nullable String name) {

    public static SkinTarget of(UUID id) {
        return new SkinTarget(id, null);
    }

    public static SkinTarget of(GameProfile profile) {
        return new SkinTarget(profile.getId(), profile.getName());
    }

    public static SkinTarget of(ServerPlayer player) {
        return SkinTarget.of(player.getGameProfile());
    }

    // a proxied offline-mode server hands its players the forwarded id, not the name-derived one,
    // so a connected player is the only reliable source for the id here
    public static SkinTarget byOfflineName(MinecraftServer server, String name) {
        var player = server.getPlayerList().getPlayerByName(name);
        if (player != null) return SkinTarget.of(player);

        return new SkinTarget(UUIDUtil.createOfflinePlayerUUID(name), name);
    }

    public Component displayName(MinecraftServer server) {
        var player = server.getPlayerList().getPlayer(this.id);
        if (player != null) {
            var displayName = player.getDisplayName();
            if (displayName != null) return displayName;
        }

        if (this.name != null) return Component.literal(this.name);

        // get(UUID) is a plain map read, unlike get(String) which falls through to the session service
        return server.getProfileCache()
                .get(this.id)
                .map(profile -> Component.literal(profile.getName()))
                .orElseGet(() -> Component.literal(this.id.toString()));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SkinTarget other && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
