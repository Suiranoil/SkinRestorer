package net.lionarius.skinrestorer.command;

import java.util.List;
import java.util.Locale;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.http.MojangClient;
import net.lionarius.skinrestorer.server.SkinServer;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;

public final class SkinCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "skin";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/skin set mojang <name> [player|uuid] | reset [player|uuid] | refresh [player|uuid] | config reload";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        SkinServer server = SkinRestorer.server();
        if (server == null) {
            SkinServer.say(sender, "SkinRestorer is available on the server only.");
            return;
        }
        if (arguments.length == 0 || arguments[0].equalsIgnoreCase("help")) {
            SkinServer.say(sender, getCommandUsage(sender));
            return;
        }
        boolean operator = sender.canCommandSenderUseCommand(2, "skin");
        String action = arguments[0].toLowerCase(Locale.ROOT);
        try {
            if (action.equals("config")) {
                if (!operator)
                    throw new IllegalArgumentException("Only operators or the console can reload configuration");
                if (arguments.length != 2 || !arguments[1].equalsIgnoreCase("reload"))
                    throw new WrongUsageException(getCommandUsage(sender));
                server.reload(sender, SkinRestorer.configFile());
                return;
            }
            int targetIndex;
            String source = null;
            if (action.equals("set")) {
                if (arguments.length < 3 || arguments.length > 4 || !arguments[1].equalsIgnoreCase("mojang")) {
                    throw new WrongUsageException(getCommandUsage(sender));
                }
                source = MojangClient.validateUsername(arguments[2]);
                targetIndex = 3;
            } else if (action.equals("reset") || action.equals("clear") || action.equals("refresh")) {
                if (arguments.length > 2) throw new WrongUsageException(getCommandUsage(sender));
                targetIndex = 1;
            } else {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            SkinServer.Target target;
            if (arguments.length > targetIndex) {
                if (!operator) throw new IllegalArgumentException("You can only change your own skin");
                target = server.resolve(arguments[targetIndex]);
            } else if (sender instanceof EntityPlayerMP player) {
                target = new SkinServer.Target(player.getUniqueID(), player.getCommandSenderName());
            } else {
                throw new IllegalArgumentException("The console must specify a known player or UUID");
            }
            if (!server.beginCommand(sender, target)) return;
            switch (action) {
                case "set" -> server.set(sender, target, source);
                case "reset", "clear" -> server.reset(sender, target);
                case "refresh" -> server.refresh(sender, target);
                default -> throw new IllegalArgumentException("Unknown skin command");
            }
        } catch (WrongUsageException usage) {
            SkinServer.say(sender, getCommandUsage(sender));
        } catch (IllegalArgumentException error) {
            SkinServer.say(sender, error.getMessage());
        } catch (RuntimeException | LinkageError failure) {
            SkinServer.say(sender, "Skin operation failed; current skin and normal gameplay are preserved.");
            SkinRestorer.LOGGER.warn("Skin command could not complete", failure);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] arguments) {
        if (arguments.length == 1)
            return getListOfStringsMatchingLastWord(arguments, "set", "reset", "refresh", "config", "help");
        if (arguments.length == 2 && arguments[0].equalsIgnoreCase("set")) {
            return getListOfStringsMatchingLastWord(arguments, "mojang");
        }
        if (arguments.length == 2 && arguments[0].equalsIgnoreCase("config")) {
            return sender.canCommandSenderUseCommand(2, "skin") ? getListOfStringsMatchingLastWord(arguments, "reload")
                : null;
        }
        SkinServer server = SkinRestorer.server();
        if (server != null && sender.canCommandSenderUseCommand(2, "skin")
            && ((arguments.length == 4 && arguments[0].equalsIgnoreCase("set"))
                || (arguments.length == 2 && !arguments[0].equalsIgnoreCase("set")))) {
            return getListOfStringsFromIterableMatchingLastWord(arguments, server.knownNames());
        }
        return null;
    }
}
