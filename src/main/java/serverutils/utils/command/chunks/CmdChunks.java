package serverutils.utils.command.chunks;

import serverutils.lib.lib.command.CmdTreeBase;
import serverutils.lib.lib.command.CmdTreeHelp;

public class CmdChunks extends CmdTreeBase {

    public CmdChunks() {
        super("chunks");
        addSubcommand(new CmdClaim());
        addSubcommand(new CmdUnclaim());
        addSubcommand(new CmdLoad());
        addSubcommand(new CmdUnload());
        addSubcommand(new CmdUnclaimAll());
        addSubcommand(new CmdUnloadAll());
        addSubcommand(new CmdUnclaimEverything());
        addSubcommand(new CmdUnloadEverything());
        addSubcommand(new CmdInfo());
        addSubcommand(new CmdClaimAs());
        addSubcommand(new CmdTreeHelp(this));
    }
}
