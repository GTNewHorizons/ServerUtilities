package serverutils.utils.net;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import serverutils.utils.ServerUtilitiesConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageUpdateTabName extends MessageToClient {

    private UUID playerId;
    private String name;
    private IChatComponent displayName;
    private boolean afk, rec;

    public MessageUpdateTabName() {}

    public MessageUpdateTabName(EntityPlayerMP player) {
        playerId = player.getUniqueID();
        name = player.getDisplayName();
        displayName = new ChatComponentText(player.getDisplayName());
        afk = (System.currentTimeMillis() - player.func_154331_x()) >= ServerUtilitiesConfig.afk.getNotificationTimer();
        rec = NBTUtils.getPersistedData(player, false).getBoolean("recording");
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeUUID(playerId);
        data.writeString(name);
        data.writeTextComponent(displayName);
        data.writeBoolean(afk);
        data.writeBoolean(rec);
    }

    @Override
    public void readData(DataIn data) {
        playerId = data.readUUID();
        name = data.readString();
        displayName = data.readTextComponent();
        afk = data.readBoolean();
        rec = data.readBoolean();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        // ignore this for now
        return;
        // List<GuiPlayerInfo> list = Minecraft.getMinecraft().thePlayer.sendQueue.playerInfoList;
        // int i;
        // for (i = 0; i < list.size(); i++) {
        // if (name.equals(list.get(i).name)) {
        // break;
        // }
        // }
        //
        // if (i >= list.size()) {
        // return;
        // }
        //
        // IChatComponent component = new ChatComponentText("");
        //
        // if (rec) {
        // IChatComponent component1 = new ChatComponentText("[REC]");
        // component1.getChatStyle().setColor(EnumChatFormatting.RED);
        // component1.getChatStyle().setBold(true);
        // component.appendSibling(component1);
        // }
        //
        // if (afk) {
        // IChatComponent component1 = new ChatComponentText("[AFK]");
        // component1.getChatStyle().setColor(EnumChatFormatting.GRAY);
        // component.appendSibling(component1);
        // }
        //
        // if (afk || rec) {
        // component.appendText(" ");
        // }
        //
        // component.appendSibling(displayName);
        // list.set(i, new GuiPlayerInfo(component.getFormattedText()));

        // This doesn't work because GuiPlayerInfo uses the name to keep track of which entries to keep.
        // If we modify the name, then it won't be removed when the player logs out.
        // This would have to modify how the name is displayed to work properly, or keep track of the modification to
        // remove the player properly upon logout.
    }
}
