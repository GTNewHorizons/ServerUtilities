package serverutils.command.ranks;

import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;

public class CmdRanks extends CmdTreeBase {

    public CmdRanks() {
        super("ranks");
        addSubcommand(new CmdInfo());
        addSubcommand(new CmdCreate());
        addSubcommand(new CmdDelete());
        addSubcommand(new CmdAdd());
        addSubcommand(new CmdRemove());
        addSubcommand(new CmdGetPermission());
        addSubcommand(new CmdSetPermission());
        addSubcommand(new CmdTreeHelp(this));
    }
}
