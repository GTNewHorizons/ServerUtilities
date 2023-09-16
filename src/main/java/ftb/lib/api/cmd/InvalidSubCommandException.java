package ftb.lib.api.cmd;

import net.minecraft.command.CommandException;

import ftb.lib.api.FTBLibLang;

/**
 * Created by LatvianModder on 23.02.2016.
 */
public class InvalidSubCommandException extends CommandException {

    private static final long serialVersionUID = 1L;

    public InvalidSubCommandException(String s) {
        super(FTBLibLang.invalid_subcmd.getID(), s);
    }
}
