package net.lionarius.skinrestorer.mixin;

import java.util.Set;

import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTrackerEntry.class)
public interface EntityTrackerEntryAccessor {

    @Accessor("trackingPlayers")
    Set<EntityPlayerMP> skinrestorer$getObservers();
}
