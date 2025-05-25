package serverutils.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.events.team.ForgeTeamDataEvent;
import serverutils.events.universe.UniverseClosedEvent;
import serverutils.events.universe.UniverseLoadedEvent;
import serverutils.events.universe.UniverseSavedEvent;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.MathUtils;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.ranks.Ranks;

public class ServerUtilitiesUniverseData {

    public static final ServerUtilitiesUniverseData INST = new ServerUtilitiesUniverseData();
    public static final BlockDimPosStorage WARPS = new BlockDimPosStorage();
    private static final List<String> worldLog = new ArrayList<>();
    private static final List<String> chatLog = new ArrayList<>();
    private List<ServerUtilitiesTeamData> registeredTeamData = new ArrayList<>();

    public static boolean isInSpawn(MinecraftServer server, ChunkDimPos pos) {
        if (pos.dim != 0 || (!server.isDedicatedServer() && !ServerUtilitiesConfig.world.spawn_area_in_sp)) {
            return false;
        }

        int radius = ServerUtilitiesConfig.world.spawn_radius;
        if (radius <= 0) {
            return false;
        }

        ChunkCoordinates c = server.getEntityWorld().getSpawnPoint();
        int minX = MathUtils.chunk(c.posX - radius);
        int minZ = MathUtils.chunk(c.posZ - radius);
        int maxX = MathUtils.chunk(c.posX + radius);
        int maxZ = MathUtils.chunk(c.posZ + radius);
        return pos.posX >= minX && pos.posX <= maxX && pos.posZ >= minZ && pos.posZ <= maxZ;
    }

    @SubscribeEvent
    public void registerTeamData(ForgeTeamDataEvent event) {
        event.register(new ServerUtilitiesTeamData(event.getTeam()));
    }

    @SubscribeEvent
    public void onCreateServerTeams(UniverseLoadedEvent.CreateServerTeams event) {
        ServerUtilitiesTeamData teamData = new ServerUtilitiesTeamData(event.getUniverse().fakePlayerTeam);
        registeredTeamData.add(teamData);
        MinecraftForge.EVENT_BUS.register(teamData);
    }

    @SubscribeEvent
    public void onUniversePreLoaded(UniverseLoadedEvent.Pre event) {
        if (ServerUtilitiesConfig.world.chunk_claiming) {
            ClaimedChunks.instance = new ClaimedChunks(event.getUniverse());
        }

        Ranks.INSTANCE = new Ranks(event.getUniverse());
    }

    @SubscribeEvent
    public void onUniversePostLoaded(UniverseLoadedEvent.Post event) {
        NBTTagCompound nbt = event.getData(ServerUtilities.MOD_ID);
        WARPS.deserializeNBT(nbt.getCompoundTag("Warps"));
    }

    @SubscribeEvent
    public void onUniverseLoaded(UniverseLoadedEvent.Finished event) {
        long now = System.currentTimeMillis();
        if (ClaimedChunks.isActive()) {
            ClaimedChunks.instance.nextChunkloaderUpdate = now + Ticks.SECOND.millis();
        }
    }

    public static void worldLog(String s) {
        StringBuilder out = new StringBuilder();
        Calendar time = Calendar.getInstance();
        appendNum(out, time.get(Calendar.YEAR), '-');
        appendNum(out, time.get(Calendar.MONTH) + 1, '-');
        appendNum(out, time.get(Calendar.DAY_OF_MONTH), ' ');
        appendNum(out, time.get(Calendar.HOUR_OF_DAY), ':');
        appendNum(out, time.get(Calendar.MINUTE), ':');
        appendNum(out, time.get(Calendar.SECOND), ' ');
        out.append(':');
        out.append(' ');
        out.append(s);
        worldLog.add(out.toString());
        Universe.get().markDirty();
    }

    public static void chatLog(String s) {
        StringBuilder out = new StringBuilder();
        Calendar time = Calendar.getInstance();
        appendNum(out, time.get(Calendar.YEAR), '-');
        appendNum(out, time.get(Calendar.MONTH) + 1, '-');
        appendNum(out, time.get(Calendar.DAY_OF_MONTH), ' ');
        appendNum(out, time.get(Calendar.HOUR_OF_DAY), ':');
        appendNum(out, time.get(Calendar.MINUTE), ':');
        appendNum(out, time.get(Calendar.SECOND), ' ');
        out.append(':');
        out.append(' ');
        out.append(s);
        chatLog.add(out.toString());
        Universe.get().markDirty();
    }

    private static void appendNum(StringBuilder sb, int num, char c) {
        if (num < 10) {
            sb.append('0');
        }
        sb.append(num);
        if (c != '\0') {
            sb.append(c);
        }
    }

    @SubscribeEvent
    public void onUniverseSaved(UniverseSavedEvent event) {
        if (ClaimedChunks.isActive()) {
            ClaimedChunks.instance.processQueue();
        }

        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Warps", WARPS.serializeNBT());
        event.setData(ServerUtilities.MOD_ID, nbt);

        if (!worldLog.isEmpty()) {
            List<String> worldLogCopy = new ArrayList<>(worldLog);
            worldLog.clear();

            ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
                try (PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new FileWriter(
                                        FileUtils.newFile(event.getUniverse().server.getFile("logs/world.log")),
                                        true)))) {
                    for (String s : worldLogCopy) {
                        out.println(s);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                return false;
            });
        }

        if (!chatLog.isEmpty()) {
            List<String> chatLogCopy = new ArrayList<>(chatLog);
            chatLog.clear();

            ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
                try (PrintWriter out = new PrintWriter(
                        new BufferedWriter(
                                new FileWriter(
                                        FileUtils.newFile(event.getUniverse().server.getFile("logs/chat.log")),
                                        true)))) {
                    for (String s : chatLogCopy) {
                        out.println(s);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                return false;
            });
        }

    }

    @SubscribeEvent
    public void onUniverseClosed(UniverseClosedEvent event) {
        if (ClaimedChunks.instance != null) {
            ClaimedChunks.instance.clear();
            ClaimedChunks.instance = null;
        }
        ServerUtilitiesLoadedChunkManager.INSTANCE.clear();
        for (ServerUtilitiesTeamData teamData : registeredTeamData) {
            MinecraftForge.EVENT_BUS.unregister(teamData);
        }
        registeredTeamData.clear();
        Ranks.INSTANCE = null;
    }
}
