package serverutils.old.api.guide;

import java.io.File;
import java.util.Arrays;

import net.minecraft.util.ChatComponentTranslation;

import latmod.lib.LMFileUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.*;
import serverutils.old.mod.ServerUtilities;
import serverutils.old.mod.client.gui.guide.GuiGuide;

public class ClientGuideFile extends GuidePage {

    public static final ClientGuideFile instance = new ClientGuideFile("ClientConfig");

    public ClientGuideFile(String id) {
        super(id);
        setTitle(new ChatComponentTranslation("player_action.serverutilities.guide"));
    }

    public void reload(EventServerUtilitiesReload e) {
        if (ServerUtilitiesLib.DEV_ENV)
            ServerUtilities.logger.info("Guide reloaded @ " + e.world.side + " as " + e.world.getMode());

        clear();

        File file = GameModes.getGameModes().commonMode.getFile("guide/");
        if (file.exists() && file.isDirectory()) {
            File[] f = file.listFiles();
            if (f != null && f.length > 0) {
                Arrays.sort(f, LMFileUtils.fileComparator);
                for (int i = 0; i < f.length; i++) loadFromFiles(this, f[i]);
            }
        }

        file = e.world.getMode().getFile("guide/");
        if (file.exists() && file.isDirectory()) {
            File[] f = file.listFiles();
            if (f != null && f.length > 0) {
                Arrays.sort(f, LMFileUtils.fileComparator);
                for (int i = 0; i < f.length; i++) loadFromFiles(this, f[i]);
            }
        }

        file = e.world.getMode().getFile("guide_intro.txt");
        if (file.exists() && file.isFile()) {
            try {
                String text = LMFileUtils.loadAsText(file);
                if (text != null && !text.isEmpty()) printlnText(text.replace("\r", ""));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        new ServerUtilitiesClientGuide(this).post();

        cleanup();
        GuiGuide.clientGuideGui = null;
    }
}
