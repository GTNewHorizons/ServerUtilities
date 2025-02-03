package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.invsee.InvseeContainer;
import serverutils.invsee.inventories.InvSeeInventories;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageInvseeSwitch extends MessageToServer {

    private InvSeeInventories inventory;

    public MessageInvseeSwitch() {}

    public MessageInvseeSwitch(InvSeeInventories inv) {
        this.inventory = inv;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(inventory.ordinal());
    }

    @Override
    public void readData(DataIn data) {
        inventory = InvSeeInventories.VALUES[data.readVarInt()];
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
