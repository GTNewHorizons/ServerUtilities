package serverutils.utils.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibCommon;
import serverutils.lib.lib.EnumMessageLocation;
import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.config.RankConfigAPI;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.PlayerData;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.math.BlockDimPos;
import serverutils.lib.lib.math.TeleporterDimPos;
import serverutils.lib.lib.util.NBTUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.misc.IScheduledTask;
import serverutils.lib.lib.util.misc.TimeType;
import serverutils.lib.lib.util.text_components.TextComponentParser;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.ServerUtilitiesPermissions;

public class ServerUtilitiesPlayerData extends PlayerData {

    public static final String TAG_FLY = "fly";
    public static final String TAG_MUTED = "muted";
    public static final String TAG_LAST_CHUNK = "ftbu_lchunk";

    public enum Timer {

        HOME(TeleportType.HOME),
        WARP(TeleportType.WARP),
        BACK(TeleportType.BACK),
        SPAWN(TeleportType.SPAWN),
        TPA(TeleportType.TPA),
        RTP(TeleportType.RTP);

        public static final Timer[] VALUES = values();

        private final String cooldown;
        private final String warmup;
        private final TeleportType teleportType;

        Timer(TeleportType teleportType) {
            this.teleportType = teleportType;
            this.cooldown = teleportType.getCooldownPermission();
            this.warmup = teleportType.getWarmupPermission();
        }

        public void teleport(EntityPlayerMP player, Function<EntityPlayerMP, TeleporterDimPos> pos,
                @Nullable IScheduledTask extraTask) {
            Universe universe = Universe.get();
            int seconds = (int) RankConfigAPI.get(player, warmup).getTimer().seconds();

            if (seconds > 0) {
                player.addChatMessage(
                        StringUtils.color(
                                ServerUtilitiesLib.lang(player, "stand_still", seconds).appendText(" [" + seconds + "]"),
                                EnumChatFormatting.GOLD));
                universe.scheduleTask(
                        TimeType.MILLIS,
                        System.currentTimeMillis() + 1000L,
                        new TeleportTask(teleportType, player, this, seconds, seconds, pos, extraTask));
            } else {
                new TeleportTask(teleportType, player, this, 0, 0, pos, extraTask).execute(universe);
            }
        }
    }

    private static class TeleportTask implements IScheduledTask {

        private final EntityPlayerMP player;
        private final Timer timer;
        private final BlockDimPos startPos;
        private final Function<EntityPlayerMP, TeleporterDimPos> pos;
        private final float startHP;
        private final int startSeconds, secondsLeft;
        private final IScheduledTask extraTask;
        private final TeleportType teleportType;

        private TeleportTask(TeleportType teleportType, EntityPlayerMP p, Timer t, int ss, int s,
                Function<EntityPlayerMP, TeleporterDimPos> to, @Nullable IScheduledTask e) {
            this.teleportType = teleportType;
            this.player = p;
            this.timer = t;
            this.startPos = new BlockDimPos((Entity) player);
            this.startHP = player.getHealth();
            this.pos = to;
            this.startSeconds = ss;
            this.secondsLeft = s;
            this.extraTask = e;
        }

        @Override
        public void execute(Universe universe) {
            if (!startPos.equalsPos(new BlockDimPos((Entity) player)) || startHP > player.getHealth()) {
                player.addChatMessage(
                        StringUtils.color(ServerUtilitiesLib.lang(player, "stand_still_failed"), EnumChatFormatting.RED));
            } else if (secondsLeft <= 1) {
                TeleporterDimPos teleporter = pos.apply(player);

                if (teleporter != null) {
                    ServerUtilitiesPlayerData data = get(universe.getPlayer(player));
                    data.setLastTeleport(teleportType, new BlockDimPos((Entity) player));
                    teleporter.teleport(player);

                    if (player.ridingEntity != null) {
                        teleporter.teleport(player.ridingEntity);
                    }

                    data.lastTeleport[timer.ordinal()] = System.currentTimeMillis();

                    if (secondsLeft != 0) {
                        player.addChatMessage(ServerUtilitiesLib.lang(player, "teleporting"));
                    }

                    if (extraTask != null) {
                        extraTask.execute(universe);
                    }
                }
            } else {
                universe.scheduleTask(
                        TimeType.MILLIS,
                        System.currentTimeMillis() + 1000L,
                        new TeleportTask(teleportType, player, timer, startSeconds, secondsLeft - 1, pos, extraTask));
                player.addChatMessage(
                        StringUtils.color(
                                ServerUtilitiesLib.lang(player, "stand_still", startSeconds)
                                        .appendText(" [" + (secondsLeft - 1) + "]"),
                                EnumChatFormatting.GOLD));
            }
        }
    }

    public static ServerUtilitiesPlayerData get(ForgePlayer player) {
        return player.getData().get(ServerUtilities.MOD_ID);
    }

    private boolean renderBadge = true;
    private boolean disableGlobalBadge = false;
    private boolean enablePVP = true;
    private String nickname = "";
    private EnumMessageLocation afkMesageLocation = EnumMessageLocation.CHAT;

    public final Collection<ForgePlayer> tpaRequestsFrom;
    public long afkTime;
    private IChatComponent cachedNameForChat;

    private BlockDimPos lastSafePos;
    private final long[] lastTeleport;
    public final BlockDimPosStorage homes;
    private TeleportTracker teleportTracker;

    public ServerUtilitiesPlayerData(ForgePlayer player) {
        super(player);
        homes = new BlockDimPosStorage();
        tpaRequestsFrom = new HashSet<>();
        lastTeleport = new long[Timer.VALUES.length];
        teleportTracker = new TeleportTracker();
    }

    @Override
    public String getId() {
        return ServerUtilities.MOD_ID;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("RenderBadge", renderBadge);
        nbt.setBoolean("DisableGlobalBadges", disableGlobalBadge);
        nbt.setBoolean("EnablePVP", enablePVP);
        nbt.setTag("Homes", homes.serializeNBT());

        nbt.setString("Nickname", nickname);
        nbt.setString("AFK", EnumMessageLocation.NAME_MAP.getName(afkMesageLocation));
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        renderBadge = !nbt.hasKey("RenderBadge") || nbt.getBoolean("RenderBadge");
        disableGlobalBadge = nbt.getBoolean("DisableGlobalBadges");
        enablePVP = !nbt.hasKey("EnablePVP") || nbt.getBoolean("EnablePVP");
        homes.deserializeNBT(nbt.getCompoundTag("Homes"));
        teleportTracker = new TeleportTracker();
        teleportTracker.deserializeNBT(nbt.getCompoundTag("teleportTracker"));
        setLastDeath(BlockDimPos.fromIntArray(nbt.getIntArray("LastDeath")), 0);
        nickname = nbt.getString("Nickname");
        afkMesageLocation = EnumMessageLocation.NAME_MAP.get(nbt.getString("AFK"));
    }

    public void addConfig(ConfigGroup main) {
        ConfigGroup config = main.getGroup(ServerUtilities.MOD_ID);
        config.setDisplayName(new ChatComponentText(ServerUtilities.MOD_NAME));

        config.addBool("render_badge", () -> renderBadge, v -> renderBadge = v, true);
        config.addBool("disable_global_badge", () -> disableGlobalBadge, v -> disableGlobalBadge = v, false);
        config.addBool("enable_pvp", () -> enablePVP, v -> enablePVP = v, true);

        if (ServerUtilitiesConfig.commands.nick && player.hasPermission(ServerUtilitiesPermissions.CHAT_NICKNAME_SET)) {
            config.addString("nickname", () -> nickname, v -> nickname = v, "");
        }

        if (ServerUtilitiesConfig.afk.isEnabled(player.team.universe.server)) {
            config.addEnum("afk", () -> afkMesageLocation, v -> afkMesageLocation = v, EnumMessageLocation.NAME_MAP);
        }
    }

    public boolean renderBadge() {
        return renderBadge;
    }

    public boolean disableGlobalBadge() {
        return disableGlobalBadge;
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

    public EnumMessageLocation getAFKMessageLocation() {
        return afkMesageLocation;
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

    public void checkTeleportCooldown(ICommandSender sender, Timer timer) throws CommandException {
        long cooldown = lastTeleport[timer.ordinal()] + player.getRankConfig(timer.cooldown).getTimer().millis()
                - System.currentTimeMillis();

        if (cooldown > 0) {
            throw ServerUtilitiesLib.error(sender, "cant_use_now_cooldown", StringUtils.getTimeString(cooldown));
        }
    }

    @Override
    public void clearCache() {
        cachedNameForChat = null;

        EntityPlayerMP p = player.getNullablePlayer();

        if (p != null) {
            p.refreshDisplayName();
        }
    }

    public IChatComponent getNameForChat(EntityPlayerMP playerMP) {
        if (cachedNameForChat != null) {
            return cachedNameForChat.createCopy();
        }

        String text = player.getRankConfig(ServerUtilitiesPermissions.CHAT_NAME_FORMAT).getString();

        try {
            cachedNameForChat = TextComponentParser.parse(text, ServerUtilitiesLibCommon.chatFormattingSubstituteFunction(player));
        } catch (Exception ex) {
            String s = "Error parsing " + text + ": " + ex.getLocalizedMessage();
            ServerUtilities.LOGGER.error(s);
            cachedNameForChat = new ChatComponentText("BrokenFormatting");
            cachedNameForChat.getChatStyle().setColor(EnumChatFormatting.RED);
            cachedNameForChat.getChatStyle()
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(s)));
        }

        if (NBTUtils.getPersistedData(playerMP, false).getBoolean("recording")) {
            IChatComponent rec = new ChatComponentText("\u25A0 ");
            rec.getChatStyle().setColor(EnumChatFormatting.RED);
            cachedNameForChat = new ChatComponentText("").appendSibling(rec).appendSibling(cachedNameForChat);
        }

        cachedNameForChat.appendText(" ");
        return cachedNameForChat.createCopy();
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
}
