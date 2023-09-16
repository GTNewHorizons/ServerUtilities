package ftb.lib;

import java.io.File;

import cpw.mods.fml.relauncher.Side;
import ftb.lib.api.GameMode;
import ftb.lib.api.GameModes;
import latmod.lib.ByteIOStream;
import latmod.lib.LMFileUtils;

public class FTBWorld {

    public static FTBWorld server = null, client = null;

    public static FTBWorld get(Side s) {
        return s.isServer() ? server : client;
    }

    public final Side side;
    private GameMode currentMode;

    private File currentModeFile = null;
    private File currentWorldIDFile = null;

    public FTBWorld(Side sd) {
        side = sd;

        if (side.isClient()) {
            currentMode = new GameMode("default");
        } else {
            currentMode = GameModes.getGameModes().defaultMode;
            try {
                currentModeFile = new File(FTBLib.folderWorld, "ftb_gamemode.txt");
                currentMode = GameModes.getGameModes().get(LMFileUtils.loadAsText(currentModeFile).trim());
            } catch (Exception ex) { /* ex.printStackTrace(); */ }

            for (GameMode s : GameModes.getGameModes().modes.values()) s.getFolder();
        }
    }

    public GameMode getMode() {
        return currentMode;
    }

    public void writeReloadData(ByteIOStream io) {
        io.writeUTF(currentMode.getID());
    }

    public void readReloadData(ByteIOStream io) {
        String mode = io.readUTF();
        GameModes.reload();
        currentMode = GameModes.getGameModes().get(mode);
    }

    /**
     * 0 = OK, 1 - Mode is invalid, 2 - Mode already set (will be ignored and return 0, if forced == true)
     */
    public int setMode(String s) {
        GameMode m = GameModes.getGameModes().modes.get(s);

        if (m == null) return 1;
        if (m.equals(currentMode)) return 2;

        currentMode = m;

        if (side.isServer()) {
            try {
                LMFileUtils.save(currentModeFile, currentMode.getID());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return 0;
    }
}
