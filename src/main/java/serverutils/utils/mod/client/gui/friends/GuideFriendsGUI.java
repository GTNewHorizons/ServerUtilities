package serverutils.utils.mod.client.gui.friends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.LMColor;
import serverutils.lib.api.friends.LMPNameComparator;
import serverutils.lib.api.friends.LMPStatusComparator;
import serverutils.utils.api.guide.GuidePage;
import serverutils.utils.mod.client.ServerUtilitiesClient;
import serverutils.utils.world.LMPlayer;
import serverutils.utils.world.LMWorldClient;

/**
 * Created by LatvianModder on 23.03.2016.
 */
@SideOnly(Side.CLIENT)
public class GuideFriendsGUI extends GuidePage {

    public GuideFriendsGUI() {
        super("friends_gui");
        setTitle(new ChatComponentText("FriendsGUI"));

        List<LMPlayer> tempPlayerList = new ArrayList<>();
        tempPlayerList.addAll(LMWorldClient.inst.playerMap.values());

        tempPlayerList.remove(LMWorldClient.inst.clientPlayer);

        if (ServerUtilitiesClient.sort_friends_az.getAsBoolean())
            Collections.sort(tempPlayerList, LMPNameComparator.instance);
        else Collections.sort(tempPlayerList, new LMPStatusComparator(LMWorldClient.inst.clientPlayer));

        addSub(new GuideFriendsGUISelfPage());

        for (LMPlayer p : tempPlayerList) {
            addSub(new GuideFriendsGUIPage(p.toPlayerSP()));
        }
    }

    public LMColor getBackgroundColor() {
        return new LMColor.RGB(30, 30, 30);
    }

    public LMColor getTextColor() {
        return new LMColor.RGB(200, 200, 200);
    }

    public Boolean useUnicodeFont() {
        return Boolean.FALSE;
    }
}
