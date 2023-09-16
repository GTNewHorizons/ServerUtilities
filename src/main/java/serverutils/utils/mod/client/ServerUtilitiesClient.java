package serverutils.utils.mod.client;

import net.minecraftforge.client.ClientCommandHandler;

import cpw.mods.fml.relauncher.*;
import serverutils.lib.EventBusHelper;
import serverutils.lib.api.config.*;
import serverutils.lib.api.gui.LMGuiHandlerRegistry;
import serverutils.utils.badges.BadgeRenderer;
import serverutils.utils.mod.*;
import serverutils.utils.mod.client.gui.guide.ClientSettings;
import serverutils.utils.mod.cmd.CmdMath;
import serverutils.utils.world.*;

@SideOnly(Side.CLIENT)
public class ServerUtilitiesClient extends ServerUtilitiesCommon {

    public static final ConfigEntryBool render_badges = new ConfigEntryBool("render_badges", true);

    public static final ConfigEntryBool sort_friends_az = new ConfigEntryBool("sort_friends_az", false);
    public static final ConfigEntryBool loaded_chunks_space_key = new ConfigEntryBool("loaded_chunks_space_key", false);

    public void preInit() {
        ClientConfigRegistry.addGroup("serverutils", ServerUtilitiesClient.class);
        ClientConfigRegistry.addGroup("serverutils_guide", ClientSettings.class);
        ClientCommandHandler.instance.registerCommand(new CmdMath());
        ServerUtilitiesActions.init();
    }

    public void postInit() {
        LMGuiHandlerRegistry.add(ServerUtilsGuiHandler.instance);
        ServerUtilitiesClickAction.init();

        EventBusHelper.register(BadgeRenderer.instance);
    }

    public LMWorld getClientWorldLM() {
        return LMWorldClient.inst;
    }
}
