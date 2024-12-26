package serverutils.command;

import static serverutils.ServerUtilitiesPermissions.SEE_VANISH;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import serverutils.lib.command.CmdBase;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdVanish extends CmdBase {

    public CmdVanish() {
        super("vanish", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP playerMP)) return;
        boolean sendConnectionMessage = false;
        if (args.length == 1) {
            sendConnectionMessage = args[0].equalsIgnoreCase("true");
        }

        toggleVanish(playerMP, sendConnectionMessage);
    }

    public static void toggleVanish(EntityPlayerMP player, boolean sendConnectionMessage) {
        NBTTagCompound tag = NBTUtils.getPersistedData(player, true);
        Universe universe = Universe.get();
        ForgePlayer forgePlayer = universe.getPlayer(player.getUniqueID());
        if (forgePlayer == null) return;

        if (tag.getBoolean("vanish")) {
            tag.removeTag("vanish");
            universe.vanishedPlayers.remove(forgePlayer);
            player.capabilities.disableDamage = false;
            player.addChatMessage(new ChatComponentTranslation("commands.vanish.off"));
            updateVanishStatus(player, false, sendConnectionMessage);
        } else {
            tag.setBoolean("vanish", true);
            universe.vanishedPlayers.add(forgePlayer);
            player.capabilities.disableDamage = true;
            player.addChatMessage(new ChatComponentTranslation("commands.vanish.on"));
            updateVanishStatus(player, true, sendConnectionMessage);
        }
    }

    private static void updateVanishStatus(EntityPlayerMP playerToUpdate, boolean vanish,
            boolean sendConnectionMessage) {
        Universe universe = Universe.get();
        EntityTrackerEntry playerTracker = getTrackerEntry(playerToUpdate);
        if (playerTracker == null) return;

        IChatComponent message;
        S38PacketPlayerListItem tablistPacket;
        if (vanish) {
            tablistPacket = new S38PacketPlayerListItem(playerToUpdate.getCommandSenderName(), false, 9999);
            message = new ChatComponentTranslation("multiplayer.player.left", playerToUpdate.func_145748_c_());
        } else {
            tablistPacket = new S38PacketPlayerListItem(
                    playerToUpdate.getCommandSenderName(),
                    true,
                    playerToUpdate.ping);
            message = new ChatComponentTranslation("multiplayer.player.joined", playerToUpdate.func_145748_c_());
        }

        for (EntityPlayerMP player : universe.server.getConfigurationManager().playerEntityList) {
            if (player == playerToUpdate || PermissionAPI.hasPermission(player, SEE_VANISH)) continue;
            player.playerNetServerHandler.sendPacket(tablistPacket);
            if (vanish) {
                playerTracker.removePlayerFromTracker(player);
            } else {
                playerTracker.isDataInitialized = false;
                playerTracker.tryStartWachingThis(player);
            }
            if (sendConnectionMessage) player.addChatMessage(message);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityTrackerEntry getTrackerEntry(EntityPlayerMP player) {
        return (EntityTrackerEntry) player.getServerForPlayer().getEntityTracker().trackedEntities.stream()
                .filter(entry -> ((EntityTrackerEntry) entry).myEntity == player).findFirst().orElse(null);
    }
}
