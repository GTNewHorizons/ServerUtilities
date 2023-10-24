package serverutils.command.client;

import java.util.Arrays;
import java.util.HashSet;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import serverutils.lib.command.CmdBase;
import serverutils.lib.util.NBTUtils;

public class CommandPrintItem extends CmdBase {

    public CommandPrintItem() {
        super("print_item", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, final String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer player)) {
            return;
        }

        ItemStack stack = player.getHeldItem();

        if (stack == null) {
            return;
        }

        HashSet<String> argsSet = new HashSet<>(Arrays.asList(args));

        IChatComponent component = new ChatComponentText(
                stack.getDisplayName() + " :: " + NBTUtils.getColoredNBTString(stack.writeToNBT(new NBTTagCompound())));
        component.getChatStyle().setChatClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, stack.writeToNBT(new NBTTagCompound()).toString()));
        sender.addChatMessage(component);

        if (argsSet.contains("copy")) {
            GuiScreen.setClipboardString(stack.writeToNBT(new NBTTagCompound()).toString());
        }
    }
}
