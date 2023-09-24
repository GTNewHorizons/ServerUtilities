package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ForgeTeam;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class ForgeTeamOwnerChangedEvent extends ForgeTeamEvent
{
	private final ForgePlayer oldOwner;

	public ForgeTeamOwnerChangedEvent(ForgeTeam team, @Nullable ForgePlayer o0)
	{
		super(team);
		oldOwner = o0;
	}

	@Nullable
	public ForgePlayer getOldOwner()
	{
		return oldOwner;
	}
}