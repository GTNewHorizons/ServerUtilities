package serverutils.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;

public class CmdInv extends CmdTreeBase {

    public static class CmdView extends CmdBase {

        public CmdView() {
            super("view", Level.OP);
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return Universe.get().getPlayers().stream().map(ForgePlayer::getName).collect(Collectors.toList());
            }
            return super.addTabCompletionOptions(sender, args);
        }

        @Override
        public List<String> getCommandAliases() {
            return Collections.singletonList("edit");
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            checkArgs(sender, args, 1);
            EntityPlayerMP self = getCommandSenderAsPlayer(sender);
            ForgePlayer other = Universe.get().getPlayer(args[0]);

            if (other == null || other.isFake() || other.getPlayerNBT() == null) {
                throw new CommandException("commands.generic.player.notFound", args[0]);
            }

            if (other.isOnline()) {
                self.displayGUIChest(new InvSeeInventory(other.getPlayer().inventory, other.getPlayer()));
            } else {
                NBTTagCompound tag = other.getPlayerNBT();
                InventoryPlayer playerInv = new InventoryPlayer(null);
                playerInv.readFromNBT(tag.getTagList("Inventory", Constants.NBT.TAG_COMPOUND));
                InvSeeInventory invSee = new InvSeeInventory(playerInv, null);
                invSee.setSaveCallback(inv -> {
                    InventoryPlayer invPlayer = inv.getPlayerInv();
                    NBTTagList invTag = new NBTTagList();
                    invPlayer.writeToNBT(invTag);
                    tag.setTag("Inventory", invTag);
                    other.setPlayerNBT(tag);
                });
                self.displayGUIChest(invSee);
            }
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
            ConfigurationManager.save(ServerUtilitiesConfig.class);
        }
    }

    public CmdInv() {
        super("inv");
        addSubcommand(new CmdView());
        addSubcommand(new CmdDisableRightClick());
        addSubcommand(new CmdTreeHelp(this));
    }
}
