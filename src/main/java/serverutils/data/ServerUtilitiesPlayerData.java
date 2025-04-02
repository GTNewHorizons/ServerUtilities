package serverutils.data;

import static serverutils.ServerUtilitiesNotifications.TELEPORT_WARMUP;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesCommon;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.PlayerData;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.TextComponentParser;
import serverutils.net.MessageUpdateTabName;
import serverutils.ranks.Ranks;
import serverutils.task.Task;
import serverutils.task.TeleportTask;

public class ServerUtilitiesPlayerData extends PlayerData {

    public static final String TAG_FLY = "fly";
    public static final String TAG_MUTED = "muted";
    public static final String TAG_LAST_CHUNK = "serveru_lchunk";

    public static ServerUtilitiesPlayerData get(ForgePlayer player) {
        return player.getData().get(ServerUtilities.MOD_ID);
    }

    public static ServerUtilitiesPlayerData get(ICommandSender player) {
        return get(Universe.get().getPlayer(player));
    }

    @Nullable
    public static ServerUtilitiesPlayerData getNullable(EntityPlayer player) {
        ForgePlayer fp = Universe.get().getPlayer(player.getGameProfile());
        return fp == null ? null : get(fp);
    }

    private boolean enablePVP = true;
    private boolean showTeamPrefix = false;
    private String nickname = "";

    public final Collection<ForgePlayer> tpaRequestsFrom;
    public long afkTime;
    private IChatComponent cachedNameForChat;

    private BlockDimPos lastSafePos;
    public final BlockDimPosStorage homes;
    private final TeleportTracker teleportTracker;
    private final VanishData vanishData;

    public ServerUtilitiesPlayerData(ForgePlayer player) {
        super(player);
        homes = new BlockDimPosStorage();
        tpaRequestsFrom = new HashSet<>();
        teleportTracker = new TeleportTracker();
        vanishData = new VanishData();
    }

    @Override
    public String getId() {
        return ServerUtilities.MOD_ID;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("EnablePVP", enablePVP);
        nbt.setBoolean("ShowTeamPrefix", showTeamPrefix);
        nbt.setTag("Homes", homes.serializeNBT());
        nbt.setTag("TeleportTracker", teleportTracker.serializeNBT());
        nbt.setString("Nickname", nickname);
        nbt.setTag("VanishData", vanishData.serializeNBT());
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        enablePVP = !nbt.hasKey("EnablePVP") || nbt.getBoolean("EnablePVP");
        showTeamPrefix = nbt.getBoolean("ShowTeamPrefix");
        homes.deserializeNBT(nbt.getCompoundTag("Homes"));
        teleportTracker.deserializeNBT(nbt.getCompoundTag("TeleportTracker"));
        setLastDeath(BlockDimPos.fromIntArray(nbt.getIntArray("LastDeath")), 0);
        nickname = nbt.getString("Nickname");
        vanishData.deserializeNBT(nbt.getCompoundTag("VanishData"));
    }

    public void addConfig(ConfigGroup main) {
        ConfigGroup config = main.getGroup(ServerUtilities.MOD_ID);
        config.setDisplayName(new ChatComponentText(ServerUtilities.MOD_NAME));

        config.addBool("enable_pvp", () -> enablePVP, v -> enablePVP = v, true)
                .setCanEdit(ServerUtilitiesConfig.world.enable_pvp.isDefault());
        config.addString("nickname", () -> nickname, v -> nickname = v, "").setCanEdit(
                ServerUtilitiesConfig.commands.nick
                        && player.hasPermission(ServerUtilitiesPermissions.CHAT_NICKNAME_SET));
        IChatComponent info = new ChatComponentTranslation(
                "player_config.serverutilities.show_team_prefix.info",
                player.team.getTitle());
        config.addBool("show_team_prefix", () -> showTeamPrefix, v -> showTeamPrefix = v, false).setInfo(info)
                .setExcluded(ServerUtilitiesConfig.teams.force_team_prefix);

        if (player.hasPermission(ServerUtilitiesPermissions.SEE_VANISH)) {
            vanishData.addConfigs(main);
        }
    }

    public boolean enablePVP() {
        return enablePVP;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String name) {
        nickname = name.equals(player.getName()) ? "" : name;
        player.markDirty();
        clearCache();
    }

    public void setLastDeath(@Nullable BlockDimPos pos) {
        setLastDeath(pos, MinecraftServer.getSystemTimeMillis());
    }

    public void setLastDeath(@Nullable BlockDimPos pos, long timestamp) {
        if (pos == null) {
            return;
        }
        teleportTracker.logTeleport(TeleportType.RESPAWN, pos, timestamp);
        player.markDirty();
    }

    public BlockDimPos getLastDeath() {
        return teleportTracker.getLastDeath().getBlockDimPos();
    }

    public void setLastSafePos(@Nullable BlockDimPos pos) {
        lastSafePos = pos;
        player.markDirty();
    }

    @Nullable
    public BlockDimPos getLastSafePos() {
        return lastSafePos;
    }

    public void checkTeleportCooldown(ICommandSender sender, TeleportType teleportType) throws CommandException {
        String cooldownPermission = teleportType.getCooldownPermission();
        if (cooldownPermission == null) return;

        long cooldown = teleportTracker.getLastTeleportTime(teleportType)
                + player.getRankConfig(cooldownPermission).getTimer().millis()
                - System.currentTimeMillis();
        if (cooldown > 0) {
            throw ServerUtilities.error(sender, "cant_use_now_cooldown", StringUtils.getTimeString(cooldown));
        }
    }

    @Override
    public void clearCache() {
        cachedNameForChat = null;
        if (player.isFake()) return;
        EntityPlayerMP p = player.getNullablePlayer();
        if (p == null) return;

        p.refreshDisplayName();
        if (ServerUtilitiesConfig.chat.replace_tab_names) {
            getNameForChat();
        }
    }

    public IChatComponent getNameForChat() {
        EntityPlayerMP playerMP = player.getNullablePlayer();
        if (playerMP == null) {
            return new ChatComponentText("");
        }

        if (ServerUtils.isFake(playerMP)) {
            return new ChatComponentText(player.getName());
        }

        if (cachedNameForChat != null) {
            return cachedNameForChat.createCopy();
        }

        if (Ranks.isActive() && ServerUtilitiesConfig.ranks.override_chat) {
            String text = player.getRankConfig(ServerUtilitiesPermissions.CHAT_NAME_FORMAT).getString();

            try {
                cachedNameForChat = TextComponentParser
                        .parse(text, ServerUtilitiesCommon.chatFormattingSubstituteFunction(player));
            } catch (Exception ex) {
                String s = "Error parsing " + text + ": " + ex.getLocalizedMessage();
                ServerUtilities.LOGGER.error(s);
                cachedNameForChat = new ChatComponentText("BrokenFormatting");
                cachedNameForChat.getChatStyle().setColor(EnumChatFormatting.RED);
                cachedNameForChat.getChatStyle()
                        .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(s)));
            }
        } else {
            cachedNameForChat = new ChatComponentText(player.getDisplayNameString());
        }

        if (ServerUtilitiesConfig.teams.force_team_prefix || showTeamPrefix) {
            IChatComponent end = new ChatComponentText("] ");
            IChatComponent prefix = new ChatComponentText("[").appendSibling(player.team.getTitle()).appendSibling(end);
            cachedNameForChat = new ChatComponentText("").appendSibling(prefix).appendSibling(cachedNameForChat);
        }

        if (NBTUtils.getPersistedData(playerMP, false).getBoolean("recording")) {
            IChatComponent rec = new ChatComponentText("\u25A0 ");
            rec.getChatStyle().setColor(EnumChatFormatting.RED);
            cachedNameForChat = new ChatComponentText("").appendSibling(rec).appendSibling(cachedNameForChat);
        }

        if (ServerUtilitiesConfig.chat.replace_tab_names
                && !cachedNameForChat.getUnformattedText().equals(player.getName())) {
            new MessageUpdateTabName(player, cachedNameForChat).sendToAll();
        }

        cachedNameForChat.appendText(" ");
        return cachedNameForChat.createCopy();
    }

    public void teleport(TeleporterDimPos pos, TeleportType teleportType, @Nullable Task extraTask) {
        EntityPlayerMP player = this.player.getPlayer();
        Universe universe = Universe.get();
        String warmup = teleportType.getWarmupPermission();
        int seconds = warmup == null ? 0 : (int) RankConfigAPI.get(player, warmup).getTimer().seconds();

        if (seconds > 0) {
            IChatComponent component = StringUtils.color(
                    ServerUtilities.lang(player, "stand_still", seconds).appendText(" [" + seconds + "]"),
                    EnumChatFormatting.GOLD);
            TELEPORT_WARMUP.createNotification(component).setVanilla(true).send(player);

            universe.scheduleTask(new TeleportTask(teleportType, player, seconds, pos, extraTask));
        } else {
            new TeleportTask(teleportType, player, 0, pos, extraTask).execute(universe);
        }
    }

    public TeleportLog getLastTeleportLog() {
        return teleportTracker.getLastAvailableLog(player.getProfile());
    }

    public void setLastTeleport(TeleportType teleportType, BlockDimPos from) {
        teleportTracker.logTeleport(teleportType, from, MinecraftServer.getSystemTimeMillis());
        player.markDirty();
    }

    public void clearLastTeleport(TeleportType teleportType) {
        teleportTracker.clearLog(teleportType);
        player.markDirty();
    }

    public VanishData getVanishData() {
        return vanishData;
    }
}
