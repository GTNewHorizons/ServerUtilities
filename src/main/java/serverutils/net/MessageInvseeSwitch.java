package serverutils.net;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.invsee.InvseeContainer;
import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeRegistry;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageInvseeSwitch extends MessageToServer {

    private IModdedInventory inventory;
    private int windowId;

    public MessageInvseeSwitch() {}

    @SideOnly(Side.CLIENT)
    public MessageInvseeSwitch(IModdedInventory inv) {
        this(inv, Minecraft.getMinecraft().thePlayer.openContainer.windowId);
    }

    public MessageInvseeSwitch(IModdedInventory inv, int windowId) {
        this.inventory = inv;
        this.windowId = windowId;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(windowId);
        data.writeVarInt(InvSeeRegistry.getRegisteredInventories().indexOf(inventory));
    }

    @Override
    public void readData(DataIn data) {
        windowId = data.readVarInt();
        int index = data.readVarInt();
        List<IModdedInventory> inventories = InvSeeRegistry.getRegisteredInventories();
        inventory = index >= 0 && index < inventories.size() ? inventories.get(index) : null;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (player.openContainer instanceof InvseeContainer invsee && invsee.windowId == windowId) {
            invsee.switchInventory(inventory);
        }
    }
}
