package serverutils.serverlib.lib.command;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibConfig;
import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.config.ConfigValue;
import serverutils.serverlib.lib.config.ConfigValueInstance;
import serverutils.serverlib.lib.config.IConfigCallback;
import serverutils.serverlib.lib.data.ServerLibAPI;
import serverutils.serverlib.lib.math.BlockDimPos;
import serverutils.serverlib.lib.util.StringUtils;
import serverutils.serverlib.lib.util.text_components.Notification;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author LatvianModder
 */
public abstract class CmdEditConfigBase extends CmdBase
{
	public CmdEditConfigBase(String n, Level l)
	{
		super(n, l);
	}

	public abstract ConfigGroup getGroup(ICommandSender sender) throws CommandException;

	public IConfigCallback getCallback(ICommandSender sender) throws CommandException
	{
		return IConfigCallback.DEFAULT;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockDimPos pos)
	{
		try
		{
			ConfigGroup group = getGroup(sender);

			if (args.length == 1)
			{
				List<String> keys = getListOfStringsMatchingLastWord(args, group.getValueKeyTree());

				if (keys.size() > 1)
				{
					keys.sort(StringUtils.ID_COMPARATOR);
				}

				return keys;
			}
			else if (args.length == 2)
			{
				ConfigValue value = group.getValue(args[0]);

				if (!value.isNull())
				{
					List<String> variants = value.getVariants();

					if (!variants.isEmpty())
					{
						return getListOfStringsMatchingLastWord(args, variants);
					}
				}

				return Collections.emptyList();
			}
		}
		catch (CommandException ex)
		{
			//ITextComponent c = new TextComponentTranslation(ex.getMessage(), ex.getErrorObjects());
			//c.getStyle().setColor(TextFormatting.DARK_RED);
			//sender.addChatMessage(c);
		}

		return super.getTabCompletions(server, sender, args, pos);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		if (args.length == 0 && sender instanceof EntityPlayerMP)
		{
			ServerLibAPI.editServerConfig(getCommandSenderAsPlayer(sender), getGroup(sender), getCallback(sender));
			return;
		}

		checkArgs(sender, args, 1);
		ConfigGroup group = getGroup(sender);
		ConfigValueInstance instance = group.getValueInstance(args[0]);

		if (instance == null)
		{
			throw ServerLib.error(sender, "serverlib.lang.config_command.invalid_key", args[0]);
		}

		if (args.length >= 2)
		{
			String valueString = String.valueOf(StringUtils.joinSpaceUntilEnd(1, args));

			if (!instance.getValue().setValueFromString(sender, valueString, true))
			{
				return;
			}

			if (ServerLibConfig.debugging.log_config_editing)
			{
				ServerLib.LOGGER.info("Setting " + instance.getPath() + " to " + valueString);
			}

			instance.getValue().setValueFromString(sender, valueString, false);
			getCallback(sender).onConfigSaved(group, sender);
			Notification.of(Notification.VANILLA_STATUS, ServerLib.lang(sender, "serverlib.lang.config_command.set", instance.getDisplayName(), group.getValue(args[0]).toString())).send(server, getCommandSenderAsPlayer(sender));
		}

		sender.addChatMessage(instance.getValue().getStringForGUI());
	}
}