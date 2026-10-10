package net.lionarius.skinrestorer.server;

import java.util.ArrayList;
import java.util.Set;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.mixin.EntityPlayerMPAccessor;
import net.lionarius.skinrestorer.mixin.EntityTrackerAccessor;
import net.lionarius.skinrestorer.mixin.EntityTrackerEntryAccessor;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S13PacketDestroyEntities;

public final class ObserverRefresh {

    private ObserverRefresh() {}

    public static void refresh(EntityPlayerMP player) {
        EntityTracker tracker = player.getServerForPlayer()
            .getEntityTracker();
        if (!(tracker instanceof EntityTrackerAccessor)) {
            throw new IllegalStateException("Observer skin hook is unavailable");
        }
        EntityTrackerEntry entry = (EntityTrackerEntry) ((EntityTrackerAccessor) tracker)
            .skinrestorer$getTrackedEntities()
            .lookup(player.getEntityId());
        if (entry == null) return;
        if (!(entry instanceof EntityTrackerEntryAccessor)) {
            throw new IllegalStateException("Observer tracking hook is unavailable");
        }
        Set<EntityPlayerMP> observers = ((EntityTrackerEntryAccessor) entry).skinrestorer$getObservers();
        for (EntityPlayerMP observer : new ArrayList<>(observers)) {
            boolean removed = false;
            try {
                if (observer == player || !observer.playerNetServerHandler.netManager.isChannelOpen()) continue;
                if (!(observer instanceof EntityPlayerMPAccessor)) {
                    throw new IllegalStateException("Observer removal hook is unavailable");
                }
                ((EntityPlayerMPAccessor) observer).skinrestorer$getPendingDestroy()
                    .removeIf(id -> id == player.getEntityId());
                observer.playerNetServerHandler.sendPacket(new S13PacketDestroyEntities(player.getEntityId()));
                observers.remove(observer);
                removed = true;
                entry.tryStartWachingThis(observer);
            } catch (RuntimeException | LinkageError failure) {
                if (removed) observers.add(observer);
                SkinServer.say(
                    observer,
                    "A player skin could not refresh; gameplay continues. Re-enter the view area to retry.");
                SkinRestorer.LOGGER
                    .warn("Observer skin refresh failed for {}", observer.getCommandSenderName(), failure);
            }
        }
    }
}
