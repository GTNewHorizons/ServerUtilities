package serverutils.net;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.gui.GuiEditNBT;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageEditNBT extends MessageToClient {

    private NBTTagCompound info, mainNbt;

    public MessageEditNBT() {}

    public MessageEditNBT(NBTTagCompound i, NBTTagCompound nbt) {
        info = i;
        mainNbt = nbt;

        if (ServerUtilitiesConfig.debugging.log_config_editing) {
            ServerUtilities.LOGGER.info("Editing NBT: " + mainNbt);
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeNBT(info);
        data.writeNBT(mainNbt);
    }

    @Override
    public void readData(DataIn data) {
        info = data.readNBT();
        mainNbt = data.readNBT();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiEditNBT(info, mainNbt).openGui();
    }
}
