package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerAccessor {
    @Accessor("gameProfile")
    @Mutable
    void setGameProfile(GameProfile properties);
}
