package serverutils.lib.events;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.lib.EnumReloadType;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.events.universe.UniverseEvent;

public class ServerReloadEvent extends UniverseEvent {

	public static final ResourceLocation ALL = new ResourceLocation("*:*");

	private final ICommandSender sender;
	private final EnumReloadType type;
	private final ResourceLocation reloadId;
	private final Collection<ResourceLocation> failed;
	private boolean clientReloadRequired;
	private final Collection<EntityPlayerMP> onlinePlayers;

	@SuppressWarnings("unchecked")
	public ServerReloadEvent(Universe u, ICommandSender c, EnumReloadType t, ResourceLocation id, Collection<ResourceLocation> f) {
		super(u);
		sender = c;
		type = t;
		reloadId = id;
		failed = f;
		clientReloadRequired = false;
		onlinePlayers = u.server.getConfigurationManager() != null
				? (List<EntityPlayerMP>) u.server.getConfigurationManager().playerEntityList
				: Collections.emptyList();
	}

	public ICommandSender getSender() {
		return sender;
	}

	public EnumReloadType getType() {
		return type;
	}

	public void setClientReloadRequired() {
		clientReloadRequired = true;
	}

	public boolean isClientReloadRequired() {
		return clientReloadRequired;
	}

	public Collection<EntityPlayerMP> getOnlinePlayers() {
		return onlinePlayers;
	}

	public boolean reload(ResourceLocation id) {
		String ridd = reloadId.getResourceDomain();
		String ridp = reloadId.getResourcePath();
		return ridd.equals("*") || ridd.equals(reloadId.getResourceDomain()) && (ridp.equals("*") || ridp.equals(id.getResourcePath()));
	}

	public void failedToReload(ResourceLocation id) {
		failed.add(id);
	}
}
