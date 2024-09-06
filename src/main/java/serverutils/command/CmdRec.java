package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.command.CmdBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.NBTUtils;

public class CmdRec extends CmdBase {

    public CmdRec() {
        super("rec", Level.ALL);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        NBTTagCompound nbt = NBTUtils.getPersistedData(player, true);

        if (nbt.getBoolean("recording")) {
            nbt.removeTag("recording");
            IChatComponent component = new ChatComponentTranslation(
                    "commands.rec.not_recording",
                    player.getDisplayName());
            component.getChatStyle().setColor(EnumChatFormatting.GRAY);
            component.getChatStyle().setItalic(true);
            player.mcServer.getConfigurationManager().sendChatMsg(component);
        } else {
            nbt.setBoolean("recording", true);
            IChatComponent component = new ChatComponentTranslation("commands.rec.recording", player.getDisplayName());
            component.getChatStyle().setColor(EnumChatFormatting.GRAY);
            component.getChatStyle().setItalic(true);
            player.mcServer.getConfigurationManager().sendChatMsg(component);
        }

        Universe.get().getPlayer(player).clearCache();
    }
}
