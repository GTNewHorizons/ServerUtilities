package serverutils.handlers;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesNotifications;
import serverutils.ServerUtilitiesPermissions;
import serverutils.backups.Backups;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.ServerUtilitiesUniverseData;
import serverutils.events.player.ForgePlayerConfigEvent;
import serverutils.events.player.ForgePlayerDataEvent;
import serverutils.events.player.ForgePlayerLoggedInEvent;
import serverutils.events.player.ForgePlayerLoggedOutEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.net.MessageSyncData;
import serverutils.net.MessageUpdateTabName;

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
                InvUtils.giveItemFromIterable(player, ServerUtilitiesConfig.login.getStartingItems());
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

        Backups.hadPlayer = true;

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
        if (entity instanceof EntityPlayerMP entityPlayerMP) {
            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(Universe.get().getPlayer(entityPlayerMP));
            data.setLastDeath(new BlockDimPos(entity));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChunkChanged(EntityEvent.EnteringChunk event) {
        if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityPlayerMP player) || !Universe.loaded()) {
            return;
        }

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

        if (ServerUtilitiesConfig.world.isItemRightClickDisabled(event.entityPlayer.getHeldItem())) {
            event.setCanceled(true);

            if (!event.world.isRemote) {
                event.entityPlayer.addChatComponentMessage(
                        new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "Item is disabled!"));
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
        if (ServerUtilitiesConfig.world.isItemRightClickDisabled(event.entityPlayer.getHeldItem())) {
            event.setCanceled(true);

            if (!event.world.isRemote) {
                event.entityPlayer.addChatComponentMessage(
                        new ChatComponentText(EnumChatFormatting.DARK_PURPLE + "Item is disabled!"));
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

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (ServerUtilitiesConfig.commands.nick && Universe.loaded() && event.entityPlayer instanceof EntityPlayerMP) {
            ForgePlayer p = Universe.get().getPlayer(event.entityPlayer.getGameProfile());

            if (p != null) {
                ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(p);

                if (!data.getNickname().isEmpty() && PermissionAPI
                        .hasPermission(event.entityPlayer, ServerUtilitiesPermissions.CHAT_NICKNAME_SET)) {
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

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.ticksExisted % 5 == 2 && event.player instanceof EntityPlayerMP player) {
            byte opState = player.getEntityData().getByte("ServerLibOP");
            byte newOpState = ServerUtils.isOP(player) ? (byte) 2 : (byte) 1;

            if (opState != newOpState) {
                player.getEntityData().setByte("ServerLibOP", newOpState);
                Universe.get().clearCache();
                ForgePlayer forgePlayer = Universe.get().getPlayer(player.getGameProfile());
                if (forgePlayer != null) {
                    new MessageSyncData(false, player, forgePlayer).sendTo(player);
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

        if (ServerUtilitiesConfig.world.logging.block_broken && player instanceof EntityPlayerMP playerMP
                && ServerUtilitiesConfig.world.logging.log(playerMP)) {
            ServerUtilitiesUniverseData.worldLog(
                    String.format(
                            "%s broke %s at %s in %s",
                            playerMP.getDisplayName(),
                            getStateName(event.world, event.x, event.y, event.z),
                            getPos(event.x, event.y, event.z),
                            getDim(playerMP)));
        }
    }

    @SubscribeEvent
    public void onBlockPlaceLog(BlockEvent.PlaceEvent event) {
        EntityPlayer player = event.player;

        if (ServerUtilitiesConfig.world.logging.block_placed && player instanceof EntityPlayerMP playerMP
                && ServerUtilitiesConfig.world.logging.log(playerMP)) {
            ServerUtilitiesUniverseData.worldLog(
                    String.format(
                            "%s placed %s at %s in %s",
                            playerMP.getDisplayName(),
                            getStateName(event.world, event.x, event.y, event.z),
                            getPos(event.x, event.y, event.z),
                            getDim(playerMP)));
        }
    }

    @SubscribeEvent
    public void onRightClickItemLog(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            return;
        }
        EntityPlayer player = event.entityPlayer;

        if (ServerUtilitiesConfig.world.logging.item_clicked_in_air && player instanceof EntityPlayerMP playerMP
                && ServerUtilitiesConfig.world.logging.log(playerMP)) {
            ServerUtilitiesUniverseData.worldLog(
                    String.format(
                            "%s clicked %s in air at %s in %s",
                            playerMP.getDisplayName(),
                            playerMP.getHeldItem().getDisplayName(),
                            getPos(event.x, event.y, event.z),
                            getDim(playerMP)));
        }
    }

    @SubscribeEvent
    public void onEntityAttackedLog(AttackEntityEvent event) {
        EntityPlayer player = event.entityPlayer;
        Entity target = event.target;

        if (ServerUtilitiesConfig.world.logging.entity_attacked && player instanceof EntityPlayerMP playerMP
                && ServerUtilitiesConfig.world.logging.log(playerMP)) {
            if (target instanceof EntityPlayerMP targetPlayer) {
                ServerUtilitiesUniverseData.worldLog(
                        String.format(
                                "%s attacked %s with %s at %s in %s",
                                playerMP.getDisplayName(),
                                targetPlayer.getDisplayName(),
                                playerMP.getHeldItem().getDisplayName(),
                                getPos((int) playerMP.posX, (int) playerMP.posY, (int) playerMP.posZ),
                                getDim(playerMP)));
            }
            if (!ServerUtilitiesConfig.world.logging.exclude_mob_entity
                    && target instanceof EntityCreature targetCreature) {
                ServerUtilitiesUniverseData.worldLog(
                        String.format(
                                "%s attacked %s with %s at %s in %s",
                                playerMP.getDisplayName(),
                                targetCreature.getCommandSenderName(),
                                playerMP.getHeldItem().getDisplayName(),
                                getPos((int) playerMP.posX, (int) playerMP.posY, (int) playerMP.posZ),
                                getDim(playerMP)));
            }
        }
    }
}
