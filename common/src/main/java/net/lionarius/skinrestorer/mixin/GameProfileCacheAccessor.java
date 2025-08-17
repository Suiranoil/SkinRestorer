package net.lionarius.skinrestorer.mixin;

import net.minecraft.server.players.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(GameProfileCache.class)
public interface GameProfileCacheAccessor {
    
    @Accessor
    Map<String, GameProfileCache.GameProfileInfo> getProfilesByName();
}
