package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.invsee.InvseeContainer;
import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeRegistry;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageInvseeSwitch extends MessageToServer {

    private IModdedInventory inventory;

    public MessageInvseeSwitch() {}

    public MessageInvseeSwitch(IModdedInventory inv) {
        this.inventory = inv;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(InvSeeRegistry.getRegisteredInventories().indexOf(inventory));
    }

    @Override
    public void readData(DataIn data) {
        inventory = InvSeeRegistry.getRegisteredInventories().get(data.readVarInt());
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (player.openContainer instanceof InvseeContainer invsee) {
            invsee.setActiveInventory(inventory);
        }
    }
}
