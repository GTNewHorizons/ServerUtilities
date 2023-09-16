package ftb.lib.mod.cmd;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.api.cmd.CommandLM;
import ftb.lib.api.cmd.CommandLevel;
import ftb.lib.mod.client.FTBLibModClient;
import ftb.lib.mod.net.MessageReload;

@SideOnly(Side.CLIENT)
public class CmdReloadClient extends CommandLM {

    public CmdReloadClient() {
        super(FTBLibModClient.reload_client_cmd.getAsString(), CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) {
        MessageReload.reloadClient(0L, true);
        return null;
    }
}
