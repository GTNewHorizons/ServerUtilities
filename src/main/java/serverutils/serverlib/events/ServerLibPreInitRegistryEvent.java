package serverutils.serverlib.events;

import serverutils.serverlib.lib.config.ConfigValueProvider;
import serverutils.serverlib.lib.data.AdminPanelAction;
import serverutils.serverlib.lib.data.ISyncData;
import serverutils.serverlib.lib.data.TeamAction;
import net.minecraft.util.ResourceLocation;

public class ServerLibPreInitRegistryEvent extends ServerLibEvent
{
	public interface Registry
	{
		void registerConfigValueProvider(String id, ConfigValueProvider provider);

		void registerSyncData(String mod, ISyncData data);

		void registerServerReloadHandler(ResourceLocation id, IReloadHandler handler);

		void registerAdminPanelAction(AdminPanelAction action);

		void registerTeamAction(TeamAction action);
	}

	private final Registry registry;

	public ServerLibPreInitRegistryEvent(Registry r)
	{
		registry = r;
	}

	public Registry getRegistry()
	{
		return registry;
	}
}