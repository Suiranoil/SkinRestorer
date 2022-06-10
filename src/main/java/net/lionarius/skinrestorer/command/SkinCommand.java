package net.lionarius.skinrestorer.command;

import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lionarius.skinrestorer.MineskinSkinProvider;
import net.lionarius.skinrestorer.MojangSkinProvider;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.enums.SkinVariant;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SkinCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("skin")
                .then(literal("set")
                        .then(literal("mojang")
                                .then(argument("skin_name", StringArgumentType.word())
                                        .executes(context ->
                                                skinAction(Collections.singleton(context.getSource().getPlayer()), false,
                                                        () -> MojangSkinProvider.getSkin(StringArgumentType.getString(context, "skin_name"))))
                                        .then(argument("targets", EntityArgumentType.players()).requires(source -> source.hasPermissionLevel(3))
                                                .executes(context ->
                                                        skinAction(EntityArgumentType.getPlayers(context, "targets"), true,
                                                                () -> MojangSkinProvider.getSkin(StringArgumentType.getString(context, "skin_name")))))))
                        .then(literal("web")
                                .then(literal("classic")
                                        .then(argument("url", StringArgumentType.string())
                                                .executes(context ->
                                                        skinAction(Collections.singleton(context.getSource().getPlayer()), false,
                                                                () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.CLASSIC)))
                                                .then(argument("targets", EntityArgumentType.players()).requires(source -> source.hasPermissionLevel(3))
                                                        .executes(context ->
                                                                skinAction(EntityArgumentType.getPlayers(context, "targets"), true,
                                                                        () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.CLASSIC))))))
                                .then(literal("slim")
                                        .then(argument("url", StringArgumentType.string())
                                                .executes(context ->
                                                        skinAction(Collections.singleton(context.getSource().getPlayer()), false,
                                                                () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.SLIM)))
                                                .then(argument("targets", EntityArgumentType.players()).requires(source -> source.hasPermissionLevel(3))
                                                        .executes(context ->
                                                                skinAction(EntityArgumentType.getPlayers(context, "targets"), true,
                                                                        () -> MineskinSkinProvider.getSkin(StringArgumentType.getString(context, "url"), SkinVariant.SLIM))))))))
                .then(literal("clear")
                        .executes(context ->
                                skinAction(Collections.singleton(context.getSource().getPlayer()), false,
                                        () -> null))
                        .then(argument("targets", EntityArgumentType.players()).executes(context ->
                                skinAction(EntityArgumentType.getPlayers(context, "targets"), true,
                                        () -> null))))
        );
    }

    private static int skinAction(Collection<ServerPlayerEntity> targets, boolean setByOperator, Supplier<Property> skinSupplier) {
        new Thread(() -> {
            if (!setByOperator)
                targets.stream().findFirst().get().sendMessage(Text.of("§6[SkinRestorer]§f Downloading skin."), true);

            Property skin = skinSupplier.get();

            for (ServerPlayerEntity player : targets) {
                SkinRestorer.getSkinStorage().setSkin(player.getUuid(), skin);

                if (setByOperator)
                    player.sendMessage(Text.of("§a[SkinRestorer]§f Operator changed your skin. You need to reconnect to apply it."), true);
                else
                    player.sendMessage(Text.of("§a[SkinRestorer]§f You need to reconnect to apply skin."), true);
            }
        }).start();

        return targets.size();
    }
}
