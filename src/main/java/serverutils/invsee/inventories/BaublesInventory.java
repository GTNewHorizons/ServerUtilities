package serverutils.invsee.inventories;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.container.InventoryBaubles;
import baubles.common.container.SlotBauble;
import baubles.common.lib.PlayerHandler;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.ImageIcon;
import serverutils.lib.icon.PartIcon;
import serverutils.lib.util.CommonUtils;
import serverutils.lib.util.NBTUtils;

public class BaublesInventory implements IModdedInventory {

    private static final boolean isExpanded = CommonUtils.getClassExists("baubles.api.expanded.BaubleExpandedSlots");
    private static final Icon BAUBLES_ICON;
    private static final Method DYNAMIC_SLOT_TYPE = findDynamicSlotType();
    private static final Method WRITE_CONTAINER_DATA = findContainerMethod("writeContainerData");
    private static final Method READ_CONTAINER_DATA = findContainerMethod("readContainerData", NBTTagCompound.class);
    private final Map<IInventory, String[]> clientSlotTypes = new WeakHashMap<>();
    private final Set<IInventory> offlineInventories = Collections.newSetFromMap(new WeakHashMap<>());

    static {
        ImageIcon icon;
        if (isExpanded) {
            icon = new ImageIcon(new ResourceLocation("baubles", "textures/gui/bauble_inventory.png"));
        } else {
            icon = new ImageIcon(new ResourceLocation("baubles", "textures/gui/expanded_inventory.png"));
        }

        BAUBLES_ICON = new PartIcon(icon, 210, 48, 10, 10, 4) {

            @Override
            public void draw(int x, int y, int w, int h) {
                // this icon does not look good when stretched to 16x16
                super.draw(x + 3, y + 3, 10, 10);
            }
        };
    }

    @Override
    public @Nullable IInventory loadOnlineInventory(EntityPlayerMP player) {
        return PlayerHandler.getPlayerBaubles(player);
    }

    @Override
    public @Nullable IInventory loadOfflineInventory(ForgePlayer player) {
        NBTTagCompound playerData = player.getPlayerNBT();
        NBTTagCompound data;
        if (playerData != null && playerData.hasKey("Baubles", 10)) {
            data = playerData.getCompoundTag("Baubles");
        } else {
            File baublesFile = getBaublesFile(player);
            if (baublesFile == null) return null;
            data = NBTUtils.readNBT(baublesFile);
        }
        if (data == null || data.hasNoTags()) return null;

        InventoryBaubles baubles = new InventoryBaubles(null) {

            @Override
            public void syncSlotToClients(int slot) {}
        };
        baubles.blockEvents = true;
        baubles.readNBT(data);
        offlineInventories.add(baubles);
        return baubles;
    }

    @Override
    public @NotNull IInventory createInventory(EntityPlayer player, int size) {
        InventoryBaubles inventory = new InventoryBaubles(player);
        inventory.stackList = new ItemStack[size];
        inventory.blockEvents = true;
        return inventory;
    }

    @Override
    public NBTTagCompound getContainerData(IInventory inventory) {
        if (WRITE_CONTAINER_DATA != null && READ_CONTAINER_DATA != null) {
            return (NBTTagCompound) invokeContainerMethod(WRITE_CONTAINER_DATA, inventory);
        }
        NBTTagCompound data = new NBTTagCompound();
        if (isExpanded) {
            NBTTagList types = new NBTTagList();
            for (int index = 0; index < inventory.getSizeInventory(); index++) {
                types.appendTag(new NBTTagString(getSlotType(inventory, index)));
            }
            data.setTag("SlotTypes", types);
        }
        return data;
    }

    @Override
    public void readContainerData(IInventory inventory, NBTTagCompound data) {
        if (WRITE_CONTAINER_DATA != null && READ_CONTAINER_DATA != null) {
            invokeContainerMethod(READ_CONTAINER_DATA, inventory, data);
            return;
        }
        if (!isExpanded) return;
        NBTTagList types = data.getTagList("SlotTypes", 8);
        if (types.tagCount() != inventory.getSizeInventory()) {
            throw new IllegalArgumentException("Baubles container layout has the wrong size");
        }
        String[] assignments = new String[types.tagCount()];
        for (int index = 0; index < assignments.length; index++) {
            assignments[index] = types.getStringTagAt(index);
        }
        clientSlotTypes.put(inventory, assignments);
    }

    @Override
    public Object getLayoutIdentity(IInventory inventory) {
        InventoryBaubles baubles = (InventoryBaubles) inventory;
        EntityPlayer owner = baubles.player.get();
        if (!offlineInventories.contains(inventory) && owner instanceof EntityPlayerMP && !owner.worldObj.isRemote) {
            if (MinecraftServer.getServer().getConfigurationManager().func_152612_a(owner.getCommandSenderName())
                    != owner || PlayerHandler.getPlayerBaubles(owner) != baubles)
                return null;
        }
        return baubles.stackList;
    }

    private static Method findDynamicSlotType() {
        try {
            return InventoryBaubles.class.getMethod("getSlotType", int.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findContainerMethod(String name, Class<?>... parameters) {
        try {
            return InventoryBaubles.class.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object invokeContainerMethod(Method method, IInventory inventory, Object... arguments) {
        try {
            return method.invoke(inventory, arguments);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot synchronize Baubles container rules", e);
        }
    }

    private String getSlotType(IInventory inventory, int index) {
        String[] types = clientSlotTypes.get(inventory);
        if (types != null) return types[index];
        if (DYNAMIC_SLOT_TYPE != null) {
            try {
                return (String) DYNAMIC_SLOT_TYPE.invoke(inventory, index);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read the current Baubles layout", e);
            }
        }
        return BaubleExpandedSlots.getSlotType(index);
    }

    @Override
    public @NotNull Icon getButtonIcon() {
        return BAUBLES_ICON;
    }

    @Override
    public @NotNull String getButtonText() {
        return StatCollector.translateToLocal("serverutilities.invsee.baubles");
    }

    @Override
    public void saveInventory(ForgePlayer player, IInventory inventory) {
        if (!player.isOnline()) {
            InventoryBaubles baubles = (InventoryBaubles) inventory;
            NBTTagCompound data = new NBTTagCompound();
            baubles.saveNBT(data);
            NBTTagCompound playerData = player.getPlayerNBT();
            if (playerData != null && playerData.hasKey("Baubles", 10)) {
                playerData.setTag("Baubles", data);
                player.setPlayerNBT(playerData);
            }
            File baublesFile = getBaublesFile(player);
            if (baublesFile != null) {
                NBTUtils.writeNBTSafe(baublesFile, data);
            }
        }
    }

    @Override
    public @Nullable Slot getSlot(EntityPlayer player, IInventory inventory, int index, int x, int y) {
        if (offlineInventories.contains(inventory)) {
            // Evaluate offline edits using the editor, without applying equipment effects or sending owner packets.
            ((InventoryBaubles) inventory).player = new WeakReference<>(player);
        }
        if (isExpanded) {
            String slotType = getSlotType(inventory, index);
            if (slotType.equals(BaubleExpandedSlots.unknownType)) return null;
            return new SlotBauble(inventory, slotType, index, x, y);
        }

        BaubleType type;
        if (index == 0) {
            type = BaubleType.AMULET;
        } else if (index == 1 || index == 2) {
            type = BaubleType.RING;
        } else {
            type = BaubleType.BELT;
        }
        // noinspection deprecation
        return new SlotBauble(inventory, type, index, x, y);
    }

    private @Nullable File getBaublesFile(ForgePlayer player) {
        File baublesFile = new File(
                player.team.universe.getWorldDirectory(),
                "playerdata/" + player.getName() + ".baub");
        if (!baublesFile.exists()) {
            baublesFile = new File(
                    player.team.universe.getWorldDirectory(),
                    "playerdata/" + player.getName() + ".baubback");
        }
        if (!baublesFile.exists()) return null;

        return baublesFile;
    }
}
