package serverutils.serverlib.lib;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.server.management.ServerConfigurationManager;  //PlayerList;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author LatvianModder
 */
public final class ATHelper
{
	public static List<IContainerListener> getContainerListeners(Container container)
	{
		return container.listeners;
	}

	public static EnumChatFormatting getTextFormattingFromDyeColor(ChatStyle color)
	{
		return color.getColor();
	}

	public static char getTextFormattingChar(EnumChatFormatting formatting)
	{
		return formatting.getFormattingCode();
	}

	public static Set<ICommand> getCommandSet(CommandHandler handler)
	{
		return handler.commandSet;
	}

	public static boolean areCommandsAllowedForAll(PlayerList playerList)
	{
		return playerList.commandsAllowedForAll;
	}

	@Nullable
	public static Boolean getBold(ChatStyle style)
	{
		return style.getBold();
	}

	@Nullable
	public static Boolean getItalic(ChatStyle style)
	{
		return style.getItalic();
	}

	@Nullable
	public static Boolean getStriketrough(ChatStyle style)
	{
		return style.getStrikethrough();
	}

	@Nullable
	public static Boolean getUnderlined(ChatStyle style)
	{
		return style.getUnderlined();
	}

	@Nullable
	public static Boolean getObfuscated(ChatStyle style)
	{
		return style.getObfuscated();
	}

	@Nullable
	public static EnumChatFormatting getColor(ChatStyle style)
	{
		return style.getColor();
	}

	@Nullable
	public static ClickEvent getClickEvent(ChatStyle style)
	{
		return style.getChatClickEvent();
	}

	@Nullable
	public static HoverEvent getHoverEvent(ChatStyle style)
	{
		return style.getChatHoverEvent();
	}

	//@Nullable
	//public static String getInsertion(ChatStyle style)
	//{
	//	return style.insertion;
	//}
}