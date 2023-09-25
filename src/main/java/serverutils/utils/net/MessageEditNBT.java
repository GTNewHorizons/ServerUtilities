package serverutils.utils.net;

import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.ServerUtilitiesLibConfig;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.utils.ServerUtilities;
import serverutils.utils.gui.GuiEditNBT;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageEditNBT extends MessageToClient {

    private NBTTagCompound info, mainNbt;

    public MessageEditNBT() {}

    public MessageEditNBT(NBTTagCompound i, NBTTagCompound nbt) {
        info = i;
        mainNbt = nbt;

        if (ServerUtilitiesLibConfig.debugging.log_config_editing) {
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
