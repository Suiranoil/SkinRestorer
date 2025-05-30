package net.lionarius.skinrestorer.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerLoginPacketListenerImpl.class)
public interface ServerLoginPacketListenerImplAccessorInvoker {
    
    @Accessor
    GameProfile getGameProfile();
    
    @Invoker
    GameProfile invokeCreateFakeProfile(GameProfile original);
}
