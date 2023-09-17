package serverutils.lib.mod.net;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.ByteCount;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.config.ConfigGroup;
import serverutils.lib.api.config.ServerConfigProvider;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;
import serverutils.lib.mod.client.gui.GuiEditConfig;

public class MessageEditConfig extends MessageLM // MessageEditConfigResponse
{

    public MessageEditConfig() {
        super(ByteCount.INT);
    }

    public MessageEditConfig(long t, boolean reload, ConfigGroup group) {
        this();
        io.writeLong(t);
        io.writeUTF(group.getID());
        io.writeBoolean(reload);

        NBTTagCompound tag = new NBTTagCompound();
        group.writeToNBT(tag, true);
        writeTag(tag);

        if (ServerUtilitiesLib.DEV_ENV)
            ServerUtilitiesLib.dev_logger.info("TX Send: " + group.getSerializableElement());
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesLibraryLibNetHandler.NET;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        long token = io.readLong();
        String id = io.readUTF();
        boolean reload = io.readBoolean();

        ConfigGroup group = new ConfigGroup(id);
        group.readFromNBT(readTag(), true);

        if (ServerUtilitiesLib.DEV_ENV)
            ServerUtilitiesLib.dev_logger.info("RX Send: " + group.getSerializableElement());

        ServerUtilitiesLibraryClient.openGui(
                new GuiEditConfig(
                        ServerUtilitiesLibraryClient.mc.currentScreen,
                        new ServerConfigProvider(token, reload, group)));
        return null;
    }
}
