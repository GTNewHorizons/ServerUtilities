package serverutils.net;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.gui.misc.GuiPlayerInfoWrapper;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.ranks.Ranks;

public class MessageUpdateTabName extends MessageToClient {

    private String name;
    private IChatComponent displayComponent;
    private boolean afk;

    public MessageUpdateTabName() {}

    public MessageUpdateTabName(ForgePlayer player) {
        EntityPlayerMP playerMP = player.getPlayer();
        name = playerMP.getCommandSenderName();
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(player);
        afk = data.afkTime >= ServerUtilitiesConfig.afk.getNotificationTimer();
        if (Ranks.isActive()) {
            displayComponent = data.getNameForChat(playerMP);
        } else {
            displayComponent = new ChatComponentText(playerMP.getDisplayName());
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(name);
        data.writeTextComponent(displayComponent);
        data.writeBoolean(afk);
    }

    @Override
    public void readData(DataIn data) {
        name = data.readString();
        displayComponent = data.readTextComponent();
        afk = data.readBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer()) return;
        NetHandlerPlayClient handler = mc.getNetHandler();
        // noinspection unchecked
        Map<String, GuiPlayerInfo> infoMap = (Map<String, GuiPlayerInfo>) handler.playerInfoMap;
        List<GuiPlayerInfo> infoList = handler.playerInfoList;
        GuiPlayerInfo info = infoMap.get(name);
        if (info == null) return;

        String displayName = displayComponent.getFormattedText().replaceAll("[<>]", "");

        if (afk) {
            displayName = EnumChatFormatting.GRAY + "[AFK] " + EnumChatFormatting.RESET + displayName;
        }

        if (info instanceof GuiPlayerInfoWrapper wrapper) {
            wrapper.displayName = displayName;
        } else {
            infoMap.remove(name);
            infoList.remove(info);
            GuiPlayerInfoWrapper newInfo = new GuiPlayerInfoWrapper(info, displayName);
            infoMap.put(name, newInfo);
            infoList.add(newInfo);
        }
    }
}
