package latmod.core.mod.cmd;

import latmod.core.mod.LC;
import net.minecraft.command.*;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public abstract class CommandBaseLC extends CommandBase
{
	public static void registerCommands(FMLServerStartingEvent e)
	{
		regCmd(e, new CmdLatCore(LC.config.commands.latcore));
		regCmd(e, new CmdSetNick(LC.config.commands.setnick));
		//regCmd(e, new CmdSetSkin(LC.config.commands.setskin));
		//regCmd(e, new CmdSetCape(LC.config.commands.setcape));
	}
	
	private static void regCmd(FMLServerStartingEvent e, CommandBaseLC c)
	{ if(c.enabled > 0) e.registerServerCommand(c); }
	
	public final int enabled;
	
	public CommandBaseLC(int e)
	{ enabled = e; }
	
	public final int getRequiredPermissionLevel()
	{ return (enabled == 2) ? 2 : ((enabled == 1) ? 0 : 5); }
	
	public final boolean canCommandSenderUseCommand(ICommandSender ics)
	{ return enabled > 0; }
}