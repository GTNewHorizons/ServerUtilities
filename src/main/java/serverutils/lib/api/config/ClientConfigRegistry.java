package serverutils.lib.api.config;

import java.io.File;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.ServerUtilitiesLib;

@SideOnly(Side.CLIENT)
public final class ClientConfigRegistry {

    private static final ConfigFile file = new ConfigFile("client_config");

    public static IConfigProvider provider() {
        return new IConfigProvider() {

            public String getGroupTitle(ConfigGroup g) {
                return I18n.format(g.getID());
            }

            public String getEntryTitle(ConfigEntry e) {
                return I18n.format(e.getFullID());
            }

            public ConfigGroup getConfigGroup() {
                if (file.getFile() == null) {
                    file.setFile(new File(ServerUtilitiesLib.folderLocal, "client/config.json"));
                    file.load();
                }

                return file;
            }

            public void save() {
                file.save();
            }
        };
    }

    /**
     * Do this before postInit()
     */
    public static void addGroup(String id, Class<?> c) {
        file.addGroup(id, c);
    }

    public static void add(ConfigGroup group) {
        file.add(group, false);
    }
}
