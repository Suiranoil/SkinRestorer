package net.lionarius.skinrestorer.neoforge;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.command.SkinCommand;
import net.lionarius.skinrestorer.compat.skinshuffle.SkinShuffleCompatibility;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(SkinRestorer.MOD_ID)
@EventBusSubscriber(modid = SkinRestorer.MOD_ID)
public final class SkinRestorerNeoForge {
    
    public SkinRestorerNeoForge() {
        SkinRestorer.onInitialize();
        
        if (SkinShuffleCompatibility.shouldApply())
            net.lionarius.skinrestorer.neoforge.compat.skinshuffle.SkinShuffleCompatibility.initialize();
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
