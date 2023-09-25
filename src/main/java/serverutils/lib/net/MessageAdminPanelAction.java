package serverutils.lib.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.ServerUtilitiesLibCommon;
import serverutils.lib.lib.data.Action;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToServer;
import serverutils.lib.lib.net.NetworkWrapper;

public class MessageAdminPanelAction extends MessageToServer {

    private ResourceLocation action;

    public MessageAdminPanelAction() {}

    public MessageAdminPanelAction(ResourceLocation id) {
        action = id;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerLibNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeResourceLocation(action);
    }

    @Override
    public void readData(DataIn data) {
        action = data.readResourceLocation();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        Action a = ServerUtilitiesLibCommon.ADMIN_PANEL_ACTIONS.get(action);

        if (a != null) {
            ForgePlayer p = Universe.get().getPlayer(player);
            NBTTagCompound data = new NBTTagCompound();

            if (a.getType(p, data).isEnabled()) {
                a.onAction(p, data);
            }
        }
    }
}
