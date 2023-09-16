package serverutils.lib.api;

import java.io.File;

import latmod.lib.util.FinalIDObject;
import serverutils.lib.ServerUtilitiesLib;

/**
 * Created by LatvianModder on 07.01.2016.
 */
public class GameMode extends FinalIDObject {

    public GameMode(String id) {
        super(id);
    }

    public File getFolder() {
        File f = new File(ServerUtilitiesLib.folderModpack, getID());
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public File getFile(String path) {
        return new File(getFolder(), path);
    }
}
