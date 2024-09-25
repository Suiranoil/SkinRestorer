package net.lionarius.skinrestorer;

import com.mojang.authlib.GameProfile;
import net.lionarius.skinrestorer.config.BuiltInProviderConfig;
import net.lionarius.skinrestorer.config.Config;
import net.lionarius.skinrestorer.platform.Services;
import net.lionarius.skinrestorer.skin.SkinIO;
import net.lionarius.skinrestorer.skin.SkinStorage;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.provider.*;
import net.lionarius.skinrestorer.translation.Translation;
import net.lionarius.skinrestorer.util.FileUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
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
    
    public static Optional<SkinProvider> getProvider(String name) {
        return Optional.ofNullable(SkinRestorer.providersRegistry.get(name));
    }
    
    public static void onInitialize() {
        SkinRestorer.configDir = Services.PLATFORM.getConfigDirectory().resolve(SkinRestorer.MOD_ID);
        SkinRestorer.reloadConfig();
        
        SkinRestorer.providersRegistry.register(EmptySkinProvider.PROVIDER_NAME, SkinProvider.EMPTY, false);
        
        SkinRestorer.registerDefaultSkinProvider(MojangSkinProvider.PROVIDER_NAME, SkinProvider.MOJANG, SkinRestorer.getConfig().providersConfig().mojang());
        SkinRestorer.registerDefaultSkinProvider(ElyBySkinProvider.PROVIDER_NAME, SkinProvider.ELY_BY, SkinRestorer.getConfig().providersConfig().ely_by());
        SkinRestorer.registerDefaultSkinProvider(MineskinSkinProvider.PROVIDER_NAME, SkinProvider.MINESKIN, SkinRestorer.getConfig().providersConfig().mineskin());
    }
    
    private static void registerDefaultSkinProvider(String defaultName, SkinProvider provider, BuiltInProviderConfig config) {
        var isDefaultName = config.name().equals(defaultName);
        SkinRestorer.providersRegistry.register(defaultName, provider, config.enabled() && isDefaultName);
        
        if (!isDefaultName && Arrays.stream(SkinProvider.BUILTIN_PROVIDER_NAMES).noneMatch(name -> name.equals(config.name())))
            SkinRestorer.providersRegistry.register(config.name(), provider, config.enabled());
    }
    
    public static void onServerStarted(MinecraftServer server) {
        Path worldSkinDirectory = server.getWorldPath(LevelResource.ROOT).resolve(SkinRestorer.MOD_ID);
        FileUtils.tryMigrateOldSkinDirectory(SkinRestorer.getConfigDir(), worldSkinDirectory);
        
        SkinRestorer.skinStorage = new SkinStorage(new SkinIO(worldSkinDirectory));
    }
    
    public static void reloadConfig() {
        SkinRestorer.config = Config.load(SkinRestorer.getConfigDir());
        Translation.reloadTranslations();
        WebUtils.recreateHttpClient();
        
        MojangSkinProvider.createCache();
        ElyBySkinProvider.createCache();
        MineskinSkinProvider.createCache();
    }
    
    public static String assetPath(String name) {
        return String.format("/assets/%s/%s", SkinRestorer.MOD_ID, name);
    }
    
    public static Collection<ServerPlayer> applySkin(MinecraftServer server, Iterable<GameProfile> targets, SkinValue value, boolean save) {
        var acceptedPlayers = new HashSet<ServerPlayer>();
        
        for (var profile : targets) {
            if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId()))
                value = value.setOriginalValue(PlayerUtils.getPlayerSkin(profile));
            
            if (save)
                SkinRestorer.getSkinStorage().setSkin(profile.getId(), value);
            
            if (PlayerUtils.areSkinPropertiesEquals(value.value(), PlayerUtils.getPlayerSkin(profile)))
                continue;
            
            PlayerUtils.applyRestoredSkin(profile, value.value());
            
            var player = server.getPlayerList().getPlayer(profile.getId());
            if (player == null)
                continue;
            
            PlayerUtils.refreshPlayer(player);
            acceptedPlayers.add(player);
        }
        
        return acceptedPlayers;
    }
    
    public static Collection<ServerPlayer> applySkin(MinecraftServer server, Iterable<GameProfile> targets, SkinValue value) {
        return SkinRestorer.applySkin(server, targets, value, true);
    }
    
    public static CompletableFuture<Result<Collection<ServerPlayer>, String>> setSkinAsync(
            MinecraftServer server,
            Collection<GameProfile> targets,
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
                        return Result.<Collection<ServerPlayer>, String>error(skinResult.getErrorValue().getMessage());
                    
                    var skinValue = SkinValue.fromProviderContextWithValue(context, skinResult.getSuccessValue().orElse(null));
                    
                    var acceptedPlayers = SkinRestorer.applySkin(server, targets, skinValue, save);
                    
                    return Result.<Collection<ServerPlayer>, String>success(acceptedPlayers);
                }, server)
                .exceptionally(e -> {
                    SkinRestorer.LOGGER.error(e.toString());
                    return Result.error(e.getMessage());
                });
    }
}
