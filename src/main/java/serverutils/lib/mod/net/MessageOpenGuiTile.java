package serverutils.lib.mod.net;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;
import serverutils.lib.api.tile.IGuiTile;

public class MessageOpenGuiTile extends MessageLM {

    public MessageOpenGuiTile() {
        super(ByteCount.INT);
    }

    public MessageOpenGuiTile(TileEntity t, NBTTagCompound tag, int wid) {
        this();
        io.writeInt(t.xCoord);
        io.writeInt(t.yCoord);
        io.writeInt(t.zCoord);
        writeTag(tag);
        io.writeByte(wid);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesLibraryLibNetHandler.NET_GUI;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        int x = io.readInt();
        int y = io.readInt();
        int z = io.readInt();

        TileEntity te = ServerUtilitiesLibraryClient.mc.theWorld.getTileEntity(x, y, z);

        if (te != null && !te.isInvalid() && te instanceof IGuiTile) {
            GuiScreen gui = ((IGuiTile) te).getGui(ServerUtilitiesLibraryClient.mc.thePlayer, readTag());

            if (gui != null) {
                ServerUtilitiesLibraryClient.openGui(gui);
                ServerUtilitiesLibraryClient.mc.thePlayer.openContainer.windowId = io.readUnsignedByte();
            }
        }

        return null;
    }
}
