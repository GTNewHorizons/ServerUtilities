package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.invsee.GuiInvseeContainer;
import serverutils.invsee.inventories.InvSeeInventories;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.StringUtils;

public class MessageInvseeContainer extends MessageToClient {

    private String playerName;
    private String playerUUID;
    private Collection<InvSeeInventories> foundInventories;
    private int windowId;

    public MessageInvseeContainer() {}

    public MessageInvseeContainer(ForgePlayer player, Collection<InvSeeInventories> foundInventories, int windowId) {
        this.playerName = player.getName();
        this.playerUUID = StringUtils.fromUUID(player.getProfile().getId());
        this.foundInventories = foundInventories;
        this.windowId = windowId;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(windowId);
        data.writeString(playerName);
        data.writeString(playerUUID);
        data.writeVarInt(foundInventories.size());
        for (InvSeeInventories inv : foundInventories) {
            data.writeVarInt(inv.ordinal());
        }
    }

    @Override
    public void readData(DataIn data) {
        windowId = data.readVarInt();
        playerName = data.readString();
        playerUUID = data.readString();
        int size = data.readVarInt();
        foundInventories = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            InvSeeInventories inv = InvSeeInventories.VALUES[data.readVarInt()];
            foundInventories.add(inv);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onMessage() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Map<InvSeeInventories, IInventory> inventories = new LinkedHashMap<>();
        for (InvSeeInventories inv : foundInventories) {
            IInventory inventory = inv.getInventory().createInventory(player);
            inventories.put(inv, inventory);
        }

        new GuiInvseeContainer(inventories, playerName, playerUUID).openGui();
        player.openContainer.windowId = windowId;
    }
}
