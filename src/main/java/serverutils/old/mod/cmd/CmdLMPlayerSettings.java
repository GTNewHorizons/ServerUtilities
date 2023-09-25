package serverutils.old.mod.cmd;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import serverutils.lib.PrivacyLevel;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.api.cmd.CommandSubLM;
import serverutils.old.world.LMPlayerServer;
import serverutils.old.world.PersonalSettings;

/**
 * Created by LatvianModder on 14.01.2016.
 */
public class CmdLMPlayerSettings extends CommandSubLM {

    public CmdLMPlayerSettings() {
        super("lmplayer_settings", CommandLevel.ALL);
        add(new CmdSettingBool("chat_links", PersonalSettings.CHAT_LINKS));
        add(new CmdSettingBool("explosions", PersonalSettings.EXPLOSIONS));
        add(new CmdSettingBool("fake_players", PersonalSettings.FAKE_PLAYERS));
        add(new CmdBlockSecurity("block_security"));
        add(new CmdRenderBadge("render_badge"));
    }

    public static class CmdSettingBool extends CommandLM {

        public final byte flag;

        public CmdSettingBool(String s, byte f) {
            super(s, CommandLevel.ALL);
            flag = f;
        }

        public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
            if (i == 0) return new String[] { "true", "false" };
            return null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 1);
            LMPlayerServer p = LMPlayerServer.get(ics);
            boolean b = args[0].equals("toggle") ? !p.getSettings().get(flag) : parseBoolean(ics, args[0]);
            p.getSettings().set(flag, b);
            p.sendUpdate();
            if (!args[0].equals("toggle")) ServerUtilitiesLib.printChat(ics, commandName + " set to " + b);
            return null;
        }
    }

    public static class CmdBlockSecurity extends CommandLM {

        public CmdBlockSecurity(String s) {
            super(s, CommandLevel.ALL);
        }

        public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
            if (i == 0) return PrivacyLevel.getNames();
            return null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 1);
            LMPlayerServer p = LMPlayerServer.get(ics);

            if (args[0].equals("toggle")) {
                p.getSettings().blocks = PrivacyLevel.VALUES_3[(p.getSettings().blocks.ID + 1) % 3];
                p.sendUpdate();
                return null;
            }

            PrivacyLevel l = PrivacyLevel.get(args[0]);
            if (l != null) {
                p.getSettings().blocks = l;
                ServerUtilitiesLib.printChat(ics, commandName + " set to " + l.name().toLowerCase());
            }

            return null;
        }
    }

    public static class CmdRenderBadge extends CommandLM {

        public CmdRenderBadge(String s) {
            super(s, CommandLevel.ALL);
        }

        public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
            if (i == 0) return new String[] { "true", "false" };
            return null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 1);
            LMPlayerServer p = LMPlayerServer.get(ics);
            boolean b = args[0].equals("toggle") ? !p.renderBadge : parseBoolean(ics, args[0]);
            p.renderBadge = b;
            p.sendUpdate();
            if (!args[0].equals("toggle")) ServerUtilitiesLib.printChat(ics, commandName + " set to " + b);
            return null;
        }
    }
}
