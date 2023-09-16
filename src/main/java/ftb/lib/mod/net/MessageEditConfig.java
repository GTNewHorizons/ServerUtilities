package ftb.lib.mod.net;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.*;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.FTBLib;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.config.ConfigGroup;
import ftb.lib.api.config.ServerConfigProvider;
import ftb.lib.api.net.LMNetworkWrapper;
import ftb.lib.api.net.MessageLM;
import ftb.lib.mod.client.gui.GuiEditConfig;
import latmod.lib.ByteCount;

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

        if (FTBLib.DEV_ENV) FTBLib.dev_logger.info("TX Send: " + group.getSerializableElement());
    }

    public LMNetworkWrapper getWrapper() {
        return FTBLibNetHandler.NET;
    }

    @SideOnly(Side.CLIENT)
    public IMessage onMessage(MessageContext ctx) {
        long token = io.readLong();
        String id = io.readUTF();
        boolean reload = io.readBoolean();

        ConfigGroup group = new ConfigGroup(id);
        group.readFromNBT(readTag(), true);

        if (FTBLib.DEV_ENV) FTBLib.dev_logger.info("RX Send: " + group.getSerializableElement());

        FTBLibClient.openGui(
                new GuiEditConfig(FTBLibClient.mc.currentScreen, new ServerConfigProvider(token, reload, group)));
        return null;
    }
}
