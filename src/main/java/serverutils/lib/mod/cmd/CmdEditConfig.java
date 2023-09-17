package serverutils.lib.mod.cmd;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMJsonUtils;
import latmod.lib.LMListUtils;
import latmod.lib.LMStringUtils;
import serverutils.lib.LMAccessToken;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.api.config.ConfigEntry;
import serverutils.lib.api.config.ConfigFile;
import serverutils.lib.api.config.ConfigGroup;
import serverutils.lib.api.config.ConfigRegistry;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmdNames;
import serverutils.lib.mod.net.MessageEditConfig;

public class CmdEditConfig extends CommandLM {

    public CmdEditConfig() {
        super(ServerUtilitiesLibConfigCmdNames.edit_config.getAsString(), CommandLevel.OP);
    }

    public String getCommandUsage(ICommandSender ics) {
        return "/" + commandName + " <ID> [group] [entry] [value]";
    }

    public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
        if (i == 0) return LMListUtils.toStringArray(ConfigRegistry.map.keySet());
        else if (i == 1) {
            ConfigFile file = ConfigRegistry.map.get(args[0]);
            if (file != null) return LMListUtils.toStringArray(file.entryMap.keySet());
        } else if (i == 2) {
            ConfigFile file = ConfigRegistry.map.get(args[0]);
            if (file != null) {
                ConfigGroup group = file.getGroup(args[1]);
                if (group != null) return LMListUtils.toStringArray(group.entryMap.keySet());
            }
        }

        return null;
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);

        if (args.length == 1 && ics instanceof EntityPlayerMP) {
            EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
            ConfigFile file = ConfigRegistry.map.get(args[0]);

            if (file == null) return error(new ChatComponentText("Invalid file: '" + args[0] + "'!"));

            new MessageEditConfig(LMAccessToken.generate(ep), true, file).sendTo(ep);
            return null;
        }

        checkArgs(args, 3); // file, group, entry, value...

        ConfigFile file = ConfigRegistry.map.get(args[0]);
        if (file == null) return error(new ChatComponentText("Can only edit files!"));

        boolean success = false;
        ConfigGroup group = file.getGroup(args[1]);
        ConfigEntry entry = (group == null) ? null : group.getEntry(args[2]);

        if (entry == null) return error(
                new ChatComponentText("Can't find config entry '" + args[0] + " " + args[1] + " " + args[2] + "'"));

        if (args.length >= 4) {
            String json = LMStringUtils.unsplitSpaceUntilEnd(3, args);

            ServerUtilitiesLibraryMod.logger.info("Setting " + args[0] + " " + args[1] + " " + args[2] + " to " + json);

            try {
                entry.func_152753_a(LMJsonUtils.fromJson(json));
                file.save();
                ServerUtilitiesLib.reload(ics, true, false);
                return new ChatComponentText(args[2] + " set to " + entry.getAsString());
            } catch (Exception ex) {
                ChatComponentText error = new ChatComponentText(ex.toString());
                error.getChatStyle().setColor(EnumChatFormatting.RED);
                return error;
            }
        }

        return new ChatComponentText(entry.getAsString());
    }
}
