package serverutils.net;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.client.tab.TabChannelHandler;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageTabConfig extends MessageToClient {

    private String tabHeaderText;
    private String tabFooterText;
    private boolean tabShowPlayerHeads;
    private boolean tabShowPingNumber;
    private boolean tabShowPingBars;

    public MessageTabConfig() {}

    public MessageTabConfig(String headerText, String footerText, boolean showHeads, boolean showPingNumber,
            boolean showPingBars) {
        this.tabHeaderText = headerText != null ? headerText : "";
        this.tabFooterText = footerText != null ? footerText : "";
        this.tabShowPlayerHeads = showHeads;
        this.tabShowPingNumber = showPingNumber;
        this.tabShowPingBars = showPingBars;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(tabHeaderText);
        data.writeString(tabFooterText);
        data.writeBoolean(tabShowPlayerHeads);
        data.writeBoolean(tabShowPingNumber);
        data.writeBoolean(tabShowPingBars);
    }

    @Override
    public void readData(DataIn data) {
        tabHeaderText = data.readString();
        tabFooterText = data.readString();
        tabShowPlayerHeads = data.readBoolean();
        tabShowPingNumber = data.readBoolean();
        tabShowPingBars = data.readBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        TabChannelHandler.INSTANCE.setServerData(tabHeaderText, tabFooterText);
        ServerUtilitiesClientConfig.tabShowPlayerHeads = tabShowPlayerHeads;
        ServerUtilitiesClientConfig.tabShowPingNumber = tabShowPingNumber;
        ServerUtilitiesClientConfig.tabShowPingBars = tabShowPingBars;
    }
}
