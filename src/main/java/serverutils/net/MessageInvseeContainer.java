package serverutils.net;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import serverutils.invsee.GuiInvseeContainer;
import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeRegistry;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.StringUtils;

public class MessageInvseeContainer extends MessageToClient {

    private String playerName;
    private String playerUUID;
    private Object2IntMap<IModdedInventory> inventoriesWithSize;
    private int windowId;

    public MessageInvseeContainer() {}

    public MessageInvseeContainer(ForgePlayer player, Map<IModdedInventory, IInventory> foundInventories,
            int windowId) {
        this.playerName = player.getName();
        this.playerUUID = StringUtils.fromUUID(player.getProfile().getId());
        this.windowId = windowId;

        inventoriesWithSize = new Object2IntLinkedOpenHashMap<>(foundInventories.size());
        for (Map.Entry<IModdedInventory, IInventory> entry : foundInventories.entrySet()) {
            inventoriesWithSize.put(entry.getKey(), entry.getValue().getSizeInventory());
        }
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
        data.writeVarInt(inventoriesWithSize.size());
        for (Object2IntMap.Entry<IModdedInventory> entry : inventoriesWithSize.object2IntEntrySet()) {
            data.writeVarInt(InvSeeRegistry.getRegisteredInventories().indexOf(entry.getKey()));
            data.writeVarInt(entry.getIntValue());
        }
    }

    @Override
    public void readData(DataIn data) {
        windowId = data.readVarInt();
        playerName = data.readString();
        playerUUID = data.readString();
        int size = data.readVarInt();
        inventoriesWithSize = new Object2IntLinkedOpenHashMap<>(size);
        for (int i = 0; i < size; i++) {
            IModdedInventory inv = InvSeeRegistry.getRegisteredInventories().get(data.readVarInt());
            inventoriesWithSize.put(inv, data.readVarInt());
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onMessage() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Map<IModdedInventory, IInventory> inventories = new LinkedHashMap<>();
        for (Object2IntMap.Entry<IModdedInventory> entry : inventoriesWithSize.object2IntEntrySet()) {
            IModdedInventory inv = entry.getKey();
            IInventory inventory = inv.createInventory(player, entry.getIntValue());
            inventories.put(inv, inventory);
        }

        new GuiInvseeContainer(inventories, playerName, playerUUID).openGui();
        player.openContainer.windowId = windowId;
    }
}
