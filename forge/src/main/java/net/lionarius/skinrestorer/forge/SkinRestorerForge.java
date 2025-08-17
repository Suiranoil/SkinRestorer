package net.lionarius.skinrestorer.forge;

import net.lionarius.skinrestorer.SkinRestorer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(SkinRestorer.MOD_ID)
@Mod.EventBusSubscriber(modid = SkinRestorer.MOD_ID)
public final class SkinRestorerForge {
    
    public SkinRestorerForge() {
        SkinRestorer.onInitialize();
    }
    
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        SkinRestorer.Events.onCommandRegister(event.getDispatcher());
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SkinRestorer.Events.onServerStarted(event.getServer());
    }
    
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SkinRestorer.Events.onServerStopped(event.getServer());
    }
}
