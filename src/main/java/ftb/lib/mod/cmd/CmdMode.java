package ftb.lib.mod.cmd;

import net.minecraft.command.*;
import net.minecraft.util.*;

import ftb.lib.FTBLib;
import ftb.lib.FTBWorld;
import ftb.lib.api.GameModes;
import ftb.lib.api.cmd.CommandLM;
import ftb.lib.api.cmd.CommandLevel;
import ftb.lib.api.cmd.CommandSubLM;
import ftb.lib.mod.FTBLibMod;
import latmod.lib.LMListUtils;

public class CmdMode extends CommandSubLM {

    public CmdMode() {
        super("ftb_mode", CommandLevel.OP);
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

            int i = FTBWorld.server.setMode(args[0]);

            if (i == 1) {
                c = FTBLibMod.mod.chatComponent("gamemode.not_found");
                c.getChatStyle().setColor(EnumChatFormatting.RED);
            } else if (i == 2) {
                c = FTBLibMod.mod.chatComponent("gamemode.already_set");
                c.getChatStyle().setColor(EnumChatFormatting.RED);
            } else {
                c = FTBLibMod.mod.chatComponent("gamemode.loaded", args[0]);
                c.getChatStyle().setColor(EnumChatFormatting.GREEN);
                FTBLib.reload(ics, true, true);
            }

            return c;
        }
    }

    public static class CmdGet extends CommandLM {

        public CmdGet(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            IChatComponent c = FTBLibMod.mod.chatComponent("gamemode.current", FTBWorld.server.getMode());
            c.getChatStyle().setColor(EnumChatFormatting.AQUA);
            return c;
        }
    }

    public static class CmdList extends CommandLM {

        public CmdList(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            IChatComponent c = FTBLibMod.mod.chatComponent(
                    "gamemode.list",
                    joinNiceStringFromCollection(GameModes.getGameModes().modes.keySet()));
            c.getChatStyle().setColor(EnumChatFormatting.AQUA);
            return c;
        }
    }
}
