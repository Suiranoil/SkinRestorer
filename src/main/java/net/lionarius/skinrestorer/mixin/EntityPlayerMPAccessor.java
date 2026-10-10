package net.lionarius.skinrestorer.mixin;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayerMP.class)
public interface EntityPlayerMPAccessor {

    @Accessor("destroyedItemsNetCache")
    List<Integer> skinrestorer$getPendingDestroy();
}
