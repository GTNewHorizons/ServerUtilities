package serverutils.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;
import serverutils.lib.command.CommandUtils;

public class CmdInv extends CmdTreeBase {

    public static class CmdView extends CmdBase {

        public CmdView() {
            super("view", Level.OP);
        }

        @Override
        public List<String> getCommandAliases() {
            return Collections.singletonList("edit");
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return index == 0;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            checkArgs(sender, args, 1);
            EntityPlayerMP self = getCommandSenderAsPlayer(sender);
            EntityPlayerMP other = CommandUtils.getForgePlayer(sender, args[0]).getCommandPlayer(sender);
            self.displayGUIChest(new InvSeeInventory(other.inventory, other));
        }
    }

    public static class CmdDisableRightClick extends CmdBase {

        public CmdDisableRightClick() {
            super("disable_right_click", Level.OP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            LinkedHashSet<String> list = new LinkedHashSet<>(
                    Arrays.asList(ServerUtilitiesConfig.world.disabled_right_click_items));
            ItemStack stack = getCommandSenderAsPlayer(sender).getHeldItem();
            String s = stack.getItem().getUnlocalizedName()
                    + (stack.getHasSubtypes() ? ("@" + stack.getItemDamage()) : "");

            if (list.contains(s)) {
                list.remove(s);
            } else {
                list.add(s);
            }

            ServerUtilitiesConfig.world.disabled_right_click_items = list.toArray(new String[0]);
            ServerUtilitiesConfig.sync();
        }
    }

    public CmdInv() {
        super("inv");
        addSubcommand(new CmdView());
        addSubcommand(new CmdDisableRightClick());
        addSubcommand(new CmdTreeHelp(this));
    }
}
