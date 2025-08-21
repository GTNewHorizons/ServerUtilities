package serverutils.lib.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.Constants;

import serverutils.ServerUtilities;
import serverutils.data.ClaimedChunk;
import serverutils.events.team.ForgeTeamConfigEvent;
import serverutils.events.team.ForgeTeamConfigSavedEvent;
import serverutils.events.team.ForgeTeamDataEvent;
import serverutils.events.team.ForgeTeamOwnerChangedEvent;
import serverutils.events.team.ForgeTeamPlayerJoinedEvent;
import serverutils.events.team.ForgeTeamPlayerLeftEvent;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.PlayerHeadIcon;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FinalIDObject;
import serverutils.lib.util.INBTSerializable;
import serverutils.lib.util.StringUtils;

public class ForgeTeam extends FinalIDObject implements INBTSerializable<NBTTagCompound>, IConfigCallback {

    public static final int MAX_TEAM_ID_LENGTH = 35;
    public static final Pattern TEAM_ID_PATTERN = Pattern.compile("^[a-z0-9_]{1," + MAX_TEAM_ID_LENGTH + "}$");

    private final short uid;
    public final Universe universe;
    public final TeamType type;
    public ForgePlayer owner;
    private final NBTDataStorage dataStorage;
    private String title;
    private String desc;
    private EnumTeamColor color;
    private String icon;
    private boolean freeToJoin;
    private EnumTeamStatus fakePlayerStatus;
    private final Collection<ForgePlayer> requestingInvite;
    public final Map<ForgePlayer, EnumTeamStatus> players;
    private ConfigGroup cachedConfig;
    private IChatComponent cachedTitle;
    private Icon cachedIcon;
    public boolean needsSaving;
    private long lastActivity;
    public final Set<ClaimedChunk> claimedChunks = new HashSet<>();

    public ForgeTeam(Universe u, short id, String n, TeamType t) {
        super(n, t.isNone ? 0 : (StringUtils.FLAG_ID_DEFAULTS | StringUtils.FLAG_ID_ALLOW_EMPTY));
        uid = id;
        universe = u;
        type = t;
        title = "";
        desc = "";
        color = EnumTeamColor.BLUE;
        icon = "";
        freeToJoin = false;
        fakePlayerStatus = EnumTeamStatus.ALLY;
        requestingInvite = new HashSet<>();
        players = new HashMap<>();
        dataStorage = new NBTDataStorage();
        new ForgeTeamDataEvent(this, dataStorage).post();
        clearCache();
        cachedIcon = null;
        needsSaving = false;
        lastActivity = 0L;
    }

    public final short getUID() {
        return uid;
    }

    public final int hashCode() {
        return uid;
    }

    public final boolean equals(Object o) {
        return o == this || uid == Objects.hashCode(o);
    }

    public final String getUIDCode() {
        return String.format("%04X", uid);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        if (owner != null) {
            nbt.setString("Owner", owner.getName());
        }

        nbt.setString("Title", title);
        nbt.setString("Desc", desc);
        nbt.setString("Color", EnumTeamColor.NAME_MAP.getName(color));
        nbt.setString("Icon", icon);
        nbt.setBoolean("FreeToJoin", freeToJoin);
        nbt.setString("FakePlayerStatus", EnumTeamStatus.NAME_MAP_PERMS.getName(fakePlayerStatus));
        nbt.setLong("LastActivity", lastActivity);

        NBTTagCompound nbt1 = new NBTTagCompound();

        if (!players.isEmpty()) {
            for (Map.Entry<ForgePlayer, EnumTeamStatus> entry : players.entrySet()) {
                nbt1.setString(entry.getKey().getName(), entry.getValue().getName());
            }
        }

        nbt.setTag("Players", nbt1);

        NBTTagList list = new NBTTagList();

        for (ForgePlayer player : requestingInvite) {
            list.appendTag(new NBTTagString(player.getName()));
        }

        nbt.setTag("RequestingInvite", list);
        nbt.setTag("Data", dataStorage.serializeNBT());
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        owner = universe.getPlayer(nbt.getString("Owner"));

        if (!isValid()) {
            return;
        }

        title = nbt.getString("Title");
        desc = nbt.getString("Desc");
        color = EnumTeamColor.NAME_MAP.get(nbt.getString("Color"));
        icon = nbt.getString("Icon");
        freeToJoin = nbt.getBoolean("FreeToJoin");
        fakePlayerStatus = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("FakePlayerStatus"));
        lastActivity = nbt.getLong("LastActivity");

        players.clear();

        if (nbt.hasKey("Players")) {
            NBTTagCompound nbt1 = nbt.getCompoundTag("Players");

            for (String s : nbt1.func_150296_c()) {
                ForgePlayer player = universe.getPlayer(s);

                if (player != null) {
                    EnumTeamStatus status = EnumTeamStatus.NAME_MAP.get(nbt1.getString(s));

                    if (status.canBeSet()) {
                        setStatus(player, status);
                    }
                }
            }
        }

        NBTTagList list = nbt.getTagList("RequestingInvite", Constants.NBT.TAG_STRING);

        for (int i = 0; i < list.tagCount(); i++) {
            ForgePlayer player = universe.getPlayer(list.getStringTagAt(i));

            if (player != null && !isMember(player)) {
                setRequestingInvite(player, true);
            }
        }

        list = nbt.getTagList("Invited", Constants.NBT.TAG_STRING);

        for (int i = 0; i < list.tagCount(); i++) {
            ForgePlayer player = universe.getPlayer(list.getStringTagAt(i));

            if (player != null && !isMember(player)) {
                setStatus(player, EnumTeamStatus.INVITED);
            }
        }

        dataStorage.deserializeNBT(nbt.getCompoundTag("Data"));
    }

    public void clearCache() {
        cachedTitle = null;
        cachedIcon = null;
        cachedConfig = null;
        dataStorage.clearCache();
    }

    public void markDirty() {
        needsSaving = true;
        universe.checkSaving = true;
    }

    public NBTDataStorage getData() {
        return dataStorage;
    }

    @Nullable
    public ForgePlayer getOwner() {
        return type.isPlayer ? owner : null;
    }

    public IChatComponent getTitle() {
        if (cachedTitle != null) {
            return cachedTitle;
        }

        if (title.isEmpty()) {
            cachedTitle = getOwner() != null ? getOwner().getDisplayName().appendText("'s Team")
                    : new ChatComponentTranslation("serverutilities.lang.team.no_team");
        } else {
            cachedTitle = new ChatComponentText(title);
        }

        cachedTitle = StringUtils.color(cachedTitle, getColor().getEnumChatFormatting());
        return cachedTitle;
    }

    public IChatComponent getCommandTitle() {
        IChatComponent component = getTitle().createCopy();

        if (!isValid()) {
            return component;
        }

        component.getChatStyle().setChatHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("/team info " + getId())));
        component.getChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team info " + getId()));
        component.getChatStyle().setColor(getColor().getEnumChatFormatting());
        return component;
    }

    public void setTitle(String s) {
        if (!title.equals(s)) {
            title = s;
            markDirty();
        }
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String s) {
        if (!desc.equals(s)) {
            desc = s;
            markDirty();
        }
    }

    public EnumTeamColor getColor() {
        return color;
    }

    public void setColor(EnumTeamColor col) {
        if (color != col) {
            color = col;
            markDirty();
        }
    }

    public Icon getIcon() {
        if (cachedIcon == null) {
            if (icon.isEmpty()) {
                if (getOwner() != null) {
                    cachedIcon = new PlayerHeadIcon(getOwner().getProfile().getId());
                } else {
                    cachedIcon = getColor().getColor();
                }
            } else {
                cachedIcon = Icon.getIcon(icon);
            }
        }

        return cachedIcon;
    }

    public void setIcon(String s) {
        if (!icon.equals(s)) {
            icon = s;
            markDirty();
        }
    }

    public boolean isFreeToJoin() {
        return freeToJoin;
    }

    public void setFreeToJoin(boolean b) {
        if (freeToJoin != b) {
            freeToJoin = b;
            markDirty();
        }
    }

    public EnumTeamStatus getFakePlayerStatus(ForgePlayer player) {
        return fakePlayerStatus;
    }

    public EnumTeamStatus getHighestStatus(@Nullable ForgePlayer player) {
        if (player == null) {
            return EnumTeamStatus.NONE;
        } else if (player.isFake()) {
            return fakePlayerStatus;
        } else if (isOwner(player)) {
            return EnumTeamStatus.OWNER;
        } else if (isModerator(player)) {
            return EnumTeamStatus.MOD;
        } else if (isMember(player)) {
            return EnumTeamStatus.MEMBER;
        } else if (isEnemy(player)) {
            return EnumTeamStatus.ENEMY;
        } else if (isAlly(player)) {
            return EnumTeamStatus.ALLY;
        } else if (isInvited(player)) {
            return EnumTeamStatus.INVITED;
        }

        return EnumTeamStatus.NONE;
    }

    private EnumTeamStatus getSetStatus(@Nullable ForgePlayer player) {
        if (player == null || !isValid()) {
            return EnumTeamStatus.NONE;
        } else if (player.isFake()) {
            return fakePlayerStatus;
        } else if (type == TeamType.SERVER && getId().equals("singleplayer")) {
            return EnumTeamStatus.MOD;
        }

        EnumTeamStatus status = players.get(player);
        return status == null ? EnumTeamStatus.NONE : status;
    }

    public boolean hasStatus(@Nullable ForgePlayer player, EnumTeamStatus status) {
        if (player == null || !isValid()) {
            return false;
        }

        if (player.isFake()) {
            return getFakePlayerStatus(player).isEqualOrGreaterThan(status);
        }

        return switch (status) {
            case NONE -> true;
            case ENEMY -> isEnemy(player);
            case ALLY -> isAlly(player);
            case INVITED -> isInvited(player);
            case MEMBER -> isMember(player);
            case MOD -> isModerator(player);
            case OWNER -> isOwner(player);
            default -> false;
        };
    }

    public boolean setStatus(@Nullable ForgePlayer player, EnumTeamStatus status) {
        if (player == null || !isValid() || player.isFake()) {
            return false;
        } else if (status == EnumTeamStatus.OWNER) {
            if (!isMember(player)) {
                return false;
            }

            if (!player.equalsPlayer(getOwner())) {
                universe.clearCache();
                ForgePlayer oldOwner = getOwner();
                owner = player;
                players.remove(player);
                new ForgeTeamOwnerChangedEvent(this, oldOwner).post();

                if (oldOwner != null) {
                    oldOwner.markDirty();
                }

                owner.markDirty();
                markDirty();
                return true;
            }

            return false;
        } else if (!status.isNone() && status.canBeSet()) {
            if (players.put(player, status) != status) {
                universe.clearCache();
                player.markDirty();
                markDirty();
                return true;
            }
        } else if (players.remove(player) != status) {
            universe.clearCache();
            player.markDirty();
            markDirty();
            return true;
        }

        return false;
    }

    public <C extends Collection<ForgePlayer>> C getPlayersWithStatus(C collection, EnumTeamStatus status) {
        if (!isValid()) {
            return collection;
        }

        for (ForgePlayer player : universe.getPlayers()) {
            if (!player.isFake() && hasStatus(player, status)) {
                collection.add(player);
            }
        }

        return collection;
    }

    public List<ForgePlayer> getPlayersWithStatus(EnumTeamStatus status) {
        return isValid() ? getPlayersWithStatus(new ArrayList<>(), status) : Collections.emptyList();
    }

    public boolean addMember(ForgePlayer player, boolean simulate) {
        if (isValid() && ((isOwner(player) || isInvited(player)) && !isMember(player))) {
            if (!simulate) {
                universe.clearCache();
                player.team = this;
                players.remove(player);
                requestingInvite.remove(player);

                ForgeTeamPlayerJoinedEvent event = new ForgeTeamPlayerJoinedEvent(player);
                event.post();

                if (event.getDisplayGui() != null) {
                    event.getDisplayGui().run();
                }

                player.markDirty();
                markDirty();
            }

            return true;
        }

        return false;
    }

    public boolean removeMember(ForgePlayer player) {
        if (!isValid() || !isMember(player)) {
            return false;
        } else if (getMembers().size() == 1) {
            universe.clearCache();
            new ForgeTeamPlayerLeftEvent(player).post();

            if (type.isPlayer) {
                delete();
            } else {
                setStatus(player, EnumTeamStatus.NONE);
            }

            player.team = universe.getTeam("");
            player.markDirty();
            markDirty();
        } else if (isOwner(player)) {
            return false;
        }

        universe.clearCache();
        new ForgeTeamPlayerLeftEvent(player).post();
        player.team = universe.getTeam("");
        setStatus(player, EnumTeamStatus.NONE);
        player.markDirty();
        markDirty();
        return true;
    }

    public void delete() {
        universe.removeTeam(this);
    }

    public List<ForgePlayer> getMembers() {
        return getPlayersWithStatus(EnumTeamStatus.MEMBER);
    }

    public boolean isMember(@Nullable ForgePlayer player) {
        if (player == null) {
            return false;
        } else if (player.isFake()) {
            return fakePlayerStatus.isEqualOrGreaterThan(EnumTeamStatus.MEMBER);
        }

        return isValid() && equalsTeam(player.team);
    }

    public boolean isAlly(@Nullable ForgePlayer player) {
        return isValid() && (isMember(player) || getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.ALLY));
    }

    public boolean isInvited(@Nullable ForgePlayer player) {
        return isValid() && (isMember(player)
                || ((isFreeToJoin() || getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.INVITED))
                        && !isEnemy(player)));
    }

    public boolean setRequestingInvite(@Nullable ForgePlayer player, boolean value) {
        if (player != null && isValid()) {
            if (value) {
                if (requestingInvite.add(player)) {
                    player.markDirty();
                    markDirty();
                    return true;
                }
            } else if (requestingInvite.remove(player)) {
                player.markDirty();
                markDirty();
                return true;
            }

            return false;
        }

        return false;
    }

    public boolean isRequestingInvite(@Nullable ForgePlayer player) {
        return player != null && isValid()
                && !isMember(player)
                && requestingInvite.contains(player)
                && !isEnemy(player);
    }

    public boolean isEnemy(@Nullable ForgePlayer player) {
        return getSetStatus(player) == EnumTeamStatus.ENEMY;
    }

    public boolean isModerator(@Nullable ForgePlayer player) {
        return isOwner(player) || isMember(player) && getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.MOD);
    }

    public boolean isOwner(@Nullable ForgePlayer player) {
        return player != null && player.equalsPlayer(getOwner());
    }

    public ConfigGroup getSettings() {
        if (cachedConfig == null) {
            cachedConfig = ConfigGroup.newGroup("team_config");
            cachedConfig.setDisplayName(
                    new ChatComponentTranslation("gui.settings").appendSibling(
                            StringUtils.bold(
                                    StringUtils
                                            .color(new ChatComponentText(" #" + getId()), EnumChatFormatting.DARK_GRAY),
                                    false)));
            ForgeTeamConfigEvent event = new ForgeTeamConfigEvent(this, cachedConfig);
            event.post();

            ConfigGroup main = cachedConfig.getGroup(ServerUtilities.MOD_ID);
            main.setDisplayName(new ChatComponentText(ServerUtilities.MOD_NAME));
            main.addBool("free_to_join", () -> freeToJoin, v -> freeToJoin = v, false);

            ConfigGroup display = main.getGroup("display");
            display.addEnum("color", () -> color, v -> color = v, EnumTeamColor.NAME_MAP);
            display.addEnum(
                    "fake_player_status",
                    () -> fakePlayerStatus,
                    v -> fakePlayerStatus = v,
                    EnumTeamStatus.NAME_MAP_PERMS);
            display.addString("title", () -> title, v -> title = v, "");
            display.addString("desc", () -> desc, v -> desc = v, "");
        }

        return cachedConfig;
    }

    public boolean isValid() {
        if (type.isNone) {
            return false;
        }

        return type.isServer || getOwner() != null;
    }

    public boolean equalsTeam(@Nullable ForgeTeam team) {
        return team == this || uid == Objects.hashCode(team);
    }

    public boolean anyPlayerHasPermission(String permission, EnumTeamStatus status) {
        for (ForgePlayer player : getPlayersWithStatus(status)) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    public boolean anyMemberHasPermission(String permission) {
        return anyPlayerHasPermission(permission, EnumTeamStatus.MEMBER);
    }

    public File getDataFile(String ext) {
        File dir = new File(universe.dataFolder, "teams/");

        if (ext.isEmpty()) {
            return new File(dir, getId() + ".dat");
        }

        File extFolder = new File(dir, ext);

        if (!extFolder.exists()) {
            extFolder.mkdirs();
        }

        File extFile = new File(extFolder, getId() + ".dat");

        if (!extFile.exists()) {
            File oldExtFile = new File(dir, getId() + "." + ext + ".dat");

            if (oldExtFile.exists()) {
                oldExtFile.renameTo(extFile);
                oldExtFile.deleteOnExit();
            }
        }

        return extFile;
    }

    @Override
    public void onConfigSaved(ConfigGroup group, ICommandSender sender) {
        clearCache();
        markDirty();
        new ForgeTeamConfigSavedEvent(this, group, sender).post();
    }

    public List<EntityPlayerMP> getOnlineMembers() {
        List<EntityPlayerMP> list = new ArrayList<>();

        for (ForgePlayer player : getMembers()) {
            EntityPlayerMP p = player.getNullablePlayer();

            if (p != null) {
                list.add(p);
            }
        }

        return list;
    }

    public long getLastActivity() {
        if (lastActivity == 0) {
            long latestActivity = 0;
            for (ForgePlayer player : getMembers()) {
                latestActivity = Math.max(player.getLastTimeSeen(), latestActivity);
            }
            lastActivity = System.currentTimeMillis() - Ticks.get(universe.ticks.ticks() - latestActivity).millis();
            markDirty();
        }
        return lastActivity;
    }

    public void refreshActivity() {
        lastActivity = System.currentTimeMillis();
        markDirty();
    }

    public Ticks getHighestTimer(String node) {
        Ticks highest = Ticks.NO_TICKS;
        for (ForgePlayer player : getMembers()) {
            Ticks ticks = player.getRankConfig(node).getTimer();
            if (ticks.millis() == Ticks.NO_TICKS.millis()) {
                return ticks;
            }

            if (ticks.millis() > highest.millis()) {
                highest = ticks;
            }
        }

        return highest;
    }
}
