package serverutils.events;

import net.minecraft.util.ResourceLocation;

import serverutils.lib.config.ConfigValueProvider;
import serverutils.lib.data.AdminPanelAction;
import serverutils.lib.data.ISyncData;
import serverutils.lib.data.TeamAction;

public class ServerUtilitiesPreInitRegistryEvent extends ServerUtilitiesEvent {

    public interface Registry {

        void registerConfigValueProvider(String id, ConfigValueProvider provider);

        void registerSyncData(String mod, ISyncData data);

        void registerServerReloadHandler(ResourceLocation id, IReloadHandler handler);

        void registerAdminPanelAction(AdminPanelAction action);

        void registerTeamAction(TeamAction action);
    }

    private final Registry registry;

    public ServerUtilitiesPreInitRegistryEvent(Registry r) {
        registry = r;
    }

    public Registry getRegistry() {
        return registry;
    }
}
