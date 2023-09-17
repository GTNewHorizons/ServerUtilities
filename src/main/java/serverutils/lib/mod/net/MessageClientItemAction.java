package serverutils.lib.mod.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import latmod.lib.ByteCount;
import serverutils.lib.api.item.IClientActionItem;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;

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
        return ServerUtilitiesLibraryLibNetHandler.NET_GUI;
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
