package serverutils.lib.api.cmd;

import net.minecraft.command.CommandException;

import serverutils.lib.api.ServerUtilitiesLibLang;

/**
 * Created by LatvianModder on 23.02.2016.
 */
public class InvalidSubCommandException extends CommandException {

    private static final long serialVersionUID = 1L;

    public InvalidSubCommandException(String s) {
        super(ServerUtilitiesLibLang.invalid_subcmd.getID(), s);
    }
}
