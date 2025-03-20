package serverutils.lib.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.events.player.ForgePlayerConfigEvent;
import serverutils.events.player.ForgePlayerConfigSavedEvent;
import serverutils.events.player.ForgePlayerDataEvent;
import serverutils.events.player.ForgePlayerLoggedInEvent;
import serverutils.events.player.ForgePlayerLoggedOutEvent;
import serverutils.events.team.ForgeTeamCreatedEvent;
import serverutils.events.team.ForgeTeamPlayerJoinedEvent;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.icon.PlayerHeadIcon;
import serverutils.lib.util.INBTSerializable;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.EnumPrivacyLevel;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.lib.util.permission.context.IContext;
import serverutils.lib.util.permission.context.PlayerContext;
import serverutils.lib.util.permission.context.WorldContext;
import serverutils.net.MessageSyncData;

public class ForgePlayer implements INBTSerializable<NBTTagCompound>, Comparable<ForgePlayer>, IConfigCallback {

    private static FakePlayer playerForStats;

    public GameProfile profile;
    private final NBTDataStorage dataStorage;
    public ForgeTeam team;
    private boolean hideTeamNotification;
    public NBTTagCompound cachedPlayerNBT;
    private ConfigGroup cachedConfig;
    public long lastTimeSeen;
    public boolean needsSaving;
    public EntityPlayerMP tempPlayer;

    public ForgePlayer(Universe u, GameProfile p) {
        profile = p;
        dataStorage = new NBTDataStorage();
        team = u.getTeam("");
        hideTeamNotification = false;
        new ForgePlayerDataEvent(this, dataStorage).post();
        needsSaving = false;
    }

    public ForgePlayer(Universe u, UUID id, String name) {
        this(u, new GameProfile(id, name));
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("HideTeamNotification", hideTeamNotification);
        nbt.setLong("LastTimeSeen", lastTimeSeen);
        nbt.setTag("Data", dataStorage.serializeNBT());
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        hideTeamNotification = nbt.getBoolean("HideTeamNotification");
        lastTimeSeen = nbt.getLong("LastTimeSeen");
        dataStorage.deserializeNBT(nbt.getCompoundTag("Data"));
    }

    public void clearCache() {
        cachedPlayerNBT = null;
        cachedConfig = null;
        dataStorage.clearCache();
    }

    public void markDirty() {
        needsSaving = true;
        team.universe.checkSaving = true;
    }

    public boolean hasTeam() {
        return team.isValid();
    }

    public GameProfile getProfile() {
        return profile;
    }

    public final UUID getId() {
        return profile.getId();
    }

    public final String getName() {
        return profile.getName();
    }

    public final String getDisplayNameString() {
        if (isOnline()) {
            try {
                return getPlayer().getDisplayName();
            } catch (Exception ignored) {}
        }

        return getName();
    }

    public final IChatComponent getDisplayName() {
        if (isOnline()) {
            try {
                return new ChatComponentText(getDisplayNameString());

            } catch (Exception ignored) {}
        }

        return new ChatComponentText(getName());
    }

    public EntityPlayerMP getCommandPlayer(ICommandSender sender) throws CommandException {
        if (!isOnline()) {
            throw ServerUtilities.error(sender, "player_must_be_online");
        }

        return getPlayer();
    }

    public NBTDataStorage getData() {
        return dataStorage;
    }

    public boolean equalsPlayer(@Nullable ForgePlayer player) {
        return player == this || (player != null && getId().equals(player.getId()));
    }

    public boolean equalsPlayer(@Nullable ICommandSender player) {
        return player instanceof EntityPlayerMP && ((EntityPlayerMP) player).getUniqueID().equals(getId());
    }

    @Override
    public final int compareTo(ForgePlayer o) {
        return StringUtils.IGNORE_CASE_COMPARATOR.compare(getDisplayNameString(), o.getDisplayNameString());
    }

    public final String toString() {
        return getName();
    }

    public final int hashCode() {
        return getId().hashCode();
    }

    public boolean equals(Object o) {
        return o == this || o instanceof ForgePlayer && equalsPlayer((ForgePlayer) o);
    }

    public boolean canInteract(@Nullable ForgePlayer owner, EnumPrivacyLevel level) {
        if (level == EnumPrivacyLevel.PUBLIC || owner == null) {
            return true;
        } else if (owner.equalsPlayer(this)) {
            return true;
        } else if (level == EnumPrivacyLevel.PRIVATE) {
            return false;
        } else if (level == EnumPrivacyLevel.TEAM) {
            return owner.team.isAlly(this);
        }

        return false;
    }

    public boolean isOnline() {
        return getNullablePlayer() != null;
    }

    @Nullable
    public EntityPlayerMP getNullablePlayer() {
        if (tempPlayer != null) {
            return tempPlayer;
        }

        return team.universe.server.getConfigurationManager().func_152612_a(getName());
    }

    public EntityPlayerMP getPlayer() {
        EntityPlayerMP p = getNullablePlayer();

        if (p == null) {
            throw new NullPointerException(getName() + " is not online!");
        }

        return p;
    }

    public boolean isFake() {
        return tempPlayer instanceof FakePlayer;
    }

    public boolean isOP() {
        return ServerUtils.isOP(team.universe.server, getProfile());
    }

    void onLoggedIn(EntityPlayerMP player, Universe universe, boolean firstLogin) {
        tempPlayer = player;

        boolean sendTeamJoinEvent = false, sendTeamCreatedEvent = false;

        if (firstLogin && (ServerUtilitiesConfig.teams.disable_teams
                || (player.mcServer.isSinglePlayer() ? ServerUtilitiesConfig.teams.autocreate_sp
                        : ServerUtilitiesConfig.teams.autocreate_mp))) {
            if (player.mcServer.isSinglePlayer()) {
                team = universe.getTeam("singleplayer");

                if (!team.isValid()) {
                    team = new ForgeTeam(universe, (short) 2, "singleplayer", TeamType.SERVER);
                    team.setFreeToJoin(true);
                    universe.addTeam(team);
                    team.setTitle(getName());
                    team.setIcon(new PlayerHeadIcon(getId()).toString());
                    team.setColor(EnumTeamColor.NAME_MAP.getRandom(universe.world.rand));
                    team.markDirty();
                    sendTeamCreatedEvent = true;
                }

                sendTeamJoinEvent = true;
            } else {
                String id = getName().toLowerCase();

                if (universe.getTeam(id).isValid()) {
                    id = StringUtils.fromUUID(getId());
                }

                if (!universe.getTeam(id).isValid()) {
                    team = new ForgeTeam(universe, universe.generateTeamUID((short) 0), id, TeamType.PLAYER);
                    team.owner = this;
                    universe.addTeam(team);
                    team.setColor(EnumTeamColor.NAME_MAP.getRandom(universe.world.rand));
                    team.markDirty();
                    sendTeamCreatedEvent = true;
                    sendTeamJoinEvent = true;
                }
            }
        }

        if (!isFake()) {
            lastTimeSeen = universe.ticks.ticks();
            new MessageSyncData(true, player, this).sendTo(player);
        }

        new ForgePlayerLoggedInEvent(this).post();

        if (sendTeamCreatedEvent) {
            new ForgeTeamCreatedEvent(team).post();
        }

        if (sendTeamJoinEvent) {
            ForgeTeamPlayerJoinedEvent event = new ForgeTeamPlayerJoinedEvent(this);
            event.post();

            if (event.getDisplayGui() != null) {
                event.getDisplayGui().run();
            }
        }

        if (!hideTeamNotification() && !hasTeam()) {
            IChatComponent b1 = ServerUtilities.lang(player, "click_here");
            b1.getChatStyle().setColor(EnumChatFormatting.GOLD);
            b1.getChatStyle().setChatClickEvent(
                    new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/serverutilities_simulate_button custom:serverutilities:my_team_gui"));
            b1.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ServerUtilities.lang(player, "sidebar_button.serverutilities.my_team")));
            IChatComponent b2 = ServerUtilities.lang(player, "click_here");
            b2.getChatStyle().setColor(EnumChatFormatting.GOLD);
            b2.getChatStyle().setChatClickEvent(
                    new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/my_settings " + ServerUtilities.MOD_ID + ".hide_team_notification toggle"));
            b2.getChatStyle().setChatHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ServerUtilities.lang(player, "serverutilities.lang.team.notification.hide")));
            player.addChatMessage(ServerUtilities.lang(player, "serverutilities.lang.team.notification", b1, b2));
        }

        universe.clearCache();
        tempPlayer = null;
        markDirty();
    }

    void onLoggedOut(EntityPlayerMP p) {
        tempPlayer = p;
        lastTimeSeen = p.worldObj.getTotalWorldTime();
        new ForgePlayerLoggedOutEvent(this).post();
        clearCache();
        tempPlayer = null;
        markDirty();
    }

    public StatisticsFile stats() {
        if (playerForStats == null) {
            playerForStats = new FakePlayer(team.universe.world, ServerUtils.FAKE_PLAYER_PROFILE);
        }

        playerForStats.setWorld(team.universe.world);
        playerForStats.entityUniqueID = getId();
        return team.universe.server.getConfigurationManager().func_152602_a(playerForStats);
    }

    public ConfigGroup getSettings() {
        if (cachedConfig == null) {
            cachedConfig = ConfigGroup.newGroup("player_config");
            cachedConfig.setDisplayName(new ChatComponentTranslation("player_config"));
            ForgePlayerConfigEvent event = new ForgePlayerConfigEvent(this, cachedConfig);
            event.post();

            ConfigGroup config = cachedConfig.getGroup(ServerUtilities.MOD_ID);
            config.setDisplayName(new ChatComponentText(ServerUtilities.MOD_NAME));
            config.addBool("hide_team_notification", () -> hideTeamNotification, v -> hideTeamNotification = v, false);
        }

        return cachedConfig;
    }

    public NBTTagCompound getPlayerNBT() {
        if (isOnline()) {
            NBTTagCompound nbt = new NBTTagCompound();
            getPlayer().writeToNBT(nbt);
            return nbt;
        }

        if (cachedPlayerNBT == null) {
            try (InputStream stream = new FileInputStream(
                    new File(team.universe.getWorldDirectory(), "playerdata/" + getId() + ".dat"))) {
                cachedPlayerNBT = CompressedStreamTools.readCompressed(stream);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (cachedPlayerNBT == null) {
            cachedPlayerNBT = new NBTTagCompound();
        }

        return cachedPlayerNBT;
    }

    public void setPlayerNBT(NBTTagCompound nbt) {
        if (isOnline()) {
            EntityPlayerMP player = getPlayer();
            player.readEntityFromNBT(nbt);

            if (player.isEntityAlive()) {
                player.worldObj.updateEntityWithOptionalForce(player, true);
            }
        } else {
            try (FileOutputStream stream = new FileOutputStream(
                    new File(team.universe.getWorldDirectory(), "playerdata/" + getId() + ".dat"))) {
                CompressedStreamTools.writeCompressed(nbt, stream);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        cachedPlayerNBT = nbt;
        markDirty();
    }

    public boolean hideTeamNotification() {
        return ServerUtilitiesConfig.teams.hide_team_notification || hideTeamNotification || isFake();
    }

    public long getLastTimeSeen() {
        return isOnline() ? team.universe.ticks.ticks() : lastTimeSeen;
    }

    public boolean hasPermission(String node, @Nullable IContext context) {
        return PermissionAPI.hasPermission(getProfile(), node, context);
    }

    public IContext getContext() {
        if (isOnline()) {
            return new PlayerContext(getPlayer());
        }

        return new WorldContext(team.universe.world);
    }

    public boolean hasPermission(String node) {
        return PermissionAPI.hasPermission(getProfile(), node, getContext());
    }

    public ConfigValue getRankConfig(String node) {
        return RankConfigAPI.get(team.universe.server, getProfile(), node);
    }

    public File getDataFile() {
        File dir = new File(team.universe.dataFolder, "players/");
        return new File(dir, getName().toLowerCase() + ".dat");
    }

    @Override
    public void onConfigSaved(ConfigGroup group, ICommandSender sender) {
        clearCache();
        markDirty();
        new ForgePlayerConfigSavedEvent(this, group, sender).post();
    }

    public boolean isVanished() {
        EntityPlayerMP player = getNullablePlayer();
        if (player == null) return false;
        return ServerUtils.isVanished(player);
    }
}
