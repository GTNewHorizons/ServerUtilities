package serverutils.utils.handlers;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import serverutils.lib.events.player.ForgePlayerConfigEvent;
import serverutils.lib.events.player.ForgePlayerDataEvent;
import serverutils.lib.events.player.ForgePlayerLoggedInEvent;
import serverutils.lib.events.player.ForgePlayerLoggedOutEvent;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.math.BlockDimPos;
import serverutils.lib.lib.math.ChunkDimPos;
import serverutils.lib.lib.util.InvUtils;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.permission.PermissionAPI;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.ServerUtilitiesNotifications;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunks;
import serverutils.utils.data.ServerUtilitiesPlayerData;
import serverutils.utils.data.ServerUtilitiesUniverseData;
import serverutils.utils.net.MessageUpdateTabName;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ServerUtilitiesPlayerEventHandler {

    public static final ServerUtilitiesPlayerEventHandler INST = new ServerUtilitiesPlayerEventHandler();

    @SubscribeEvent
    public void registerPlayerData(ForgePlayerDataEvent event) {
        event.register(new ServerUtilitiesPlayerData(event.getPlayer()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerLoggedIn(ForgePlayerLoggedInEvent event) {
        EntityPlayerMP player = event.getPlayer().getPlayer();

        if (ServerUtils.isFirstLogin(player, "serverutilities_starting_items")) {
            if (ServerUtilitiesConfig.login.enable_starting_items) {
                InvUtils.dropAllItems(
                        player.getEntityWorld(),
                        player.posX,
                        player.posY,
                        player.posZ,
                        ServerUtilitiesConfig.login.getStartingItems());
            }
        }

        if (ServerUtilitiesConfig.login.enable_motd) {
            for (IChatComponent t : ServerUtilitiesConfig.login.getMOTD()) {
                player.addChatMessage(t);
            }
        }

        if (ClaimedChunks.isActive()) {
            ClaimedChunks.instance.markDirty();
        }

        if (ServerUtilitiesConfig.chat.replace_tab_names) {
            new MessageUpdateTabName(player).sendToAll();

            for (EntityPlayerMP player1 : (List<EntityPlayerMP>) player.mcServer
                    .getConfigurationManager().playerEntityList) {
                if (player1 != player) {
                    new MessageUpdateTabName(player1).sendTo(player);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(ForgePlayerLoggedOutEvent event) {
        EntityPlayerMP player = event.getPlayer().getPlayer();

        if (ClaimedChunks.isActive()) {
            ClaimedChunks.instance.markDirty();
        }

        ServerUtilitiesUniverseData.updateBadge(player.getUniqueID());
        player.getEntityData().removeTag(ServerUtilitiesPlayerData.TAG_LAST_CHUNK);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.entityPlayer.getEntityData().removeTag(ServerUtilitiesPlayerData.TAG_LAST_CHUNK);
    }

    @SubscribeEvent
    public void getPlayerSettings(ForgePlayerConfigEvent event) {
        ServerUtilitiesPlayerData.get(event.getPlayer()).addConfig(event.getConfig());
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        EntityLivingBase entity = event.entityLiving;
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP entityPlayerMP = (EntityPlayerMP) entity;
            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(Universe.get().getPlayer(entityPlayerMP));
            data.setLastDeath(new BlockDimPos(entity));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChunkChanged(EntityEvent.EnteringChunk event) {
        if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityPlayerMP) || !Universe.loaded()) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.entity;
        player.func_143004_u();
        ForgePlayer p = Universe.get().getPlayer(player.getGameProfile());

        if (p == null || p.isFake()) {
            return;
        }

        ServerUtilitiesPlayerData.get(p).setLastSafePos(new BlockDimPos((ICommandSender) player));
        ServerUtilitiesNotifications
                .updateChunkMessage(player, new ChunkDimPos(event.newChunkX, event.newChunkZ, player.dimension));
    }

    @SubscribeEvent
    public void onEntityDamage(LivingAttackEvent event) {
        if (ServerUtilitiesConfig.world.disable_player_suffocation_damage && event.entity instanceof EntityPlayer
                && (event.source == DamageSource.inWall)) {
            // event.ammount = 0;
            event.setCanceled(true);
        }
    }

    // TODO: I am registering the event handlers because ftbutil data was not working!!!

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityAttacked(AttackEntityEvent event) {
        if (!ClaimedChunks.canAttackEntity(event.entityPlayer, event.target)) {
            InvUtils.forceUpdate(event.entityPlayer);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (ServerUtilitiesConfig.world.isItemRightClickDisabled(event.entityPlayer.getItemInUse())) {
            event.setCanceled(true);

            if (!event.world.isRemote) {
                event.entityPlayer.addChatComponentMessage(new ChatComponentText("Item disabled!"));
            }

            return;
        }

        if (ClaimedChunks.blockBlockInteractions(event.entityPlayer, event.x, event.y, event.z, 0)) {
            InvUtils.forceUpdate(event.entityPlayer);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (ServerUtilitiesConfig.world.isItemRightClickDisabled(event.entityPlayer.getItemInUse())) {
            event.setCanceled(true);

            if (!event.world.isRemote) {
                event.entityPlayer.addChatComponentMessage(new ChatComponentText("Item disabled!"));
            }

            return;
        }

        if (ClaimedChunks.blockItemUse(event.entityPlayer, event.x, event.y, event.z)) {
            InvUtils.forceUpdate(event.entityPlayer);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (ClaimedChunks.blockBlockEditing(event.getPlayer(), event.x, event.y, event.z, 0)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockEvent.PlaceEvent event) {
        if (ClaimedChunks.blockBlockEditing(event.player, event.x, event.y, event.z, 0)) {
            InvUtils.forceUpdate(event.player);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockLeftClick(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (ClaimedChunks.blockBlockEditing(event.entityPlayer, event.x, event.y, event.z, 0)) {
            event.setCanceled(true);
        }
    }

    /*
     * @SubscribeEvent(priority = EventPriority.HIGH) public static void onItemPickup(EntityItemPickupEvent event) { }
     */

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (ServerUtilitiesConfig.commands.nick && Universe.loaded() && event.entityPlayer instanceof EntityPlayerMP) {
            ForgePlayer p = Universe.get().getPlayer(event.entityPlayer.getGameProfile());

            if (p != null) {
                ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(p);

                if (!data.getNickname().isEmpty()
                        && PermissionAPI.hasPermission(event.entityPlayer, ServerUtilitiesPermissions.CHAT_NICKNAME_SET)) {
                    String name = StringUtils.addFormatting(data.getNickname());

                    if (!p.hasPermission(ServerUtilitiesPermissions.CHAT_NICKNAME_COLORS)) {
                        name = StringUtils.unformatted(name);
                    } else if (name.indexOf(StringUtils.FORMATTING_CHAR) != -1) {
                        name += EnumChatFormatting.RESET;
                    }

                    if (ServerUtilitiesConfig.chat.add_nickname_tilde) {
                        name = "~" + name;
                    }

                    event.displayname = name;
                }
            }
        }
    }

    private static String getStateName(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        Block block = world.getBlock(x, y, z);
        return block.getLocalizedName() + ":" + meta;
    }

    private static String getDim(EntityPlayer player) {
        return ServerUtils.getDimensionName(player.dimension).getUnformattedText();
    }

    private static String getPos(int x, int y, int z) {
        return String.format("[%d, %d, %d]", x, y, z);
    }

    @SubscribeEvent
    public void onBlockBreakLog(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();

        if (ServerUtilitiesConfig.world.logging.block_broken && player instanceof EntityPlayerMP
                && ServerUtilitiesConfig.world.logging.log((EntityPlayerMP) player)) {
            ServerUtilitiesUniverseData.worldLog(
                    String.format(
                            "%s broke %s at %s in %s",
                            player.getDisplayName(),
                            getStateName(event.world, event.x, event.y, event.z),
                            getPos(event.x, event.y, event.z),
                            getDim(player)));
        }
    }

    @SubscribeEvent
    public void onBlockPlaceLog(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.player;

        if (ServerUtilitiesConfig.world.logging.block_placed && player instanceof EntityPlayerMP
                && ServerUtilitiesConfig.world.logging.log((EntityPlayerMP) player)) {
            ServerUtilitiesUniverseData.worldLog(
                    String.format(
                            "%s placed %s at %s in %s",
                            player.getDisplayName(),
                            getStateName(event.world, event.x, event.y, event.z),
                            getPos(event.x, event.y, event.z),
                            getDim(player)));
        }
    }

    @SubscribeEvent
    public void onRightClickItemLog(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            return;
        }
        EntityPlayer player = event.entityPlayer;

        if (ServerUtilitiesConfig.world.logging.item_clicked_in_air && player instanceof EntityPlayerMP
                && ServerUtilitiesConfig.world.logging.log((EntityPlayerMP) player)) {
            ServerUtilitiesUniverseData.worldLog(
                    String.format(
                            "%s clicked %s in air at %s in %s",
                            player.getDisplayName(),
                            event.entityPlayer.getItemInUse().getItem()
                                    .getItemStackDisplayName(event.entityPlayer.getItemInUse()),
                            getPos(event.x, event.y, event.z),
                            getDim(player)));
        }
    }
}
