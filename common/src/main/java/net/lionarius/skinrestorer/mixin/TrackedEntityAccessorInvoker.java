package net.lionarius.skinrestorer.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface TrackedEntityAccessorInvoker {
    @Accessor
    Set<ServerPlayerConnection> getSeenBy();

    @Invoker
    void invokeRemovePlayer(ServerPlayer player);

    @Invoker
    void invokeUpdatePlayer(ServerPlayer player);
}
