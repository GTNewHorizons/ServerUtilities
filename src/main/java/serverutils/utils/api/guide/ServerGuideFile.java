package serverutils.utils.api.guide;

import java.io.File;
import java.util.*;

import net.minecraft.command.ICommand;
import net.minecraft.util.*;

import com.google.gson.JsonPrimitive;

import latmod.lib.*;
import serverutils.lib.*;
import serverutils.lib.api.cmd.ICustomCommandInfo;
import serverutils.lib.api.notification.*;
import serverutils.utils.api.guide.lines.GuideExtendedTextLine;
import serverutils.utils.mod.*;
import serverutils.utils.mod.config.*;
import serverutils.utils.world.*;

public class ServerGuideFile extends GuidePage {

    public static class CachedInfo {

        public static final GuidePage main = new GuidePage("server_info")
                .setTitle(new ChatComponentTranslation("player_action.serverutilities.server_info"));
        public static GuidePage categoryServer;

        public static void reload() {
            main.clear();

            File file = new File(ServerUtilitiesLib.folderLocal, "guide/");
            if (file.exists() && file.isDirectory()) {
                File[] f = file.listFiles();
                if (f != null && f.length > 0) {
                    Arrays.sort(f, LMFileUtils.fileComparator);
                    for (int i = 0; i < f.length; i++) loadFromFiles(main, f[i]);
                }
            }

            file = new File(ServerUtilitiesLib.folderLocal, "guide_cover.txt");
            if (file.exists() && file.isFile()) {
                try {
                    String text = LMFileUtils.loadAsText(file);
                    if (text != null && !text.isEmpty()) main.printlnText(text.replace("\r", ""));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            main.cleanup();
        }
    }

    private List<LMPlayerServer> players = null;
    private LMPlayerServer self;
    private GuidePage categoryTops = null;

    public ServerGuideFile(LMPlayerServer pself) {
        super(CachedInfo.main.getID());
        setTitle(CachedInfo.main.getTitleComponent());

        if ((self = pself) == null) return;
        boolean isDedi = ServerUtilitiesLib.getServer().isDedicatedServer();

        if (!ServerUtilitiesConfigLogin.motd.components.isEmpty()) {
            for (IChatComponent c : ServerUtilitiesConfigLogin.motd.components) {
                println(c);
            }

            text.add(null);
        }

        copyFrom(CachedInfo.main);

        categoryTops = getSub("tops").setTitle(ServerUtilities.mod.chatComponent("top.title"));

        players = LMWorldServer.inst.getServerPlayers();

        for (LMPlayerServer p : players) {
            p.refreshStats();
        }

        if (ServerUtilitiesConfigGeneral.restart_timer.getAsDouble() > 0D) println(
                ServerUtilities.mod.chatComponent(
                        "cmd.timer_restart",
                        LMStringUtils.getTimeString(ServerUtilsTicks.restartMillis - LMUtils.millis())));

        if (ServerUtilitiesConfigBackups.enabled.getAsBoolean()) println(
                ServerUtilities.mod.chatComponent(
                        "cmd.timer_backup",
                        LMStringUtils.getTimeString(Backups.nextBackup - LMUtils.millis())));

        if (ServerUtilitiesConfigGeneral.server_info_difficulty.getAsBoolean()) println(
                ServerUtilities.mod.chatComponent(
                        "cmd.world_difficulty",
                        LMStringUtils.firstUppercase(
                                pself.getPlayer().worldObj.difficultySetting.toString().toLowerCase())));

        if (ServerUtilitiesConfigGeneral.server_info_mode.getAsBoolean()) println(
                ServerUtilities.mod.chatComponent(
                        "cmd.serveru_gamemode",
                        LMStringUtils.firstUppercase(ServerUtilsWorld.server.getMode().toString().toLowerCase())));

        if (ServerUtilitiesConfigTops.first_joined.getAsBoolean()) addTop(Top.first_joined);
        if (ServerUtilitiesConfigTops.deaths.getAsBoolean()) addTop(Top.deaths);
        if (ServerUtilitiesConfigTops.deaths_ph.getAsBoolean()) addTop(Top.deaths_ph);
        if (ServerUtilitiesConfigTops.last_seen.getAsBoolean()) addTop(Top.last_seen);
        if (ServerUtilitiesConfigTops.time_played.getAsBoolean()) addTop(Top.time_played);

        new EventServerUtilitiesServerGuide(this, self).post();

        GuidePage page = getSub("commands").setTitle(new ChatComponentText("Commands")); // LANG
        page.clear();

        try {
            for (ICommand c : ServerUtilitiesLib.getAllCommands(self.getPlayer())) {
                try {
                    GuidePage cat = new GuidePage('/' + c.getCommandName());

                    @SuppressWarnings("unchecked")
                    List<String> al = c.getCommandAliases();
                    if (al != null && !al.isEmpty()) for (String s : al) {
                        cat.printlnText('/' + s);
                    }

                    if (c instanceof ICustomCommandInfo) {
                        List<IChatComponent> list = new ArrayList<>();
                        ((ICustomCommandInfo) c).addInfo(list, self.getPlayer());

                        for (IChatComponent c1 : list) {
                            cat.println(c1);
                        }
                    } else {
                        String usage = c.getCommandUsage(self.getPlayer());

                        if (usage != null) {
                            if (usage.indexOf('\n') != -1) {
                                String[] usageL = usage.split("\n");
                                for (String s1 : usageL) cat.printlnText(s1);
                            } else {
                                if (usage.indexOf('%') != -1 || usage.indexOf('/') != -1)
                                    cat.println(new ChatComponentText(usage));
                                else cat.println(new ChatComponentTranslation(usage));
                            }
                        }
                    }

                    cat.setParent(page);
                    page.addSub(cat);
                } catch (Exception ex1) {
                    IChatComponent cc = new ChatComponentText('/' + c.getCommandName());
                    cc.getChatStyle().setColor(EnumChatFormatting.DARK_RED);
                    page.getSub('/' + c.getCommandName()).setTitle(cc).printlnText("Errored");

                    if (ServerUtilitiesLib.DEV_ENV) ex1.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        page = getSub("warps").setTitle(new ChatComponentText("Warps")); // LANG
        GuideExtendedTextLine line;

        for (String s : LMWorldServer.inst.warps.list()) {
            line = new GuideExtendedTextLine(page, new ChatComponentText(s));
            line.setClickAction(new ClickAction(ClickActionType.CMD, new JsonPrimitive("warp " + s)));
            page.text.add(line);
        }

        page = getSub("homes").setTitle(new ChatComponentText("Homes")); // LANG

        for (String s : self.homes.list()) {
            line = new GuideExtendedTextLine(page, new ChatComponentText(s));
            line.setClickAction(new ClickAction(ClickActionType.CMD, new JsonPrimitive("home " + s)));
            page.text.add(line);
        }

        cleanup();
        sortAll();
    }

    public void addTop(Top t) {
        GuidePage thisTop = categoryTops.getSub(t.ID).setTitle(t.title);

        Collections.sort(players, t);

        int size = Math.min(players.size(), 250);

        for (int j = 0; j < size; j++) {
            LMPlayerServer p = players.get(j);

            Object data = t.getData(p);
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            sb.append(j + 1);
            sb.append(']');
            sb.append(' ');
            sb.append(p.getProfile().getName());
            sb.append(':');
            sb.append(' ');
            if (!(data instanceof IChatComponent)) sb.append(data);

            IChatComponent c = new ChatComponentText(sb.toString());
            if (p == self) c.getChatStyle().setColor(EnumChatFormatting.DARK_GREEN);
            else if (j < 3) c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
            if (data instanceof IChatComponent) c.appendSibling(ServerUtilitiesLib.getChatComponent(data));
            thisTop.println(c);
        }
    }
}
