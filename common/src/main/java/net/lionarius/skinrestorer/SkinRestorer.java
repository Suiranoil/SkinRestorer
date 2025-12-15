package net.lionarius.skinrestorer;

import com.google.common.base.Throwables;
import com.mojang.brigadier.CommandDispatcher;
import net.lionarius.skinrestorer.command.SkinCommand;
import net.lionarius.skinrestorer.config.Config;
import net.lionarius.skinrestorer.config.provider.BuiltInProviderConfig;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.mixin.PlayerAccessor;
import net.lionarius.skinrestorer.platform.Services;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.SkinStorage;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.*;
import net.lionarius.skinrestorer.translation.Translation;
import net.lionarius.skinrestorer.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class SkinRestorer {
    public static final String MOD_ID = "skinrestorer";
    public static final Logger LOGGER = LoggerFactory.getLogger("SkinRestorer");
    
    private static final SkinProviderRegistry providersRegistry = new SkinProviderRegistry();
    private static SkinStorage skinStorage;
    private static Path configDir;
    private static Config config;
    private static TickedScheduler tickedScheduler;
    private static MinecraftServer minecraftServer;
    
    private SkinRestorer() {}
    
    public static SkinStorage getSkinStorage() {
        return SkinRestorer.skinStorage;
    }
    
    public static Path getConfigDir() {
        return SkinRestorer.configDir;
    }
    
    public static Config getConfig() {
        return SkinRestorer.config;
    }
    
    public static SkinProviderRegistry getProvidersRegistry() {
        return SkinRestorer.providersRegistry;
    }
    
    public static TickedScheduler getTickedScheduler() {
        return SkinRestorer.tickedScheduler;
    }
    
    public static @Nullable MinecraftServer getMinecraftServer() {
        return SkinRestorer.minecraftServer;
    }
    
    public static Optional<SkinProvider> getProvider(String name) {
        return Optional.ofNullable(SkinRestorer.providersRegistry.get(name));
    }
    
    public static Identifier resourceLocation(String name) {
        return Identifier.fromNamespaceAndPath(SkinRestorer.MOD_ID, name);
    }
    
    public static String assetPath(String name) {
        return String.format("/assets/%s/%s", SkinRestorer.MOD_ID, name);
    }
    
    public static void onInitialize() {
        SkinRestorer.configDir = Services.PLATFORM.getConfigDirectory().resolve(SkinRestorer.MOD_ID);
        SkinRestorer.reloadConfig();
        
        SkinRestorer.providersRegistry.register(EmptySkinProvider.PROVIDER_NAME, SkinProvider.EMPTY, false);
        SkinRestorer.providersRegistry.register(SkinShuffleSkinProvider.PROVIDER_NAME, SkinProvider.SKIN_SHUFFLE, false);
        
        SkinRestorer.registerDefaultSkinProvider(MojangSkinProvider.PROVIDER_NAME, SkinProvider.MOJANG, SkinRestorer.getConfig().providersConfig().mojang());
        SkinRestorer.registerDefaultSkinProvider(ElyBySkinProvider.PROVIDER_NAME, SkinProvider.ELY_BY, SkinRestorer.getConfig().providersConfig().ely_by());
        SkinRestorer.registerDefaultSkinProvider(MineskinSkinProvider.PROVIDER_NAME, SkinProvider.MINESKIN, SkinRestorer.getConfig().providersConfig().mineskin());
    }
    
    private static void registerDefaultSkinProvider(String defaultName, SkinProvider provider, BuiltInProviderConfig config) {
        var isDefaultName = config.name().equals(defaultName);
        SkinRestorer.providersRegistry.register(defaultName, provider, config.enabled() && isDefaultName);
        
        if (!isDefaultName && !SkinProvider.BUILTIN_PROVIDER_NAMES.contains(config.name()))
            SkinRestorer.providersRegistry.register(config.name(), provider, config.enabled());
    }
    
    public static void reloadConfig() {
        SkinRestorer.config = Config.load(SkinRestorer.getConfigDir());
        Translation.reloadTranslations();
        WebUtils.recreateHttpClient();
        
        MojangSkinProvider.reload();
        ElyBySkinProvider.reload();
        MineskinSkinProvider.reload();
    }
    
    public static Collection<ServerPlayer> applySkin(MinecraftServer server, Iterable<ServerPlayer> targets, SkinValue value, boolean save) {
        var acceptedPlayers = new HashSet<ServerPlayer>();
        
        for (var player : targets) {
            var profile = player.getGameProfile();
            var skin = PlayerUtils.getPlayerSkin(profile);
            
            if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.id()))
                value = value.setOriginalValue(skin);
            
            if (PlayerUtils.areSkinPropertiesEquals(value.value(), skin))
                continue;
            
            if (save)
                SkinRestorer.getSkinStorage().setSkin(profile.id(), value);
            
            var newProfile = PlayerUtils.applyRestoredSkin(profile, value.value());
            ((PlayerAccessor) player).setGameProfile(newProfile);
            
            if (player.connection == null)
                continue;
            
            PlayerUtils.refreshPlayer(player);
            acceptedPlayers.add(player);
            
            SkinRestorer.getTickedScheduler().cancel(player.getUUID());
        }
        
        return acceptedPlayers;
    }
    
    public static Collection<ServerPlayer> applySkin(MinecraftServer server, Iterable<ServerPlayer> targets, SkinValue value) {
        return SkinRestorer.applySkin(server, targets, value, true);
    }
    
    public static CompletableFuture<Result<Collection<ServerPlayer>, String>> setSkinAsync(
            MinecraftServer server,
            Collection<ServerPlayer> targets,
            SkinProviderContext context,
            boolean save
    ) {
        return CompletableFuture.supplyAsync(
                        () -> SkinRestorer.getProvider(context.name()).map(provider -> provider.fetchSkin(context.argument(), context.variant()))
                )
                .thenApplyAsync(result -> {
                    if (result.isEmpty())
                        return Result.<Collection<ServerPlayer>, String>error("provider '" + context.name() + "' is not registered");
                    
                    var skinResult = result.get();
                    if (skinResult.isError())
                        throw new TransparentException(Throwables.getRootCause(skinResult.getErrorValue()));
                    
                    var skinValue = SkinValue.fromProviderContextWithValue(context, skinResult.getSuccessValue().orElse(null));
                    
                    var acceptedPlayers = SkinRestorer.applySkin(server, targets, skinValue, save);
                    
                    return Result.<Collection<ServerPlayer>, String>success(acceptedPlayers);
                }, server)
                .exceptionally(e -> {
                    SkinRestorer.LOGGER.error("Failed to set skin '{}:{}'", context.name(), context.argument(), e);
                    return Result.error(e.getMessage());
                });
    }
    
    public static class Events {
        private Events() {}
        
        public static void onServerStarted(MinecraftServer server) {
            Path worldSkinDirectory = server.getWorldPath(LevelResource.ROOT).resolve(SkinRestorer.MOD_ID);
            FileUtils.tryMigrateOldSkinDirectory(SkinRestorer.getConfigDir(), worldSkinDirectory);
            
            SkinRestorer.skinStorage = new SkinStorage(new SkinIO(worldSkinDirectory));
            SkinRestorer.tickedScheduler = new TickedScheduler(server);
            server.addTickable(SkinRestorer.tickedScheduler);
            
            SkinRestorer.minecraftServer = server;
        }
        
        public static void onServerStopped(MinecraftServer server) {
            SkinRestorer.skinStorage = null;
            SkinRestorer.tickedScheduler = null;
            SkinRestorer.minecraftServer = null;
        }
        
        public static void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher) {
            SkinCommand.register(dispatcher);
        }
        
        public static void onPlayerDisconnect(ServerPlayer player) {
            SkinRestorer.getSkinStorage().removeSkin(player.getUUID());
        }
    }
}
