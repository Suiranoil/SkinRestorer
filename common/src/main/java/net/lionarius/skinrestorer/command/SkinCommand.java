package net.lionarius.skinrestorer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinService;
import net.lionarius.skinrestorer.skin.SkinTarget;
import net.lionarius.skinrestorer.skin.SkinValue;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;
import net.lionarius.skinrestorer.translation.Translation;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.util.StringUtil;

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
                .requires(src -> SkinCommand.canUseSkinCommand(src) || SkinCommand.canUseConfigCommand(src))
                .then(buildSetSubcommand("clear", SkinValue.EMPTY::toProviderContext)
                        .requires(SkinCommand::canUseSkinCommand))
                .then(withTargets(
                        literal("reset")
                                .requires(SkinCommand::canUseSkinCommand)
                                .executes(context -> resetSubcommand(context.getSource())),
                        (context, targets) -> resetSubcommand(context.getSource(), targets, true)))
                .then(literal("refresh")
                        .requires(SkinCommand::canUseSkinCommand)
                        .executes(context -> refreshSubcommand(context.getSource())));

        var set = literal("set").requires(SkinCommand::canUseSkinCommand);

        var providers = SkinRestorer.getProvidersRegistry().getPublicProviders();
        for (var entry : providers) set.then(buildSetSubcommand(entry.first(), entry.second()));
        if (!providers.isEmpty()) base.then(set);

        base.then(literal("config")
                .requires(SkinCommand::canUseConfigCommand)
                .then(literal("reload").executes(SkinCommand::configReloadSubcommand)));

        dispatcher.register(base);
    }

    private static boolean canUseSkinCommand(CommandSourceStack src) {
        var config = SkinRestorer.getConfig().command();

        return config.enabled() && SkinCommand.hasPermissionLevel(src, config.permissionLevel());
    }

    private static boolean canUseConfigCommand(CommandSourceStack src) {
        return Commands.LEVEL_OWNERS.check(src.permissions());
    }

    private static boolean canUseTargets(CommandSourceStack src) {
        return SkinCommand.hasPermissionLevel(
                src, SkinRestorer.getConfig().command().targetsPermissionLevel());
    }

    private static boolean hasPermissionLevel(CommandSourceStack src, int level) {
        var check =
                switch (level) {
                    case 0 -> Commands.LEVEL_ALL;
                    case 1 -> Commands.LEVEL_MODERATORS;
                    case 2 -> Commands.LEVEL_GAMEMASTERS;
                    case 3 -> Commands.LEVEL_ADMINS;
                    default -> Commands.LEVEL_OWNERS;
                };

        return check.check(src.permissions());
    }

    private static int refreshSubcommand(CommandSourceStack src) {
        var player = src.getPlayer();
        if (player == null) return 0;

        var profile = player.getGameProfile();

        SkinProviderContext context = null;
        var save = true;
        if (!SkinRestorer.getSkinStorage().hasSavedSkin(profile.id())) {
            if (profile.properties().containsKey(PlayerUtils.TEXTURES_KEY)) {
                save = false;
                context = MojangSkinProvider.skinProviderContextFromProfile(profile);
            }
        } else {
            context = SkinRestorer.getSkinStorage().getSkin(profile.id()).toProviderContext();
        }

        if (context == null) return 0;

        return SkinCommand.setSubcommand(src, Collections.singleton(SkinTarget.of(player)), context, save, false);
    }

    private static int resetSubcommand(CommandSourceStack src, Collection<SkinTarget> targets, boolean setByOperator) {
        var storage = SkinRestorer.getSkinStorage();
        var updatedTargets = new HashSet<SkinTarget>();

        for (var target : targets) {
            if (!storage.hasSavedSkin(target.id())) continue;

            var skin = storage.getSkin(target.id()).replaceValueWithOriginal();
            SkinService.applySkin(src.getServer(), Collections.singleton(target), skin, false);

            storage.deleteSkin(target.id());
            updatedTargets.add(target);
        }

        SkinCommand.sendResponse(
                src,
                updatedTargets,
                setByOperator,
                Translation.COMMAND_SKIN_RESTORED_PLAYERS_KEY,
                Translation.COMMAND_SKIN_CLEARED_FOR_OFFLINE_KEY);

        return targets.size();
    }

    private static int resetSubcommand(CommandSourceStack src) {
        var player = src.getPlayer();
        if (player == null) return 0;

        return resetSubcommand(src, Collections.singleton(SkinTarget.of(player)), false);
    }

    private static int setSubcommand(
            CommandSourceStack src,
            Collection<SkinTarget> targets,
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

            SkinCommand.sendResponse(
                    src,
                    result.getSuccessValue(),
                    setByOperator,
                    Translation.COMMAND_SKIN_AFFECTED_PLAYERS_KEY,
                    Translation.COMMAND_SKIN_SAVED_FOR_OFFLINE_KEY);
        });

        return targets.size();
    }

    private static int setSubcommand(
            CommandSourceStack src,
            Collection<SkinTarget> targets,
            SkinProviderContext context,
            boolean setByOperator) {
        return SkinCommand.setSubcommand(src, targets, context, true, setByOperator);
    }

    private static int setSubcommand(CommandSourceStack src, SkinProviderContext context) {
        var player = src.getPlayer();
        if (player == null) return 0;

        return setSubcommand(src, Collections.singleton(SkinTarget.of(player)), context, false);
    }

    private static int configReloadSubcommand(CommandContext<CommandSourceStack> context) {
        SkinRestorer.reloadConfig();

        var server = context.getSource().getServer();
        for (var player : server.getPlayerList().getPlayers())
            server.getCommands().sendCommands(player);

        context.getSource()
                .sendSuccess(
                        () -> Translation.translatableWithFallback(Translation.COMMAND_SKIN_CONFIG_RELOADED_KEY), true);

        return 0;
    }

    private static void sendResponse(
            CommandSourceStack src,
            Collection<SkinTarget> updatedTargets,
            boolean setByOperator,
            String onlineKey,
            String offlineKey) {
        if (updatedTargets.isEmpty()) {
            src.sendSuccess(() -> Translation.translatableWithFallback(Translation.COMMAND_SKIN_NO_CHANGES_KEY), true);
            return;
        }

        if (!setByOperator) {
            src.sendSuccess(() -> Translation.translatableWithFallback(Translation.COMMAND_SKIN_OK_KEY), true);
            return;
        }

        var playerList = src.getServer().getPlayerList();

        var key = updatedTargets.stream().anyMatch(target -> playerList.getPlayer(target.id()) != null)
                ? onlineKey
                : offlineKey;
        var targetsComponent = PlayerUtils.createTargetListComponent(src.getServer(), updatedTargets);

        src.sendSuccess(() -> Translation.translatableWithFallback(key, targetsComponent), true);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSetSubcommand(String name, SkinProvider provider) {
        var action = literal(name);

        if (provider.hasVariantSupport()) {
            for (SkinVariant variant : SkinVariant.values()) {
                action.then(literal(variant.toString())
                        .then(buildSetSubcommandArgument(makeProviderArgument(provider), context -> {
                            var argument = StringArgumentType.getString(context, provider.getArgumentName());
                            return new SkinProviderContext(name, argument, variant);
                        })));
            }
        } else {
            action.then(buildSetSubcommandArgument(makeProviderArgument(provider), context -> {
                var argument = StringArgumentType.getString(context, provider.getArgumentName());
                return new SkinProviderContext(name, argument, null);
            }));
        }

        return action;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildSetSubcommand(
            String name, Supplier<SkinProviderContext> supplier) {
        return buildSetSubcommandArgument(literal(name), context -> supplier.get());
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T buildSetSubcommandArgument(
            T argument, Function<CommandContext<CommandSourceStack>, SkinProviderContext> provider) {
        return withTargets(
                argument.executes(context -> setSubcommand(context.getSource(), provider.apply(context))),
                (context, targets) -> setSubcommand(context.getSource(), targets, provider.apply(context), true));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> makeProviderArgument(SkinProvider provider) {
        return argument(provider.getArgumentName(), StringArgumentType.string())
                .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(provider.getArgumentSuggestions(), builder));
    }

    // uuid is registered first on purpose: a uuid-shaped token parses under both children and
    // brigadier's stable sort of equally good candidates then falls back to insertion order
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T withTargets(
            T parent, BiFunction<CommandContext<CommandSourceStack>, Collection<SkinTarget>, Integer> consumer) {
        return parent.then(argument("uuid", UuidArgument.uuid())
                        .requires(SkinCommand::canUseTargets)
                        .executes(context -> {
                            var id = UuidArgument.getUuid(context, "uuid");
                            var typed = SkinCommand.argumentText(context, "uuid");

                            // UuidArgument delegates to UUID.fromString, which also accepts short
                            // forms such as "a-b-c-d-e". Those are valid player names in offline
                            // mode, so only let canonical UUID text take this branch.
                            if (typed != null && !id.toString().equalsIgnoreCase(typed))
                                return consumer.apply(context, SkinCommand.resolveNameTarget(context, typed));

                            return consumer.apply(context, Collections.singleton(SkinTarget.of(id)));
                        }))
                .then(argument("targets", GameProfileArgument.gameProfile())
                        .requires(SkinCommand::canUseTargets)
                        .executes(context -> consumer.apply(context, resolveTargets(context))));
    }

    private static Collection<SkinTarget> resolveTargets(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        var server = context.getSource().getServer();
        var typed = SkinCommand.argumentText(context, "targets");

        // vanilla resolves a name through usercache and then the Mojang API regardless of the
        // server's mode, so on an offline-mode server it lands on an id the player will never
        // log in as; derive it the way the login path does instead
        if (typed != null && !typed.startsWith("@") && !server.usesAuthentication())
            return SkinCommand.resolveNameTarget(context, typed);

        return GameProfileArgument.getGameProfiles(context, "targets").stream()
                .map(SkinTarget::of)
                .toList();
    }

    private static Collection<SkinTarget> resolveNameTarget(CommandContext<CommandSourceStack> context, String typed)
            throws CommandSyntaxException {
        if (!StringUtil.isValidPlayerName(typed)) throw GameProfileArgument.ERROR_UNKNOWN_PLAYER.create();

        var server = context.getSource().getServer();
        if (!server.usesAuthentication()) return Collections.singleton(SkinTarget.byOfflineName(server, typed));

        return Collections.singleton(server.services()
                .nameToIdCache()
                .get(typed)
                .map(SkinTarget::of)
                .orElseThrow(GameProfileArgument.ERROR_UNKNOWN_PLAYER::create));
    }

    private static String argumentText(CommandContext<CommandSourceStack> context, String name) {
        for (var node : context.getNodes()) {
            if (!node.getNode().getName().equals(name)) continue;

            return node.getRange().get(context.getInput());
        }

        return null;
    }
}
