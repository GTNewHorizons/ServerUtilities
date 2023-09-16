package ftb.lib.mod.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.*;
import ftb.lib.api.item.IClientActionItem;
import ftb.lib.api.net.LMNetworkWrapper;
import ftb.lib.api.net.MessageLM;
import latmod.lib.ByteCount;

public class MessageClientItemAction extends MessageLM {

    public MessageClientItemAction() {
        super(ByteCount.INT);
    }

    public MessageClientItemAction(String s, NBTTagCompound tag) {
        this();
        io.writeUTF(s);
        writeTag(tag);
    }

    public LMNetworkWrapper getWrapper() {
        return FTBLibNetHandler.NET_GUI;
    }

    public IMessage onMessage(MessageContext ctx) {
        String action = io.readUTF();

        EntityPlayerMP ep = ctx.getServerHandler().playerEntity;

        ItemStack is = ep.inventory.mainInventory[ep.inventory.currentItem];

        if (is != null && is.getItem() instanceof IClientActionItem)
            is = ((IClientActionItem) is.getItem()).onClientAction(is, ep, action, readTag());

        if (is != null && is.stackSize <= 0) is = null;

        ep.inventory.mainInventory[ep.inventory.currentItem] = (is == null) ? null : is.copy();
        ep.inventory.markDirty();
        ep.openContainer.detectAndSendChanges();
        return null;
    }
}
