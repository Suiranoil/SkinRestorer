package net.lionarius.skinrestorer.network;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.server.SkinServer;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public final class SkinNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("skinrestorer");
    private static boolean ready;

    private SkinNetwork() {}

    public static void initialize() {
        CHANNEL.registerMessage(HelloHandler.class, Hello.class, 0, Side.SERVER);
        CHANNEL.registerMessage(UpdateHandler.class, SkinUpdate.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(HelloReplyHandler.class, HelloReply.class, 2, Side.CLIENT);
        ready = true;
    }

    public static void hello() {
        ensureReady();
        CHANNEL.sendToServer(new Hello());
    }

    public static void send(net.minecraft.entity.player.EntityPlayerMP player, SkinUpdate update) {
        ensureReady();
        CHANNEL.sendTo(update, player);
    }

    private static void ensureReady() {
        if (!ready) throw new IllegalStateException("Skin synchronization is unavailable");
    }

    public static class Hello implements IMessage {

        private int version = 1;

        @Override
        public void fromBytes(ByteBuf buffer) {
            if (buffer.readableBytes() != 4) {
                version = 0;
                SkinRestorer.LOGGER.warn("Ignoring invalid skin handshake; gameplay continues");
                return;
            }
            version = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeInt(version);
        }
    }

    public static class HelloHandler implements IMessageHandler<Hello, IMessage> {

        @Override
        public IMessage onMessage(Hello hello, MessageContext context) {
            try {
                if (hello.version == 1) {
                    SkinServer server = SkinRestorer.server();
                    if (server != null) {
                        net.minecraft.entity.player.EntityPlayerMP player = context.getServerHandler().playerEntity;
                        server.enqueue(() -> server.handshake(player));
                    }
                }
            } catch (RuntimeException | LinkageError failure) {
                SkinRestorer.LOGGER.warn("Skin handshake failed; gameplay continues", failure);
            }
            return null;
        }
    }

    public static void acknowledge(net.minecraft.entity.player.EntityPlayerMP player) {
        ensureReady();
        CHANNEL.sendTo(new HelloReply(), player);
    }

    public static class HelloReply implements IMessage {

        private boolean valid = true;

        @Override
        public void fromBytes(ByteBuf buffer) {
            valid = !buffer.isReadable();
            if (!valid) SkinRestorer.LOGGER.warn("Ignoring invalid skin handshake reply; gameplay continues");
        }

        @Override
        public void toBytes(ByteBuf buffer) {}
    }

    public static class HelloReplyHandler implements IMessageHandler<HelloReply, IMessage> {

        @Override
        public IMessage onMessage(HelloReply reply, MessageContext context) {
            try {
                if (reply.valid) {
                    SkinRestorer.proxy.receiveHello(
                        context.getClientHandler()
                            .getNetworkManager());
                }
            } catch (RuntimeException | LinkageError failure) {
                SkinRestorer.LOGGER.warn("Skin handshake reply could not be processed; gameplay continues", failure);
            }
            return null;
        }
    }

    public static class UpdateHandler implements IMessageHandler<SkinUpdate, IMessage> {

        @Override
        public IMessage onMessage(SkinUpdate update, MessageContext context) {
            try {
                update.connection = context.getClientHandler()
                    .getNetworkManager();
                SkinRestorer.proxy.receiveSkin(update);
            } catch (RuntimeException | LinkageError failure) {
                SkinRestorer.LOGGER.warn("Skin display update failed; gameplay continues", failure);
            }
            return null;
        }
    }
}
