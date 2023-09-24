package serverutils.serverlib.events.team;

import serverutils.serverlib.lib.data.ForgeTeam;

import java.io.File;

public class ForgeTeamDeletedEvent extends ForgeTeamEvent
{
	private final File folder;

	public ForgeTeamDeletedEvent(ForgeTeam team, File f)
	{
		super(team);
		folder = f;
	}

	public File getDataFolder()
	{
		return folder;
	}
}