package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
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

public class MessageUpdateTabName extends MessageToClient {

    private Collection<TabNameEntry> entries = new ArrayList<>();

    public MessageUpdateTabName() {}

    public MessageUpdateTabName(Collection<ForgePlayer> players) {
        for (ForgePlayer player : players) {
            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(player);
            boolean afk = data.afkTime >= ServerUtilitiesConfig.afk.getNotificationTimer();
            entries.add(new TabNameEntry(player.getName(), data.getNameForChat(), afk));
        }
    }

    public MessageUpdateTabName(ForgePlayer player, IChatComponent displayName) {
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(player);
        boolean afk = data.afkTime >= ServerUtilitiesConfig.afk.getNotificationTimer();
        entries.add(new TabNameEntry(player.getName(), displayName, afk));
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(entries, TabNameEntry.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        entries = data.readCollection(TabNameEntry.DESERIALIZER);
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

        for (TabNameEntry entry : entries) {
            String name = entry.name;
            GuiPlayerInfo info = infoMap.get(name);
            if (info == null || entry.displayComponent == null) return;

            String displayName = entry.displayComponent.getFormattedText().replaceAll("[<>]", "");

            if (entry.afk) {
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

    private static class TabNameEntry {

        private final String name;
        private final IChatComponent displayComponent;
        private final boolean afk;

        public TabNameEntry(String name, @Nullable IChatComponent displayComponent, boolean afk) {
            this.name = name;
            this.displayComponent = displayComponent;
            this.afk = afk;
        }

        public static final DataOut.Serializer<TabNameEntry> SERIALIZER = (data, entry) -> {
            data.writeString(entry.name);
            data.writeTextComponent(entry.displayComponent);
            data.writeBoolean(entry.afk);
        };

        public static final DataIn.Deserializer<TabNameEntry> DESERIALIZER = data -> new TabNameEntry(
                data.readString(),
                data.readTextComponent(),
                data.readBoolean());
    }
}
