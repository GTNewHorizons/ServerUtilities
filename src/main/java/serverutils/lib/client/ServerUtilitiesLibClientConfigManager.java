package serverutils.lib.client;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;

import serverutils.lib.client.resource.IResourceType;
import serverutils.lib.client.resource.ISelectiveResourceReloadListener;
import serverutils.lib.lib.io.DataReader;
import serverutils.mod.client.ServerUtilitiesClient;

/**
 * @author LatvianModder
 */
public enum ServerUtilitiesLibClientConfigManager implements ISelectiveResourceReloadListener {

    INSTANCE;

    @Override
    public void onResourceManagerReload(IResourceManager manager, Predicate<IResourceType> resourcePredicate) {
        if (!resourcePredicate.test(ServerUtilitiesLibResourceType.SERVERUTILS_CONFIG)) {
            return;
        }

        ServerUtilitiesClient.CLIENT_CONFIG_MAP.clear();

        for (String domain : (Set<String>) manager.getResourceDomains()) {
            try {
                for (IResource resource : (List<IResource>) manager
                        .getAllResources(new ResourceLocation(domain, "client_config.json"))) {
                    for (JsonElement e : DataReader.get(resource).json().getAsJsonArray()) {
                        ClientConfig c = new ClientConfig(e.getAsJsonObject());
                        ServerUtilitiesClient.CLIENT_CONFIG_MAP.put(c.id, c);
                    }
                }
            } catch (Exception ex) {
                if (!(ex instanceof FileNotFoundException)) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
