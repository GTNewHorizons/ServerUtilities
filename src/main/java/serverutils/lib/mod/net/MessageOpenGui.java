package serverutils.lib.mod.net;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.LMGuiHandler;
import serverutils.lib.api.gui.LMGuiHandlerRegistry;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;

public class MessageOpenGui extends MessageLM {

    public MessageOpenGui() {
        super(ByteCount.INT);
    }

    public MessageOpenGui(String mod, int id, NBTTagCompound tag, int wid) {
        this();
        io.writeUTF(mod);
        io.writeInt(id);
        writeTag(tag);
        io.writeByte(wid);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesLibraryLibNetHandler.NET_GUI;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        String modID = io.readUTF();
        int guiID = io.readInt();
        NBTTagCompound data = readTag();
        int windowID = io.readUnsignedByte();

        LMGuiHandler h = LMGuiHandlerRegistry.get(modID);
        if (h != null && ServerUtilitiesLibraryMod.proxy
                .openClientGui(ServerUtilitiesLibraryClient.mc.thePlayer, modID, guiID, data))
            ServerUtilitiesLibraryClient.mc.thePlayer.openContainer.windowId = windowID;
        return null;
    }
}
