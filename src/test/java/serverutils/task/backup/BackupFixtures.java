package serverutils.task.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveHandler;

import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.compression.ICompress;

public final class BackupFixtures {

    static final UUID UUID = java.util.UUID.randomUUID();

    static void setupDefaultOptions() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
        ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        ServerUtilitiesConfig.backups.excluded_backup_files = new String[0];
        ServerUtilitiesConfig.backups.backups_to_keep = 12;
        ServerUtilitiesConfig.backups.retention_policy = new String[0];
        ServerUtilitiesConfig.backups.backup_timer = "30m";
        ServerUtilitiesConfig.backups.compression_level = 1;
        ServerUtilitiesConfig.backups.enable_backups = true;
        ServerUtilitiesConfig.backups.need_online_players = false;
        ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
        ServerUtilitiesConfig.backups.silent_backup = true;
        ServerUtilitiesConfig.backups.use_separate_thread = true;
        ServerUtilitiesConfig.world.chunk_claiming = true;
    }

    static void setCurrentServer(MinecraftServer server) throws ReflectiveOperationException {
        setStatic(MinecraftServer.class, "mcServer", server);
    }

    public static void setStatic(Class<?> owner, String name, Object value) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    static ZipFile archive(BackupResult.Save result) {
        try {
            return new ZipFile(result.destination);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** A custom-named archive of saves/world carrying backup metadata. */
    public static void writeArchive(File file, String worldId, long created) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            Properties metadata = new Properties();
            metadata.setProperty("version", "1");
            metadata.setProperty("worldId", worldId);
            metadata.setProperty("createdAt", Long.toString(created));
            metadata.setProperty("customName", "true");
            zip.putNextEntry(new ZipEntry(ICompress.BACKUP_METADATA_ENTRY));
            metadata.store(zip, null);
            zip.putNextEntry(new ZipEntry("saves/world/level.dat"));
            zip.write(1);
        }
    }

    public static void writeLegacyArchive(File file, String worldFolder, UUID universeId) throws IOException {
        NBTTagCompound universe = new NBTTagCompound();
        // Universe saves UUIDs without hyphens.
        universe.setString("UUID", universeId.toString().replace("-", ""));
        ByteArrayOutputStream universeBytes = new ByteArrayOutputStream();
        // writeCompressed closes its stream, so it can't write into the zip directly.
        CompressedStreamTools.writeCompressed(universe, universeBytes);

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            zip.putNextEntry(new ZipEntry(worldFolder + "/level.dat"));
            zip.write(1);
            zip.putNextEntry(new ZipEntry(worldFolder + "/serverutilities/universe.dat"));
            zip.write(universeBytes.toByteArray());
        }
    }

    public static class Tests {

        private MinecraftServer previousServer; // I don't know why we need this but I don't want to break existing
                                                // assumptions
        private List<EntityPlayerMP> players;

        protected Universe universe;
        protected MinecraftServer server;

        protected File build;
        protected File world;
        protected File saves;
        protected File backups;

        public void testSetup(SetupFlags... flags) {
            Set<SetupFlags> flagSet = new HashSet<>(Arrays.asList(flags.length == 0 ? SetupFlags.values() : flags));

            BackupFixtures.setupDefaultOptions();

            try {
                if (flagSet.contains(SetupFlags.FOLDERS)) setupFolders();
                if (flagSet.contains(SetupFlags.MINECRAFT)) setupMinecraft();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void testReset() {
            try {
                doClearTests();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected EntityPlayerMP addPlayer() {
            EntityPlayerMP player = mock(EntityPlayerMP.class);
            players.add(player);
            return player;
        }

        protected void claim(ChunkDimPos... positions) {
            ClaimedChunks.instance = mock(ClaimedChunks.class);
            when(ClaimedChunks.instance.getAllClaimedPositions())
                .thenReturn(new HashSet<>(Arrays.asList(positions)));
        }

        private void setupFolders() throws IOException {
            Path root = Files.createTempDirectory(new File("build").toPath(), "nested-world-");
            build = root.toFile();
            saves = FileUtils.ensureExists(new File(build, "saves"));
            world = FileUtils.ensureExists(new File(saves, "world"));
            backups = FileUtils.ensureExists(new File(build, "backups"));

            Files.write(world.toPath().resolve("level.dat"), new byte[] { 1 });
            ServerUtilitiesConfig.backups.backup_folder_path = backups.getAbsolutePath();
        }

        private void setupMinecraft() throws ReflectiveOperationException {
            previousServer = MinecraftServer.getServer();

            universe = mock(Universe.class);
            server = mock(MinecraftServer.class);

            doAnswer(invocation -> {
                Runnable action = invocation.getArgument(0);
                action.run();
                return null;
            }).when(universe).scheduleAction(any());

            setupServerMocks(server);
            BackupFixtures.setCurrentServer(server);

            Field serverField = Universe.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(universe, server);
        }

        private void doClearTests() throws ReflectiveOperationException {
            BackupFixtures.setCurrentServer(previousServer);
            FileUtils.delete(build);
        }

        private void setupServerMocks(MinecraftServer server) throws NoSuchFieldException, IllegalAccessException {
            ServerConfigurationManager manager = mock(ServerConfigurationManager.class);

            players = new ArrayList<>();

            Field playerField = ServerConfigurationManager.class.getDeclaredField("playerEntityList");
            playerField.setAccessible(true);
            playerField.set(manager, players);

            when(server.getConfigurationManager()).thenReturn(manager);

            ISaveFormat format = mock(ISaveFormat.class);
            SaveHandler handler = mock(SaveHandler.class);
            when(server.getActiveAnvilConverter()).thenReturn(format);

            WorldServer worldServer = mock(WorldServer.class);
            server.worldServers = new WorldServer[] { worldServer };

            when(format.getSaveLoader(anyString(), eq(false))).thenReturn(handler);
            when(server.getFolderName()).thenAnswer(invocation -> world.getName());
            when(handler.getWorldDirectory()).thenAnswer(invocation -> world);
        }

        public enum SetupFlags {
            FOLDERS,
            MINECRAFT,
        }
    }
}
