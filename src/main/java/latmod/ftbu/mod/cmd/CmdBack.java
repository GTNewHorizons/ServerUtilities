package latmod.ftbu.mod.cmd;

import latmod.ftbu.core.Teleporter;
import latmod.ftbu.core.cmd.*;
import latmod.ftbu.core.world.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class CmdBack extends CommandLM
{
	public CmdBack()
	{ super("back", CommandLevel.ALL); }
	
	public String onCommand(ICommandSender ics, String[] args)
	{
		EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
		LMPlayerServer p = LMWorld.server.getPlayer(ep);
		if(p.lastDeath == null) return "No deathpoint found!";
		Teleporter.teleportPlayer(ep, p.lastDeath);
		return null;
	}
}