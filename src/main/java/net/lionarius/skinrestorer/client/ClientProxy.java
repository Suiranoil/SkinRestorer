package net.lionarius.skinrestorer.client;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.network.SkinUpdate;
import net.lionarius.skinrestorer.proxy.CommonProxy;
import net.minecraft.network.NetworkManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/** Loaded by the sided proxy only on physical clients. */
public final class ClientProxy extends CommonProxy {

    private boolean registered;
    private boolean unavailableLogged;

    @Override
    public void registerEvents() {
        if (registered) return;
        registered = true;
        try {
            ClientBridge.registerClientThread();
            FMLCommonHandler.instance()
                .bus()
                .register(this);
        } catch (RuntimeException | LinkageError failure) {
            fail(null, failure);
        }
    }

    @Override
    public void receiveSkin(SkinUpdate update) {
        try {
            ClientBridge.receiveSkin(update);
        } catch (RuntimeException | LinkageError failure) {
            fail(update == null ? null : update.connection, failure);
        }
    }

    @Override
    public void receiveHello(NetworkManager manager) {
        try {
            ClientBridge.acknowledge(manager);
        } catch (RuntimeException | LinkageError failure) {
            fail(manager, failure);
        }
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            ClientBridge.connected(event.manager);
        } catch (RuntimeException | LinkageError failure) {
            fail(event == null ? null : event.manager, failure);
        }
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        try {
            ClientBridge.disconnected(event.manager);
        } catch (RuntimeException | LinkageError failure) {
            fail(event == null ? null : event.manager, failure);
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        try {
            if (event.phase == TickEvent.Phase.END) ClientBridge.tick();
        } catch (RuntimeException | LinkageError failure) {
            fail(null, failure);
        }
    }

    private void fail(NetworkManager manager, Throwable cause) {
        if (manager != null) {
            try {
                ClientBridge.failure(manager, cause);
                return;
            } catch (RuntimeException | LinkageError ignored) {
                // The optional bridge itself may be missing; do not rethrow from FML ingress.
            }
        }
        if (unavailableLogged) return;
        unavailableLogged = true;
        try {
            SkinRestorer.LOGGER
                .warn("SkinRestorer client bridge is unavailable; normal game loading continues.", cause);
        } catch (RuntimeException | LinkageError ignored) {
            // Do not turn a logger/class-loading failure into a startup failure.
        }
    }
}
