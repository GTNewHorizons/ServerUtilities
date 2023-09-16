package ftb.lib.api.cmd;

import net.minecraft.command.CommandException;

import ftb.lib.api.FTBLibLang;

public class MissingArgsException extends CommandException {

    private static final long serialVersionUID = 1L;

    public MissingArgsException() {
        super(FTBLibLang.missing_args.getID(), new Object[0]);
    }
}
