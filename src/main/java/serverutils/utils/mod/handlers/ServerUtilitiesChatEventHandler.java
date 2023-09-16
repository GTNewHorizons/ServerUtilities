package serverutils.utils.mod.handlers;

import java.util.ArrayList;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.ServerTickCallback;
import serverutils.utils.world.LMPlayer;
import serverutils.utils.world.LMWorldServer;
import serverutils.utils.world.PersonalSettings;

public class ServerUtilitiesChatEventHandler {

    private static final String[] LINK_PREFIXES = { "http://", "https://" };

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChatEvent(net.minecraftforge.event.ServerChatEvent e) {
        String[] msg = ServerUtilitiesLib.removeFormatting(e.message).split(" "); // https://github.com/LatvianModder

        ArrayList<String> links = new ArrayList<>();

        for (String s : msg) {
            int index = getFirstLinkIndex(s);
            if (index != -1) links.add(s.substring(index).trim());
        }

        if (!links.isEmpty()) {
            final IChatComponent line = new ChatComponentText("");
            boolean oneLink = links.size() == 1;

            for (int i = 0; i < links.size(); i++) {
                String link = links.get(i);
                IChatComponent c = new ChatComponentText(oneLink ? "[ Link ]" : ("[ Link #" + (i + 1) + " ]"));
                c.getChatStyle()
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(link)));
                c.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
                line.appendSibling(c);
                if (!oneLink) line.appendSibling(new ChatComponentText(" "));
            }

            line.getChatStyle().setColor(EnumChatFormatting.GOLD);

            ServerUtilitiesLib.addCallback(new ServerTickCallback() {

                public void onCallback() {
                    for (LMPlayer p : LMWorldServer.inst.getAllOnlinePlayers()) {
                        if (p.getSettings().get(PersonalSettings.CHAT_LINKS))
                            ServerUtilitiesLib.printChat(p.getPlayer(), line);
                    }
                }
            });
        }
    }

    private static int getFirstLinkIndex(String s) {
        for (int i = 0; i < LINK_PREFIXES.length; i++) {
            int idx = s.indexOf(LINK_PREFIXES[i]);
            if (idx != -1) return idx;
        }

        return -1;
    }
}
