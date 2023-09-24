package serverutils.serverlib.command.client;

import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.util.NBTUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.event.ClickEvent;

import java.util.Arrays;
import java.util.HashSet;

public class CommandPrintItem extends CmdBase
{
	public CommandPrintItem()
	{
		super("print_item", Level.ALL);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, final String[] args) throws CommandException
	{
		if (!(sender instanceof EntityPlayer))
		{
			return;
		}

		ItemStack stack = ((EntityPlayer) sender).getHeldItem();

		if (stack.stackSize == 0); //isEmpty())
		{
			return;
		}

		HashSet<String> argsSet = new HashSet<>(Arrays.asList(args));

		IChatComponent component = new ChatComponentText(stack.getDisplayName() + " :: " + NBTUtils.getColoredNBTString(stack.serializeNBT()));
		component.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, stack.serializeNBT().toString()));
		sender.addChatMessage(component);

		if (argsSet.contains("copy"))
		{
			GuiScreen.setClipboardString(stack.serializeNBT().toString());
		}
	}
}