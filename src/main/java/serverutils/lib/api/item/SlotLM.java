package serverutils.lib.api.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotLM extends Slot // ContainerLM
{

    public SlotLM(IInventory inv, int i, int x, int y) {
        super(inv, i, x, y);
    }

    public boolean isItemValid(ItemStack is) {
        return inventory.isItemValidForSlot(getSlotIndex(), is);
    }

    public boolean canTakeStack(EntityPlayer ep) {
        return true;
    }
}
