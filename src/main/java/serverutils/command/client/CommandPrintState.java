package serverutils.command.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;

import serverutils.lib.command.CmdBase;

public class CommandPrintState extends CmdBase {

    public CommandPrintState() {
        super("print_block_state", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, final String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer player)) {
            return;
        }
        MovingObjectPosition ray = Minecraft.getMinecraft().objectMouseOver;
        if (ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        Block block = sender.getEntityWorld().getBlock(ray.blockX, ray.blockY, ray.blockZ);

        IChatComponent component = new ChatComponentText(
                block.getPickBlock(ray, sender.getEntityWorld(), ray.blockX, ray.blockY, ray.blockZ, player)
                        .getDisplayName() + " :: "
                        + block.getUnlocalizedName());
        component.getChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, block.getUnlocalizedName()));
        sender.addChatMessage(component);
    }
}
