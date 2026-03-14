package net.lionarius.skinrestorer;

import com.mojang.brigadier.CommandDispatcher;
import net.lionarius.skinrestorer.command.SkinCommand;
import net.lionarius.skinrestorer.config.Config;
import net.lionarius.skinrestorer.config.provider.BuiltInProviderConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomProviderConfig;
import net.lionarius.skinrestorer.mineskin.MineskinService;
import net.lionarius.skinrestorer.platform.Services;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.SkinStorage;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.skin.provider.SkinProviderRegistry;
import net.lionarius.skinrestorer.skin.provider.builtin.*;
import net.lionarius.skinrestorer.translation.Translation;
import net.lionarius.skinrestorer.util.TickedScheduler;
import net.lionarius.skinrestorer.util.WebUtils;
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
import java.util.Optional;

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
        SkinRestorer.reloadConfig(true);
        
        SkinRestorer.providersRegistry.register(EmptySkinProvider.PROVIDER_NAME, SkinProvider.EMPTY, false);
        SkinRestorer.providersRegistry.register(SkinShuffleSkinProvider.PROVIDER_NAME, SkinProvider.SKIN_SHUFFLE, false);
        
        SkinRestorer.registerDefaultSkinProvider(MojangSkinProvider.PROVIDER_NAME, SkinProvider.MOJANG, SkinRestorer.getConfig().providersConfig().mojang());
        SkinRestorer.registerDefaultSkinProvider(ElyBySkinProvider.PROVIDER_NAME, SkinProvider.ELY_BY, SkinRestorer.getConfig().providersConfig().ely_by());
        SkinRestorer.registerDefaultSkinProvider(MineskinSkinProvider.PROVIDER_NAME, SkinProvider.MINESKIN, SkinRestorer.getConfig().providersConfig().mineskin());
        SkinRestorer.registerDefaultSkinProvider(CollectionSkinProvider.PROVIDER_NAME, SkinProvider.COLLECTION, SkinRestorer.getConfig().providersConfig().collection());
        SkinRestorer.registerCustomProviders(SkinRestorer.getConfig().providersConfig().custom());
        
        SkinRestorer.providersRegistry.reload();
        
        SkinRestorer.validateAutoFetchProvider();
    }
    
    private static void validateAutoFetchProvider() {
        var providerName = SkinRestorer.config.join().autoFetchConfig().provider();
        var provider = SkinRestorer.providersRegistry.get(providerName);
        
        if (provider == null) {
            SkinRestorer.LOGGER.warn("AutoFetch provider '{}' is not registered. Auto fetch skin fetching will be skipped.", providerName);
        } else if (provider.getParameterType() != SkinProviderParameterType.USERNAME) {
            SkinRestorer.LOGGER.warn("AutoFetch provider '{}' has parameter type {}, but only USERNAME providers are supported. Auto fetch skin fetching will be skipped.",
                    providerName, provider.getParameterType());
        }
    }
    
    private static void registerDefaultSkinProvider(String defaultName, SkinProvider provider, BuiltInProviderConfig config) {
        var isDefaultName = config.name().equals(defaultName);
        SkinRestorer.providersRegistry.register(defaultName, provider, config.enabled() && isDefaultName);
        
        if (!isDefaultName && !SkinProvider.BUILTIN_PROVIDER_NAMES.contains(config.name()))
            SkinRestorer.providersRegistry.register(config.name(), provider, config.enabled());
    }
    
    private static void registerCustomProviders(Collection<CustomProviderConfig> customProviders) {
        for (var customProvider : customProviders) {
            var providerName = customProvider.name();
            
            if (providerName.isEmpty()) {
                SkinRestorer.LOGGER.warn("Skipping custom provider with empty name");
                continue;
            }
            
            if (SkinProvider.BUILTIN_PROVIDER_NAMES.contains(providerName)) {
                SkinRestorer.LOGGER.warn("Skipping custom provider '{}' because it conflicts with a built-in provider name", providerName);
                continue;
            }
            
            if (SkinRestorer.providersRegistry.get(providerName) != null) {
                SkinRestorer.LOGGER.warn("Skipping custom provider '{}' because this name is already registered", providerName);
                continue;
            }
            
            var providerResult = customProvider.createSkinProvider(MineskinService.INSTANCE);
            if (providerResult.isError()) {
                SkinRestorer.LOGGER.warn("Skipping custom provider '{}' because {}", providerName, providerResult.getErrorValue());
                continue;
            }
            
            SkinRestorer.providersRegistry.register(providerName, providerResult.getSuccessValue(), customProvider.enabled());
        }
    }
    
    public static void reloadConfig() {
        SkinRestorer.reloadConfig(false);
    }
    
    public static void reloadConfig(boolean initial) {
        SkinRestorer.config = Config.load(SkinRestorer.getConfigDir());
        Translation.reloadTranslations();
        WebUtils.recreateHttpClient();
        MineskinService.INSTANCE.reload();
        
        SkinRestorer.providersRegistry.reload();
        if (!initial)
            SkinRestorer.validateAutoFetchProvider();
    }
    
    public static class Events {
        private Events() {}
        
        public static void onServerStarted(MinecraftServer server) {
            Path worldSkinDirectory = server.getWorldPath(LevelResource.ROOT).resolve(SkinRestorer.MOD_ID);
            
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
