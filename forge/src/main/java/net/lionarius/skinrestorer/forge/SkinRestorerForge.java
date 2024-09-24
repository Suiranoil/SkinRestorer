package net.lionarius.skinrestorer.forge;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.command.SkinCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(SkinRestorer.MOD_ID)
@Mod.EventBusSubscriber(modid = SkinRestorer.MOD_ID)
public final class SkinRestorerForge {
    
    public SkinRestorerForge() {
        MinecraftForge.EVENT_BUS.register(SkinRestorerForge.class);
        
        SkinRestorer.onInitialize();
    }
    
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        SkinCommand.register(event.getDispatcher());
    }
    
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SkinRestorer.onServerStarted(event.getServer());
    }
}
