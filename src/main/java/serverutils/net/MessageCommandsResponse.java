package serverutils.net;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.GuiViewCommands;
import serverutils.lib.command.CommandTreeBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.ranks.ICommandWithPermission;

public class MessageCommandsResponse extends MessageToClient {

    private Map<String, IChatComponent> commands;

    public MessageCommandsResponse() {}

    public MessageCommandsResponse(EntityPlayerMP player) {
        commands = new HashMap<>();
        for (ICommand command : CommandUtils.getAllCommands(player)) {
            IChatComponent usage = CommandUtils.getTranslatedUsage(command, player);
            if (command instanceof CommandTreeBase cmdTree) {
                StringBuilder builder = new StringBuilder();
                builder.append(" [");
                for (ICommand subCommand : cmdTree.getSubCommands()) {
                    builder.append(subCommand.getCommandName()).append("|");
                }
                int index = builder.lastIndexOf("|");
                if (index != -1) {
                    builder.deleteCharAt(index);
                }
                builder.append("]");
                IChatComponent subCommands = new ChatComponentText(builder.toString());
                usage.appendSibling(subCommands);
            }
            if (command instanceof ICommandWithPermission cmd) {
                usage.appendText("\n").appendSibling(
                        new ChatComponentText(
                                ("Added by: " + EnumChatFormatting.GOLD + cmd.serverutilities$getModName())));
            }
            commands.put(command.getCommandName(), usage);
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeMap(commands, DataOut.STRING, DataOut.TEXT_COMPONENT);
    }

    @Override
    public void readData(DataIn data) {
        commands = data.readMap(DataIn.STRING, DataIn.TEXT_COMPONENT);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiViewCommands(commands).openGui();
    }
}
