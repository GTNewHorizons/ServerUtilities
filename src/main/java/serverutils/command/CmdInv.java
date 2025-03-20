package serverutils.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.apache.commons.lang3.StringUtils;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import serverutils.ServerUtilitiesConfig;
import serverutils.invsee.InvseeContainer;
import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeInventories;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.net.MessageInvseeContainer;

public class CmdInv extends CmdTreeBase {

    public static class CmdView extends CmdBase {

        public CmdView() {
            super("view", Level.OP);
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return Universe.get().getPlayers().stream().map(ForgePlayer::getName)
                        .filter(e -> StringUtils.startsWithIgnoreCase(e, args[0])).collect(Collectors.toList());
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

            Map<InvSeeInventories, IInventory> inventories = new LinkedHashMap<>();
            for (InvSeeInventories inv : InvSeeInventories.getActiveInventories()) {
                IModdedInventory modInv = inv.getInventory();
                IInventory inventory = other.isOnline() ? modInv.loadOnlineInventory(other.getPlayer())
                        : modInv.loadOfflineInventory(other);
                if (inventory != null) {
                    inventories.put(inv, inventory);
                }
            }

            self.getNextWindowId();
            new MessageInvseeContainer(other, inventories, self.currentWindowId).sendTo(self);
            self.openContainer = new InvseeContainer(inventories, self, other);
            self.openContainer.windowId = self.currentWindowId;
            self.openContainer.addCraftingToCrafters(self);
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
