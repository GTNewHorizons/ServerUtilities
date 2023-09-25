package serverutils.utils.net;

import net.minecraft.nbt.NBTTagCompound;

import com.feed_the_beast.ftblib.FTBLibConfig;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import serverutils.utils.ServerUtilities;
import serverutils.utils.gui.GuiEditNBT;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageEditNBT extends MessageToClient {

    private NBTTagCompound info, mainNbt;

    public MessageEditNBT() {}

    public MessageEditNBT(NBTTagCompound i, NBTTagCompound nbt) {
        info = i;
        mainNbt = nbt;

        if (FTBLibConfig.debugging.log_config_editing) {
            ServerUtilities.LOGGER.info("Editing NBT: " + mainNbt);
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.FILES;
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
