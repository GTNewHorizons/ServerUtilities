package serverutils.lib.mod.cmd;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.lib.mod.client.ServerUtilitiesLibraryModClient;
import serverutils.lib.mod.net.MessageReload;

@SideOnly(Side.CLIENT)
public class CmdReloadClient extends CommandLM {

    public CmdReloadClient() {
        super(ServerUtilitiesLibraryModClient.reload_client_cmd.getAsString(), CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) {
        MessageReload.reloadClient(0L, true);
        return null;
    }
}
