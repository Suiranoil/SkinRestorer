package net.lionarius.skinrestorer.server;

import java.util.function.Consumer;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ServerEvents {

    private boolean tickFailureReported;

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        SkinServer server = SkinRestorer.server();
        if (server == null || event.phase != TickEvent.Phase.END) return;
        try {
            server.tick();
        } catch (RuntimeException | LinkageError failure) {
            if (!tickFailureReported) {
                tickFailureReported = true;
                SkinRestorer.LOGGER.warn("Skin update processing failed; server gameplay continues", failure);
            }
        }
    }

    @SubscribeEvent
    public void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP player) dispatch(player, server -> server.joined(player));
    }

    @SubscribeEvent
    public void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP player) dispatch(player, server -> server.left(player));
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP player) dispatch(player, server -> server.rebuilt(player));
    }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP player) dispatch(player, server -> server.rebuilt(player));
    }

    private void dispatch(EntityPlayerMP player, Consumer<SkinServer> operation) {
        SkinServer server = SkinRestorer.server();
        if (server == null) {
            if (SkinRestorer.startupWarning() != null) SkinServer.say(player, SkinRestorer.startupWarning());
            return;
        }
        try {
            operation.accept(server);
        } catch (RuntimeException | LinkageError failure) {
            SkinServer.say(player, "This skin update could not be completed; normal gameplay continues.");
            SkinRestorer.LOGGER.warn("Skin player event failed for {}", player.getCommandSenderName(), failure);
        }
    }
}
