package serverutils.lib.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.BackwardsCompat;
import serverutils.events.ServerReloadEvent;
import serverutils.events.player.ForgePlayerLoadedEvent;
import serverutils.events.player.ForgePlayerSavedEvent;
import serverutils.events.team.ForgeTeamDeletedEvent;
import serverutils.events.team.ForgeTeamLoadedEvent;
import serverutils.events.team.ForgeTeamSavedEvent;
import serverutils.events.universe.UniverseClearCacheEvent;
import serverutils.events.universe.UniverseClosedEvent;
import serverutils.events.universe.UniverseLoadedEvent;
import serverutils.events.universe.UniverseSavedEvent;
import serverutils.lib.ATHelper;
import serverutils.lib.EnumReloadType;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.io.DataReader;
import serverutils.lib.math.MathUtils;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.task.Task;

public class Universe {

    private static final HashSet<UUID> LOGGED_IN_PLAYERS = new HashSet<>(); // Required because of a Forge bug
    // https://github.com/MinecraftForge/MinecraftForge/issues/5696
    private static Universe INSTANCE = null;

    public static boolean loaded() {
        return INSTANCE != null;
    }

    public static Universe get() {
        if (INSTANCE == null) {
            throw new NullPointerException("ServerUtilities Universe == null!");
        }

        return INSTANCE;
    }

    public static @Nullable Universe getNullable() {
        return INSTANCE;
    }

    // Event handlers start //

    public static void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        INSTANCE = new Universe(event.getServer());
    }

    public static void onServerStarted(FMLServerStartedEvent event) {
        INSTANCE.world = INSTANCE.server.worldServers[0];
        INSTANCE.ticks = Ticks.get(INSTANCE.world.getTotalWorldTime());
        INSTANCE.load();
    }

    public static void onServerStopping(FMLServerStoppingEvent event) {
        if (loaded()) {
            for (ForgePlayer player : INSTANCE.getPlayers()) {
                if (player.isOnline()) {
                    player.onLoggedOut(player.getPlayer());
                }
            }

            LOGGED_IN_PLAYERS.clear();
            INSTANCE.save();
            new UniverseClosedEvent(INSTANCE).post();
            INSTANCE = null;
        }
    }

    @SubscribeEvent
    public void onWorldSaved(WorldEvent.Save event) {
        if (loaded()) {
            INSTANCE.save();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (loaded() && event.player instanceof EntityPlayerMP playerMP && !ServerUtils.isFake(playerMP)) {
            LOGGED_IN_PLAYERS.add(playerMP.getUniqueID());
            INSTANCE.onPlayerLoggedIn(playerMP);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (loaded() && event.player instanceof EntityPlayerMP playerMP
                && LOGGED_IN_PLAYERS.remove(playerMP.getUniqueID())) {
            ForgePlayer p = INSTANCE.getPlayer(playerMP.getGameProfile());

            if (p != null) {
                vanishedPlayers.remove(p);
                p.onLoggedOut(playerMP);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.entity instanceof EntityPlayerMP playerMP) {
            ForgePlayer p = INSTANCE.getPlayer(playerMP.getGameProfile());

            if (p != null) {
                p.tempPlayer = playerMP;
            }

            INSTANCE.clearCache();

            if (p != null) {
                p.tempPlayer = null;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onTickEvent(TickEvent.WorldTickEvent event) {
        if (!loaded()) {
            return;
        }

        Universe universe = get();

        if (event.phase == TickEvent.Phase.START) {
            universe.ticks = Ticks.get(event.world.getTotalWorldTime());
        } else if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            universe.taskList.addAll(universe.taskQueue);
            universe.taskQueue.clear();

            Iterator<Task> taskIterator = universe.taskList.iterator();

            while (taskIterator.hasNext()) {
                Task task = taskIterator.next();
                if (task.isComplete(universe)) {
                    task.execute(universe);

                    if (task.isRepeatable()) {
                        task.setNextTime(System.currentTimeMillis() + task.getInterval());
                        continue;
                    }
                    taskIterator.remove();
                }
            }

            if (universe.server.isSinglePlayer()) {
                boolean cheats = ATHelper.areCommandsAllowedForAll(universe.server.getConfigurationManager());

                if (universe.prevCheats != cheats) {
                    universe.prevCheats = cheats;
                    universe.clearCache();
                }
            }
        }
    }

    // Event handler end //

    public final MinecraftServer server;
    public WorldServer world;
    public final Map<UUID, ForgePlayer> players;
    public final Set<ForgePlayer> vanishedPlayers;
    private final Map<String, ForgeTeam> teams;
    private final Map<Short, ForgeTeam> teamMap;
    private final ForgeTeam noneTeam;
    private UUID uuid;
    private boolean needsSaving;
    boolean checkSaving;
    public ForgeTeam fakePlayerTeam;
    public FakeForgePlayer fakePlayer;
    private final List<Task> taskList;
    private final List<Task> taskQueue;
    public Ticks ticks;
    private boolean prevCheats = false;
    public File dataFolder;
    public File latModFolder;

    public Universe(MinecraftServer s) {
        server = s;
        ticks = Ticks.NO_TICKS;
        players = new HashMap<>();
        vanishedPlayers = new ObjectOpenHashSet<>();
        teams = new HashMap<>();
        teamMap = new HashMap<>();
        noneTeam = new ForgeTeam(this, (short) 0, "", TeamType.NONE);
        uuid = null;
        needsSaving = false;
        checkSaving = true;
        taskList = new ArrayList<>();
        taskQueue = new ArrayList<>();
    }

    public void markDirty() {
        needsSaving = true;
        checkSaving = true;
    }

    public UUID getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
            markDirty();
        }

        return uuid;
    }

    public void scheduleTask(Task task) {
        scheduleTask(task, true);
    }

    public void scheduleTask(Task task, boolean condition) {
        if (!condition) return;
        if (task.getNextTime() <= -1) return;
        task.queueNotifications(this);
        taskQueue.add(task);
    }

    private void load() {
        dataFolder = new File(getWorldDirectory(), "serverutilities/");
        latModFolder = new File(getWorldDirectory(), "LatMod");
        NBTTagCompound universeData = NBTUtils.readNBT(new File(dataFolder, "universe.dat"));

        if (universeData == null) {
            universeData = new NBTTagCompound();
        }

        File worldDataJsonFile = new File(getWorldDirectory(), "world_data.json");
        JsonElement worldData = DataReader.get(worldDataJsonFile).safeJson();

        if (worldData.isJsonObject()) {
            JsonObject jsonWorldData = worldData.getAsJsonObject();

            if (jsonWorldData.has("world_id")) {
                universeData.setString("UUID", jsonWorldData.get("world_id").getAsString());
            }

            worldDataJsonFile.delete();
        }

        uuid = StringUtils.fromString(universeData.getString("UUID"));

        if (uuid != null && uuid.getLeastSignificantBits() == 0L && uuid.getMostSignificantBits() == 0L) {
            uuid = null;
        }

        NBTTagCompound data = universeData.getCompoundTag("Data");

        new UniverseLoadedEvent.Pre(this, data).post();

        Map<UUID, NBTTagCompound> playerNBT = new HashMap<>();
        Map<String, NBTTagCompound> teamNBT = new HashMap<>();

        try {
            File[] files = new File(dataFolder, "players").listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".dat")
                            && file.getName().indexOf('.') == file.getName().lastIndexOf('.')) {
                        NBTTagCompound nbt = NBTUtils.readNBT(file);

                        if (nbt != null) {
                            String uuidString = nbt.getString("UUID");

                            if (uuidString.isEmpty()) {
                                uuidString = FileUtils.getBaseName(file);
                                FileUtils.deleteSafe(file);
                            }

                            UUID uuid = StringUtils.fromString(uuidString);

                            if (uuid != null) {
                                playerNBT.put(uuid, nbt);
                                ForgePlayer player = new ForgePlayer(this, uuid, nbt.getString("Name"));
                                players.put(uuid, player);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            File[] files = new File(dataFolder, "teams").listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".dat")
                            && file.getName().indexOf('.') == file.getName().lastIndexOf('.')) {
                        NBTTagCompound nbt = NBTUtils.readNBT(file);

                        if (nbt != null) {
                            String s = nbt.getString("ID");

                            if (s.isEmpty()) {
                                s = FileUtils.getBaseName(file);
                            }

                            teamNBT.put(s, nbt);
                            short uid = nbt.getShort("UID");
                            ForgeTeam team = new ForgeTeam(
                                    this,
                                    generateTeamUID(uid),
                                    s,
                                    TeamType.NAME_MAP.get(nbt.getString("Type")));
                            addTeam(team);

                            if (uid == 0) {
                                team.markDirty();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        fakePlayerTeam = new ForgeTeam(this, (short) 1, "fakeplayer", TeamType.SERVER_NO_SAVE) {

            @Override
            public void markDirty() {
                Universe.this.markDirty();
            }
        };

        fakePlayer = new FakeForgePlayer(this);
        fakePlayer.team = fakePlayerTeam;
        fakePlayerTeam.setColor(EnumTeamColor.GRAY);

        new UniverseLoadedEvent.CreateServerTeams(this).post();

        for (ForgePlayer player : players.values()) {
            NBTTagCompound nbt = playerNBT.get(player.getId());

            if (nbt != null && !nbt.hasNoTags()) {
                player.team = getTeam(nbt.getString("TeamID"));
                player.deserializeNBT(nbt);
            }

            new ForgePlayerLoadedEvent(player).post();
        }

        for (ForgeTeam team : getTeams()) {
            if (!team.type.save) {
                continue;
            }

            NBTTagCompound nbt = teamNBT.get(team.getId());

            if (nbt != null && !nbt.hasNoTags()) {
                team.deserializeNBT(nbt);
            }

            new ForgeTeamLoadedEvent(team).post();
        }

        if (universeData.hasKey("FakePlayer")) {
            fakePlayer.deserializeNBT(universeData.getCompoundTag("FakePlayer"));
        }

        if (universeData.hasKey("FakeTeam")) {
            fakePlayerTeam.deserializeNBT(universeData.getCompoundTag("FakeTeam"));
        }

        fakePlayerTeam.owner = fakePlayer;

        new UniverseLoadedEvent.Post(this, data).post();

        if (shouldLoadLatmod()) {
            BackwardsCompat.load();
        }

        new UniverseLoadedEvent.Finished(this).post();

        ServerUtilitiesAPI.reloadServer(this, server, EnumReloadType.CREATED, ServerReloadEvent.ALL);
    }

    private void save() {
        if (!checkSaving) {
            return;
        }

        if (needsSaving) {
            if (ServerUtilitiesConfig.debugging.print_more_info) {
                ServerUtilities.LOGGER.info("Saving universe data");
            }

            NBTTagCompound universeData = new NBTTagCompound();
            NBTTagCompound data = new NBTTagCompound();
            new UniverseSavedEvent(this, data).post();
            universeData.setTag("Data", data);
            universeData.setString("UUID", StringUtils.fromUUID(getUUID()));
            universeData.setTag("FakePlayer", fakePlayer.serializeNBT());
            universeData.setTag("FakeTeam", fakePlayerTeam.serializeNBT());
            NBTUtils.writeNBTSafe(new File(dataFolder, "universe.dat"), universeData);
            needsSaving = false;
        }

        for (ForgePlayer player : players.values()) {
            if (player.needsSaving) {
                if (ServerUtilitiesConfig.debugging.print_more_info) {
                    ServerUtilities.LOGGER.info("Saved player data for " + player.getName());
                }

                NBTTagCompound nbt = player.serializeNBT();
                nbt.setString("Name", player.getName());
                nbt.setString("UUID", StringUtils.fromUUID(player.getId()));
                nbt.setString("TeamID", player.team.getId());
                NBTUtils.writeNBTSafe(player.getDataFile(), nbt);
                new ForgePlayerSavedEvent(player).post();
                player.needsSaving = false;
            }
        }

        for (ForgeTeam team : getTeams()) {
            if (team.needsSaving) {
                if (ServerUtilitiesConfig.debugging.print_more_info) {
                    ServerUtilities.LOGGER.info("Saved team data for {}", team.getId());
                }

                File file = team.getDataFile("");

                if (team.type.save && team.isValid()) {
                    NBTTagCompound nbt = team.serializeNBT();
                    nbt.setString("ID", team.getId());
                    nbt.setShort("UID", team.getUID());
                    nbt.setString("Type", team.type.getName());
                    NBTUtils.writeNBTSafe(file, nbt);
                    new ForgeTeamSavedEvent(team).post();
                } else if (file.exists()) {
                    file.delete();
                }

                team.needsSaving = false;
            }
        }

        checkSaving = false;
    }

    public File getWorldDirectory() {
        return server.worldServers[0].getSaveHandler().getWorldDirectory();
    }

    private void onPlayerLoggedIn(EntityPlayerMP player) {
        if (!player.mcServer.getConfigurationManager().func_152607_e(player.getGameProfile())) { // canjoin
            return;
        }

        ForgePlayer p = getPlayer(player.getGameProfile());

        if (p == null) {
            p = new ForgePlayer(this, player.getUniqueID(), player.getCommandSenderName());
            players.put(p.getId(), p);
            p.onLoggedIn(player, this, true);
        } else {
            if (!p.getId().equals(player.getUniqueID()) || !p.getName().equals(player.getCommandSenderName())) {
                File old = p.getDataFile();
                players.remove(p.getId());
                p.profile = new GameProfile(player.getUniqueID(), player.getCommandSenderName());
                players.put(p.getId(), p);
                old.renameTo(p.getDataFile());
                p.markDirty();
                p.team.markDirty();
                markDirty();
            }

            if (ServerUtils.isVanished(player)) {
                vanishedPlayers.add(p);
                player.capabilities.disableDamage = true;
            }

            p.onLoggedIn(player, this, false);
        }
    }

    public Collection<ForgePlayer> getPlayers() {
        return players.values();
    }

    public Collection<ForgePlayer> getVanishedPlayers() {
        return vanishedPlayers;
    }

    @Nullable
    public ForgePlayer getPlayer(@Nullable UUID id) {
        if (id == null) {
            return null;
        } else if (id.equals(ServerUtils.FAKE_PLAYER_PROFILE.getId())) {
            return fakePlayer;
        }

        return players.get(id);
    }

    @Nullable
    public ForgePlayer getPlayer(CharSequence nameOrId) {
        String s = nameOrId.toString().toLowerCase();

        if (s.isEmpty()) {
            return null;
        }

        UUID id = StringUtils.fromString(s);

        if (id != null) {
            return getPlayer(id);
        } else if (s.equals(ServerUtils.FAKE_PLAYER_PROFILE.getName().toLowerCase())) {
            return fakePlayer;
        }

        for (ForgePlayer p : players.values()) {
            if (p.getName().toLowerCase().equals(s)) {
                return p;
            }
        }

        for (ForgePlayer p : players.values()) {
            if (p.getName().toLowerCase().contains(s)) {
                return p;
            }
        }

        return null;
    }

    public ForgePlayer getPlayer(@Nullable ICommandSender sender) {
        if (sender instanceof EntityPlayerMP player) {

            if (ServerUtils.isFake(player)) {
                fakePlayer.tempPlayer = player;
                fakePlayer.clearCache();
                return fakePlayer;
            }

            ForgePlayer p = getPlayer(player.getGameProfile());

            if (p == null) {
                throw new NullPointerException(
                        "Player can't be found for " + player.getCommandSenderName()
                                + ":"
                                + StringUtils.fromUUID(player.getUniqueID())
                                + ":"
                                + player.getClass().getName());
            }

            return p;
        }

        throw new IllegalArgumentException("Sender is not a player!");
    }

    public ForgePlayer getPlayer(ForgePlayer player) {
        ForgePlayer p = getPlayer(player.getId());
        return p == null ? player : p;
    }

    @Nullable
    public ForgePlayer getPlayer(GameProfile profile) {
        ForgePlayer player = getPlayer(profile.getId());

        if (player == null
                && ServerUtilitiesConfig.general.merge_offline_mode_players.get(!server.isDedicatedServer())) {
            player = getPlayer(profile.getName());

            if (player != null) {
                players.put(profile.getId(), player);
                player.markDirty();
            }
        }

        return player;
    }

    public Collection<ForgeTeam> getTeams() {
        return teams.values();
    }

    public ForgeTeam getTeam(String id) {
        if (id.isEmpty()) {
            return noneTeam;
        } else if (id.length() == 4) {
            try {
                ForgeTeam team = getTeam(Integer.valueOf(id, 16).shortValue());

                if (team.isValid()) {
                    return team;
                }
            } catch (Exception ex) {}
        }

        if (id.equals("fakeplayer")) {
            return fakePlayerTeam;
        }

        ForgeTeam team = teams.get(id);

        if (team != null) {
            return team;
        }

        ForgePlayer player = getPlayer(id);

        if (player != null) {
            return player.team;
        }

        return noneTeam;
    }

    public ForgeTeam getTeam(short uid) {
        if (uid == 0) {
            return noneTeam;
        } else if (uid == 1) {
            return fakePlayerTeam;
        }

        ForgeTeam team = teamMap.get(uid);
        return team == null ? noneTeam : team;
    }

    public Collection<ForgePlayer> getOnlinePlayers() {
        Collection<ForgePlayer> set = Collections.emptySet();

        for (ForgePlayer player : getPlayers()) {
            if (player.isOnline()) {
                if (set.isEmpty()) {
                    set = new HashSet<>();
                }

                set.add(player);
            }
        }

        return set;
    }

    public void clearCache() {
        new UniverseClearCacheEvent(this).post();
        getTeams().forEach(ForgeTeam::clearCache);
        getPlayers().forEach(ForgePlayer::clearCache);
        fakePlayer.clearCache();
    }

    public void addTeam(ForgeTeam team) {
        teamMap.put(team.getUID(), team);
        teams.put(team.getId(), team);
    }

    public void removeTeam(ForgeTeam team) {
        File folder = new File(dataFolder, "teams/");
        new ForgeTeamDeletedEvent(team, folder).post();
        teamMap.remove(team.getUID());
        teams.remove(team.getId());
        FileUtils.deleteSafe(new File(folder, team.getId() + ".dat"));
        markDirty();
        clearCache();
    }

    public short generateTeamUID(short id) {
        while (id == 0 || id == 1 || id == 2 || teamMap.containsKey(id)) {
            id = (short) MathUtils.RAND.nextInt();
        }

        return id;
    }

    public boolean shouldLoadLatmod() {
        return latModFolder.exists() && !get().dataFolder.exists();
    }
}
