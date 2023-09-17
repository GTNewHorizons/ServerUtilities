package serverutils.lib.mod.cmd;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMListUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilsWorld;
import serverutils.lib.api.GameModes;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.api.cmd.CommandSubLM;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;

public class CmdMode extends CommandSubLM {

    public CmdMode() {
        super("server_mode", CommandLevel.OP);
        add(new CmdSet("set"));
        add(new CmdGet("get"));
        add(new CmdList("list"));
    }

    public static class CmdSet extends CommandLM {

        public CmdSet(String s) {
            super(s, CommandLevel.OP);
        }

        public String getCommandUsage(ICommandSender ics) {
            return '/' + commandName + " <modeID>";
        }

        public String[] getTabStrings(ICommandSender ics, String[] args, int i) throws CommandException {
            if (args.length == 1) return LMListUtils.toStringArray(GameModes.getGameModes().modes.keySet());
            return super.getTabStrings(ics, args, i);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            if (args.length == 0) return new ChatComponentText(getCommandUsage(ics));

            IChatComponent c;

            int i = ServerUtilsWorld.server.setMode(args[0]);

            if (i == 1) {
                c = ServerUtilitiesLibraryMod.mod.chatComponent("gamemode.not_found");
                c.getChatStyle().setColor(EnumChatFormatting.RED);
            } else if (i == 2) {
                c = ServerUtilitiesLibraryMod.mod.chatComponent("gamemode.already_set");
                c.getChatStyle().setColor(EnumChatFormatting.RED);
            } else {
                c = ServerUtilitiesLibraryMod.mod.chatComponent("gamemode.loaded", args[0]);
                c.getChatStyle().setColor(EnumChatFormatting.GREEN);
                ServerUtilitiesLib.reload(ics, true, true);
            }

            return c;
        }
    }

    public static class CmdGet extends CommandLM {

        public CmdGet(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            IChatComponent c = ServerUtilitiesLibraryMod.mod
                    .chatComponent("gamemode.current", ServerUtilsWorld.server.getMode());
            c.getChatStyle().setColor(EnumChatFormatting.AQUA);
            return c;
        }
    }

    public static class CmdList extends CommandLM {

        public CmdList(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            IChatComponent c = ServerUtilitiesLibraryMod.mod.chatComponent(
                    "gamemode.list",
                    joinNiceStringFromCollection(GameModes.getGameModes().modes.keySet()));
            c.getChatStyle().setColor(EnumChatFormatting.AQUA);
            return c;
        }
    }
}
