package serverutils.command;

import static serverutils.ServerUtilitiesPermissions.SEE_VANISH;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.VanishData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdBase.Level;
import serverutils.lib.command.CmdSimple;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdVanish extends CmdTreeBase {

    public CmdVanish() {
        super("vanish");
        addSubcommand(new CmdVanishToggle());
        addSubcommand(new CmdVanishCheck());
        addSubcommand(new CmdSimple("fakequit", Level.OP, (sender) -> sendConnectionMessage(sender, true)));
        addSubcommand(new CmdSimple("fakejoin", Level.OP, (sender) -> sendConnectionMessage(sender, false)));
        for (VanishData.DataType type : VanishData.DataType.values()) {
            addSubcommand(new CmdVanishSub(type));
        }
        addSubcommand(new CmdTreeHelp(this));
    }

    public static class CmdVanishToggle extends CmdBase {

        public CmdVanishToggle() {
            super("toggle", Level.OP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (!(sender instanceof EntityPlayerMP player)) return;
            NBTTagCompound tag = NBTUtils.getPersistedData(player, true);
            Universe universe = Universe.get();
            ForgePlayer forgePlayer = universe.getPlayer(player.getUniqueID());
            if (forgePlayer == null) return;

            boolean toVanish = !tag.getBoolean("vanish");

            if (args.length == 1) {
                toVanish = !(args[0].equals("off") || args[0].equals("false"));
            }

            if (toVanish) {
                tag.setBoolean("vanish", true);
                universe.vanishedPlayers.add(forgePlayer);
                player.capabilities.disableDamage = true;
                player.addChatMessage(new ChatComponentTranslation("commands.vanish.on"));
                updateVanishStatus(player, true);
            } else {
                tag.removeTag("vanish");
                universe.vanishedPlayers.remove(forgePlayer);
                player.capabilities.disableDamage = false;
                player.addChatMessage(new ChatComponentTranslation("commands.vanish.off"));
                updateVanishStatus(player, false);
            }
        }
    }

    public static class CmdVanishCheck extends CmdBase {

        public CmdVanishCheck() {
            super("check", Level.OP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (!(sender instanceof EntityPlayerMP player)) return;
            if (ServerUtils.isVanished(player)) {
                player.addChatMessage(new ChatComponentTranslation("commands.vanish.check.true"));
            } else {
                player.addChatMessage(new ChatComponentTranslation("commands.vanish.check.false"));
            }
        }
    }

    public static class CmdVanishSub extends CmdBase {

        private final VanishData.DataType type;

        public CmdVanishSub(VanishData.DataType type) {
            super(type.name, Level.OP);
            this.type = type;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            if (!(sender instanceof EntityPlayerMP player)) return;
            VanishData data = ServerUtilitiesPlayerData.get(player).getVanishData();
            boolean state;
            if (args.length == 1) {
                state = data.setState(type, parseBoolean(sender, args[0]));
            } else {
                state = data.toggleState(type);
            }

            String stateStr = " " + StatCollector
                    .translateToLocal("addServer.resourcePack." + (state ? "enabled" : "disabled")).toLowerCase();
            player.addChatMessage(
                    new ChatComponentText(
                            StatCollector.translateToLocal("commands.vanish." + getCommandName()) + stateStr));
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return StatCollector.translateToLocal(super.getCommandUsage(sender)) + " - " + type.desc;
        }
    }

    private static void sendConnectionMessage(ICommandSender sender, boolean vanish) {
        IChatComponent message = new ChatComponentTranslation(
                "multiplayer.player." + (vanish ? "left" : "joined"),
                sender.func_145748_c_());
        for (EntityPlayerMP playerMP : Universe.get().server.getConfigurationManager().playerEntityList) {
            if (playerMP == sender || PermissionAPI.hasPermission(playerMP, SEE_VANISH)) continue;
            playerMP.addChatMessage(message);
        }
    }

    private static void updateVanishStatus(EntityPlayerMP playerToUpdate, boolean vanish) {
        Universe universe = Universe.get();
        EntityTrackerEntry playerTracker = getTrackerEntry(playerToUpdate);
        if (playerTracker == null) return;

        playerToUpdate.worldObj
                .getEntitiesWithinAABB(EntityCreature.class, playerToUpdate.boundingBox.expand(40, 40, 40))
                .forEach(entity -> {
                    if (playerToUpdate.equals(entity.getEntityToAttack())) {
                        entity.setTarget(null);
                    }

                    if (playerToUpdate.equals(entity.getAttackTarget())) {
                        entity.setAttackTarget(null);
                    }

                    if (playerToUpdate.equals(entity.getAITarget())) {
                        entity.setRevengeTarget(null);
                    }
                });

        S38PacketPlayerListItem tablistPacket;
        if (vanish) {
            tablistPacket = new S38PacketPlayerListItem(playerToUpdate.getCommandSenderName(), false, 9999);
        } else {
            tablistPacket = new S38PacketPlayerListItem(
                    playerToUpdate.getCommandSenderName(),
                    true,
                    playerToUpdate.ping);
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
            VanishData data = ServerUtilitiesPlayerData.get(playerToUpdate).getVanishData();
            if (data.sendConnectionMessage) sendConnectionMessage(player, vanish);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityTrackerEntry getTrackerEntry(EntityPlayerMP player) {
        return (EntityTrackerEntry) player.getServerForPlayer().getEntityTracker().trackedEntities.stream()
                .filter(entry -> ((EntityTrackerEntry) entry).myEntity == player).findFirst().orElse(null);
    }
}
