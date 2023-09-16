package ftb.lib.api.cmd;

import net.minecraft.command.CommandException;

import ftb.lib.api.FTBLibLang;

public class FeatureDisabledException extends CommandException {

    private static final long serialVersionUID = 1L;

    public FeatureDisabledException() {
        super(FTBLibLang.feature_disabled.getID(), new Object[0]);
    }
}
