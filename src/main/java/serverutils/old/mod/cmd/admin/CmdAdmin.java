package serverutils.old.mod.cmd.admin;

import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.api.cmd.CommandSubLM;
import serverutils.old.mod.config.ServerUtilitiesConfigCmd;

public class CmdAdmin extends CommandSubLM {

    public CmdAdmin() {
        super(ServerUtilitiesConfigCmd.name_admin.getAsString(), CommandLevel.OP);
        // add(new CmdPlayerLM());
        add(new CmdRestart());
        add(new CmdInvsee());
        add(new CmdInvseeEnderchest());
        add(new CmdSetWarp());
        add(new CmdDelWarp());
        add(new CmdUnclaim());
        add(new CmdUnclaimAll());
        add(new CmdBackup());
        add(new CmdListFriends());
        add(new CmdUnloadAll());
        add(new CmdAdminHome());
        add(new CmdWorldBorder());
        add(new CmdServerInfo());
        // add(new CmdResetPos());
    }
}
