package serverutils.lib.api.cmd;

import net.minecraft.command.CommandException;

import serverutils.lib.api.ServerUtilitiesLibLang;

public class FeatureDisabledException extends CommandException {

    private static final long serialVersionUID = 1L;

    public FeatureDisabledException() {
        super(ServerUtilitiesLibLang.feature_disabled.getID(), new Object[0]);
    }
}
