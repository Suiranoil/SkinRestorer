package net.lionarius.skinrestorer.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinService;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;
import net.lionarius.skinrestorer.translation.Translation;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class SkinCommand {

    private SkinCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var base = literal("skin")
                .then(buildSetSubcommand("clear", SkinValue.EMPTY::toProviderContext))
                .then(literal("reset")
                        .executes(context -> resetSubcommand(context.getSource()))
                        .then(makeTargetsArgument(
                                (context, profiles) -> resetSubcommand(context.getSource(), profiles, true))))
                .then(literal("refresh").executes(context -> refreshSubcommand(context.getSource())));

        var set = literal("set");

        var providers = SkinRestorer.getProvidersRegistry().getPublicProviders();
        for (var entry : providers) set.then(buildSetSubcommand(entry.first(), entry.second()));
        if (!providers.isEmpty()) base.then(set);

        base.then(literal("config")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(4))
                .then(literal("reload").executes(SkinCommand::configReloadSubcommand)));

        dispatcher.register(base);
    }

    private static int refreshSubcommand(CommandSourceStack src) {
        var player = src.getPlayer();
        if (player == null) return 0;

        var profile = player.getGameProfile();

        SkinProviderContext context = null;
        var save = true;
        if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId())) {
            if (profile.getProperties().containsKey(PlayerUtils.TEXTURES_KEY)) {
                save = false;
                context = MojangSkinProvider.skinProviderContextFromProfile(profile);
            }
        } else {
            context = SkinRestorer.getSkinStorage().getSkin(profile.getId()).toProviderContext();
        }
        
        if (context == null)
            return 0;
        
        return SkinCommand.setSubcommand(src, Collections.singleton(profile), context, save, false);
    }
    
    private static int resetSubcommand(
            CommandSourceStack src,
            Collection<GameProfile> targets,
            boolean setByOperator
    ) {
        var updatedPlayers = new HashSet<ServerPlayer>();
        for (var profile : targets) {
            SkinValue skin = null;
            if (SkinRestorer.getSkinStorage().hasSavedSkin(profile.getId()))
                skin = SkinRestorer.getSkinStorage().getSkin(profile.getId()).replaceValueWithOriginal();
            
            if (skin == null)
                continue;
            
            var updatedPlayer = SkinService.applySkin(src.getServer(), Collections.singleton(profile), skin, false);
            SkinRestorer.getSkinStorage().deleteSkin(profile.getId());
            
            updatedPlayers.addAll(updatedPlayer);
        }

        SkinCommand.sendResponse(src, updatedPlayers, setByOperator);

        return targets.size();
    }
    
    private static int resetSubcommand(
            CommandSourceStack src
    ) {
        if (src.getPlayer() == null)
            return 0;
        
        return resetSubcommand(src, Collections.singleton(src.getPlayer().getGameProfile()), false);
    }

    private static int setSubcommand(
            CommandSourceStack src,
            Collection<GameProfile> targets,
            SkinProviderContext context,
            boolean save,
            boolean setByOperator) {
        src.sendSystemMessage(Translation.translatableWithFallback(Translation.COMMAND_SKIN_LOADING_KEY));
        
        SkinService.setSkinAsync(src.getServer(), targets, context, save).thenAccept(result -> {
            if (result.isError()) {
                src.sendFailure(Translation.translatableWithFallback(
                        Translation.COMMAND_SKIN_FAILED_KEY, result.getErrorValue()));
                return;
            }

            var updatedPlayers = result.getSuccessValue();

            SkinCommand.sendResponse(src, updatedPlayers, setByOperator);
        });

        return targets.size();
    }

    private static int setSubcommand(
            CommandSourceStack src,
            Collection<GameProfile> targets,
            SkinProviderContext context,
            boolean setByOperator
    ) {
        return SkinCommand.setSubcommand(src, targets, context, true, setByOperator);
    }
    
    private static int setSubcommand(
            CommandSourceStack src,
            SkinProviderContext context
    ) {
        if (src.getPlayer() == null)
            return 0;
        
        return setSubcommand(src, Collections.singleton(src.getPlayer().getGameProfile()), context, false);
    }

    private static int configReloadSubcommand(CommandContext<CommandSourceStack> context) {
        SkinRestorer.reloadConfig();

        context.getSource()
                .sendSuccess(
                        () -> Translation.translatableWithFallback(Translation.COMMAND_SKIN_CONFIG_RELOADED_KEY), true);

        return 0;
    }

    private static void sendResponse(
            CommandSourceStack src, Collection<ServerPlayer> updatedPlayers, boolean setByOperator) {
        if (updatedPlayers.isEmpty()) {
            src.sendSuccess(() -> Translation.translatableWithFallback(Translation.COMMAND_SKIN_NO_CHANGES_KEY), true);
            return;
        }

        if (setByOperator) {
            var playersComponent = PlayerUtils.createPlayerListComponent(updatedPlayers);

            src.sendSuccess(
                    () -> Translation.translatableWithFallback(
                            Translation.COMMAND_SKIN_AFFECTED_PLAYERS_KEY, playersComponent),
                    true);
        } else {
            src.sendSuccess(() -> Translation.translatableWithFallback(Translation.COMMAND_SKIN_OK_KEY), true);
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSetSubcommand(String name, SkinProvider provider) {
        var action = literal(name);

        if (provider.hasVariantSupport()) {
            for (SkinVariant variant : SkinVariant.values()) {
                action.then(literal(variant.toString())
                        .then(buildSetSubcommandArgument(
                                argument(provider.getArgumentName(), StringArgumentType.string()), context -> {
                                    var argument = StringArgumentType.getString(context, provider.getArgumentName());
                                    return new SkinProviderContext(name, argument, variant);
                                })));
            }
        } else {
            action.then(buildSetSubcommandArgument(
                    argument(provider.getArgumentName(), StringArgumentType.string()), context -> {
                        var argument = StringArgumentType.getString(context, provider.getArgumentName());
                        return new SkinProviderContext(name, argument, null);
                    }));
        }

        return action;
    }

    private static ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> buildSetSubcommand(
            String name, Supplier<SkinProviderContext> supplier) {
        return buildSetSubcommandArgument(literal(name), context -> supplier.get());
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>>
            ArgumentBuilder<CommandSourceStack, T> buildSetSubcommandArgument(
                    ArgumentBuilder<CommandSourceStack, T> argument,
                    Function<CommandContext<CommandSourceStack>, SkinProviderContext> provider) {
        return argument.executes(context -> setSubcommand(context.getSource(), provider.apply(context)))
                .then(makeTargetsArgument((context, targets) ->
                        setSubcommand(context.getSource(), targets, provider.apply(context), true)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> makeTargetsArgument(
            BiFunction<CommandContext<CommandSourceStack>, Collection<GameProfile>, Integer> consumer
    ) {
        return argument("targets", GameProfileArgument.gameProfile())
                .requires(source -> source.hasPermission(2))
                .executes(context -> consumer.apply(context, GameProfileArgument.getGameProfiles(context, "targets")));
    }
}
