package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.ServerUtilitiesCommon;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageEditConfigResponse extends MessageToServer {

    private NBTTagCompound nbt;

    public MessageEditConfigResponse() {}

    public MessageEditConfigResponse(NBTTagCompound n) {
        nbt = n;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.EDIT_CONFIG;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeNBT(nbt);
    }

    @Override
    public void readData(DataIn data) {
        nbt = data.readNBT();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        ServerUtilitiesCommon.EditingConfig c = ServerUtilitiesCommon.TEMP_SERVER_CONFIG
                .get(player.getGameProfile().getId());
        // TODO: Logger

        if (c != null) {
            c.group.deserializeNBT(nbt);
            c.callback.onConfigSaved(c.group, player);
            ServerUtilitiesCommon.TEMP_SERVER_CONFIG.remove(player.getUniqueID());
        }
    }
}
