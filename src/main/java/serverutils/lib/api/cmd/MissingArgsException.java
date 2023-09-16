package serverutils.lib.api.cmd;

import net.minecraft.command.CommandException;

import serverutils.lib.api.ServerUtilitiesLibLang;

public class MissingArgsException extends CommandException {

    private static final long serialVersionUID = 1L;

    public MissingArgsException() {
        super(ServerUtilitiesLibLang.missing_args.getID(), new Object[0]);
    }
}
