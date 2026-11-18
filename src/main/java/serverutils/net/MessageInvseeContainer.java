package serverutils.net;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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
    private final Map<IModdedInventory, NBTTagCompound> inventoryData = new LinkedHashMap<>();
    private int windowId;
    private IModdedInventory activeInventory;

    public MessageInvseeContainer() {}

    public MessageInvseeContainer(ForgePlayer player, Map<IModdedInventory, IInventory> foundInventories,
            int windowId) {
        this(player, foundInventories, windowId, InvSeeRegistry.getMainInventory());
    }

    public MessageInvseeContainer(ForgePlayer player, Map<IModdedInventory, IInventory> foundInventories, int windowId,
            IModdedInventory activeInventory) {
        this.activeInventory = activeInventory;
        this.playerName = player.getName();
        this.playerUUID = StringUtils.fromUUID(player.getProfile().getId());
        this.windowId = windowId;

        inventoriesWithSize = new Object2IntLinkedOpenHashMap<>(foundInventories.size());
        for (Map.Entry<IModdedInventory, IInventory> entry : foundInventories.entrySet()) {
            inventoriesWithSize.put(entry.getKey(), entry.getValue().getSizeInventory());
            inventoryData.put(entry.getKey(), entry.getKey().getContainerData(entry.getValue()));
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(windowId);
        data.writeVarInt(InvSeeRegistry.getRegisteredInventories().indexOf(activeInventory));
        data.writeString(playerName);
        data.writeString(playerUUID);
        data.writeVarInt(inventoriesWithSize.size());
        for (Object2IntMap.Entry<IModdedInventory> entry : inventoriesWithSize.object2IntEntrySet()) {
            data.writeVarInt(InvSeeRegistry.getRegisteredInventories().indexOf(entry.getKey()));
            data.writeVarInt(entry.getIntValue());
            data.writeNBT(inventoryData.get(entry.getKey()));
        }
    }

    @Override
    public void readData(DataIn data) {
        windowId = data.readVarInt();
        activeInventory = InvSeeRegistry.getRegisteredInventories().get(data.readVarInt());
        playerName = data.readString();
        playerUUID = data.readString();
        int size = data.readVarInt();
        inventoriesWithSize = new Object2IntLinkedOpenHashMap<>(size);
        inventoryData.clear();
        for (int i = 0; i < size; i++) {
            IModdedInventory inv = InvSeeRegistry.getRegisteredInventories().get(data.readVarInt());
            inventoriesWithSize.put(inv, data.readVarInt());
            inventoryData.put(inv, data.readNBT());
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
            inv.readContainerData(inventory, inventoryData.get(inv));
            inventories.put(inv, inventory);
        }

        ItemStack carried = player.inventory.getItemStack();
        player.inventory.setItemStack(null);
        try {
            new GuiInvseeContainer(inventories, playerName, playerUUID, activeInventory).openGui();
            player.openContainer.windowId = windowId;
        } finally {
            player.inventory.setItemStack(carried);
        }
    }
}
