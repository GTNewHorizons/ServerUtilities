package serverutils.command.pausewhenempty;

import serverutils.lib.command.CmdTreeBase;

public class CmdPauseWhenEmpty extends CmdTreeBase {

    public CmdPauseWhenEmpty() {
        super("pause_when_empty");
        addSubcommand(new CmdPauseWhenEmptySet());
        addSubcommand(new CmdPauseWhenEmptyMask());
    }
}
