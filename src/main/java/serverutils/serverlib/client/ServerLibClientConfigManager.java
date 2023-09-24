package serverutils.serverlib.client;

import serverutils.serverlib.client.resource.IResourceType;
import serverutils.serverlib.client.resource.ISelectiveResourceReloadListener;
import serverutils.serverlib.lib.io.DataReader;
import com.google.gson.JsonElement;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;


import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public enum ServerLibClientConfigManager implements ISelectiveResourceReloadListener {
	INSTANCE;

	@Override
	public void onResourceManagerReload(IResourceManager manager, Predicate<IResourceType> resourcePredicate) {
		if (!resourcePredicate.test(ServerLibResourceType.SERVERLIB_CONFIG)) {
			return;
		}

		ServerLibClient.CLIENT_CONFIG_MAP.clear();

		for (String domain : (Set<String>) manager.getResourceDomains()) {
			try {
				for (IResource resource : (List<IResource>) manager.getAllResources(new ResourceLocation(domain, "client_config.json"))) {
					for (JsonElement e : DataReader.get(resource).json().getAsJsonArray()) {
						ClientConfig c = new ClientConfig(e.getAsJsonObject());
						ServerLibClient.CLIENT_CONFIG_MAP.put(c.id, c);
					}
				}
			}
			catch (Exception ex) {
				if (!(ex instanceof FileNotFoundException)) {
					ex.printStackTrace();
				}
			}
		}
	}
}