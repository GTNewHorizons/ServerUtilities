package serverutils.lib.events;

import net.minecraft.util.ResourceLocation;

import serverutils.lib.lib.config.ConfigValueProvider;
import serverutils.lib.lib.data.AdminPanelAction;
import serverutils.lib.lib.data.ISyncData;
import serverutils.lib.lib.data.TeamAction;

public class ServerUtilitiesLibPreInitRegistryEvent extends ServerUtilitiesLibEvent {

    public interface Registry {

        void registerConfigValueProvider(String id, ConfigValueProvider provider);

        void registerSyncData(String mod, ISyncData data);

        void registerServerReloadHandler(ResourceLocation id, IReloadHandler handler);

        void registerAdminPanelAction(AdminPanelAction action);

        void registerTeamAction(TeamAction action);
    }

    private final Registry registry;

    public ServerUtilitiesLibPreInitRegistryEvent(Registry r) {
        registry = r;
    }

    public Registry getRegistry() {
        return registry;
    }
}
