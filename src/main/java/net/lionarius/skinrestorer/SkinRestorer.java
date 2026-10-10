package net.lionarius.skinrestorer;

import java.io.File;
import java.util.Map;

import net.lionarius.skinrestorer.command.SkinCommand;
import net.lionarius.skinrestorer.network.SkinNetwork;
import net.lionarius.skinrestorer.proxy.CommonProxy;
import net.lionarius.skinrestorer.server.ServerEvents;
import net.lionarius.skinrestorer.server.SkinServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = SkinRestorer.MOD_ID,
    name = "SkinRestorer",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:unimixins")
public final class SkinRestorer {

    public static final String MOD_ID = "skinrestorer";
    public static final Logger LOGGER = LogManager.getLogger("SkinRestorer");

    @SidedProxy(
        clientSide = "net.lionarius.skinrestorer.client.ClientProxy",
        serverSide = "net.lionarius.skinrestorer.proxy.CommonProxy")
    public static CommonProxy proxy;

    private static volatile SkinServer server;
    private static File configFile;
    private static volatile String startupWarning;
    private SkinConfig config = SkinConfig.defaults();
    private boolean lifecycleReady;

    @Mod.EventHandler
    public void initialize(FMLPreInitializationEvent event) {
        configFile = new File(event.getModConfigurationDirectory(), "skinrestorer.cfg");
        try {
            config = SkinConfig.load(configFile);
        } catch (RuntimeException | LinkageError failure) {
            startupWarning = "SkinRestorer configuration could not be loaded; safe defaults are active.";
            LOGGER.warn(startupWarning, failure);
        }
        try {
            SkinNetwork.initialize();
        } catch (RuntimeException | LinkageError failure) {
            startupWarning = "Skin synchronization is unavailable; normal gameplay remains available.";
            LOGGER.warn(startupWarning, failure);
        }
        try {
            FMLCommonHandler.instance()
                .bus()
                .register(new ServerEvents());
            lifecycleReady = true;
        } catch (RuntimeException | LinkageError failure) {
            startupWarning = "SkinRestorer is unavailable; normal gameplay remains available.";
            LOGGER.warn(startupWarning, failure);
        }
        try {
            proxy.registerEvents();
        } catch (RuntimeException | LinkageError failure) {
            startupWarning = "Client skin synchronization is unavailable; normal gameplay remains available.";
            LOGGER.warn(startupWarning, failure);
            proxy = new CommonProxy();
        }
    }

    @NetworkCheckHandler
    public boolean checkRemote(Map<String, String> mods, Side side) {
        return true;
    }

    @Mod.EventHandler
    public void start(FMLServerStartingEvent event) {
        if (lifecycleReady) {
            try {
                server = new SkinServer(
                    event.getServer(),
                    event.getServer()
                        .getFile(
                            event.getServer()
                                .getFolderName())
                        .toPath(),
                    config);
            } catch (RuntimeException | LinkageError failure) {
                server = null;
                startupWarning = "SkinRestorer could not start; normal login and gameplay remain available.";
                LOGGER.warn(startupWarning, failure);
            }
        }
        event.registerServerCommand(new SkinCommand());
    }

    @Mod.EventHandler
    public void stop(FMLServerStoppingEvent event) {
        SkinServer previous = server;
        server = null;
        if (previous != null) {
            try {
                previous.close();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.warn("SkinRestorer cleanup failed; server shutdown will continue", failure);
            }
        }
    }

    public static SkinServer server() {
        return server;
    }

    public static File configFile() {
        return configFile;
    }

    public static String startupWarning() {
        return startupWarning;
    }
}
