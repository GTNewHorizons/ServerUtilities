package serverutils.command.pregen;

import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;

public class CmdPregen extends CmdTreeBase {

    public CmdPregen() {
        super("pregen");
        addSubcommand(new CmdProgress());
        addSubcommand(new CmdStart());
        addSubcommand(new CmdStop());
        addSubcommand(new CmdTreeHelp(this));
    }
}
