package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveHandler;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.compression.ICompress;

public class BackupTaskTest {

    @BeforeClass
    public static void configureBackups() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
        ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        ServerUtilitiesConfig.backups.backups_to_keep = 12;
        ServerUtilitiesConfig.backups.compression_level = 1;
        ServerUtilitiesConfig.backups.enable_backups = true;
        ServerUtilitiesConfig.backups.need_online_players = false;
        ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
        ServerUtilitiesConfig.backups.silent_backup = true;
        ServerUtilitiesConfig.backups.use_separate_thread = true;
    }

    @AfterClass
    public static void removeTestBackups() {
        FileUtils.delete(BackupTask.BACKUP_FOLDER);
    }

    @After
    public void resetBackupState() {
        BackupTask.stopBackupThread();
    }

    @Test
    public void rejectsWorldsInsideBackupStorageBeforePreparingOrWriting() throws Exception {
        File sentinel = new File(BackupTask.BACKUP_TEMP_FOLDER, "snapshot/keep.dat");
        Files.createDirectories(sentinel.toPath().getParent());
        Files.write(sentinel.toPath(), new byte[] { 42 });
        MinecraftServer server = mock(MinecraftServer.class);
        ServerConfigurationManager manager = mock(ServerConfigurationManager.class);
        Field players = ServerConfigurationManager.class.getDeclaredField("playerEntityList");
        players.setAccessible(true);
        players.set(manager, Collections.emptyList());
        when(server.getConfigurationManager()).thenReturn(manager);
        cpw.mods.fml.common.FMLCommonHandler fml = cpw.mods.fml.common.FMLCommonHandler.instance();
        Field delegate = cpw.mods.fml.common.FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        delegate.setAccessible(true);
        Object previous = delegate.get(fml);
        cpw.mods.fml.common.IFMLSidedHandler side = mock(cpw.mods.fml.common.IFMLSidedHandler.class);
        when(side.getServer()).thenReturn(server);
        delegate.set(fml, side);
        try {
            for (File storage : new File[] { BackupTask.BACKUP_FOLDER, BackupTask.BACKUP_TEMP_FOLDER }) {
                for (String relative : new String[] { "", "world", "world/.." }) {
                    File source = new File(storage, relative);
                    org.junit.Assert.assertThrows(java.io.IOException.class, () -> ThreadBackup.snapshotFiles(source));
                    assertTrue("Validation must precede snapshot cleanup", sentinel.isFile());
                    for (ThreadBackup.Snapshot snapshot : java.util.Arrays
                            .asList(null, new ThreadBackup.Snapshot(Collections.emptyMap(), sentinel))) {
                        ICompress compressor = mock(ICompress.class);
                        ThreadBackup.doBackup(
                                compressor,
                                source,
                                "invalid-source",
                                Collections.emptySet(),
                                snapshot,
                                false);
                        org.mockito.Mockito.verifyNoInteractions(compressor);
                        assertFalse(new File(BackupTask.BACKUP_FOLDER, "invalid-source.zip").exists());
                    }
                }
            }
            doThrow(new IllegalStateException("injected preparation failure")).when(manager).saveAllPlayerData();
            new BackupTask(mock(ICommandSender.class), "invalid-source").execute(new Universe(server));
            assertTrue("Failed preparation must not delete an unowned snapshot", sentinel.isFile());
        } finally {
            delegate.set(fml, previous);
            Files.deleteIfExists(sentinel.toPath());
        }
    }

    @Test
    public void protectsNewWorldsAndRejectsSaveCommandsUntilCleanup() throws Exception {
        WorldServer world = mock(WorldServer.class);
        WorldServer loadedLater = mock(WorldServer.class);
        Field provider = net.minecraft.world.World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(loadedLater, mock(net.minecraft.world.WorldProvider.class));
        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { world });
        serverutils.handlers.ServerUtilitiesServerEventHandler
                .loadWorldEvent(new net.minecraftforge.event.world.WorldEvent.Load(loadedLater));
        assertTrue(loadedLater.levelSaving);
        for (net.minecraft.command.ICommand command : new net.minecraft.command.ICommand[] {
                new net.minecraft.command.server.CommandSaveAll(), new net.minecraft.command.server.CommandSaveOn(),
                new net.minecraft.command.server.CommandSaveOff() }) {
            net.minecraftforge.event.CommandEvent event = org.mockito.Mockito
                    .spy(new net.minecraftforge.event.CommandEvent(command, mock(ICommandSender.class), new String[0]));
            // Forge adds this override from @Cancelable when transforming CommandEvent at runtime.
            when(event.isCancelable()).thenReturn(true);
            serverutils.handlers.ServerUtilitiesServerEventHandler.onBackupSaveCommand(event);
            assertTrue(event.isCanceled());
            assertTrue(event.exception instanceof net.minecraft.command.CommandException);
        }
        BackupTask.stopBackupThread();
        assertFalse(world.levelSaving);
        assertFalse(loadedLater.levelSaving);
        net.minecraftforge.event.CommandEvent event = new net.minecraftforge.event.CommandEvent(
                new net.minecraft.command.server.CommandSaveAll(),
                mock(ICommandSender.class),
                new String[0]);
        serverutils.handlers.ServerUtilitiesServerEventHandler.onBackupSaveCommand(event);
        assertFalse(event.isCanceled());
    }

    @Test
    public void chunkFlushDrainsStrandedWorkAndRestoresFlagsOnFailure() throws Exception {
        WorldServer world = mock(WorldServer.class);
        world.levelSaving = true;
        File root = Files.createTempDirectory(new File("build").toPath(), "stranded-chunk-").toFile();
        CountDownLatch idle = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        class Loader extends net.minecraft.world.chunk.storage.AnvilChunkLoader {

            private boolean paused;

            Loader() {
                super(root);
            }

            void enqueue(int x) {
                net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                tag.setInteger("value", x);
                addChunkToPending(new net.minecraft.world.ChunkCoordIntPair(x, 0), tag);
            }

            @Override
            public boolean writeNextIO() {
                boolean more = super.writeNextIO();
                if (!more && !paused) {
                    paused = true;
                    idle.countDown();
                    try {
                        if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Producer timed out");
                    } catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                }
                return more;
            }
        }
        Loader loader = new Loader();
        doAnswer(invocation -> {
            assertFalse(world.levelSaving);
            loader.saveExtraData();
            return null;
        }).when(world).saveChunkData();
        try {
            loader.enqueue(0);
            try {
                assertTrue(idle.await(10, TimeUnit.SECONDS));
                loader.enqueue(1);
            } finally {
                release.countDown();
            }
            BackupTask.flushChunkSaves(new WorldServer[] { null, world });
            assertTrue(world.levelSaving);
            try (java.io.DataInputStream in = net.minecraft.world.chunk.storage.RegionFileCache
                    .getChunkInputStream(root, 1, 0)) {
                assertTrue("Stranded chunk must reach disk", in != null);
                assertEquals(1, net.minecraft.nbt.CompressedStreamTools.read(in).getInteger("value"));
            }
            doThrow(new IllegalStateException("flush failed")).when(world).saveChunkData();
            org.junit.Assert.assertThrows(
                    IllegalStateException.class,
                    () -> BackupTask.flushChunkSaves(new WorldServer[] { world }));
            assertTrue(world.levelSaving);
        } finally {
            release.countDown();
            net.minecraft.world.chunk.storage.RegionFileCache.clearRegionFileReferences();
            FileUtils.delete(root);
        }
    }

    @Test
    public void backupSavesPreviouslyDisabledWorldAndRestoresItsFlag() throws Exception {
        WorldServer world = mock(WorldServer.class);
        world.levelSaving = true;
        doAnswer(invocation -> {
            assertFalse("Backup must force a current chunk snapshot", world.levelSaving);
            return null;
        }).when(world).saveAllChunks(true, null);
        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { world });
        BackupTask.restoreWorldSaving();
        assertTrue(world.levelSaving);
    }

    @Test
    public void cancelledReconstructionSkipsChunkDecodingAndCleansTemporaryRegion() throws Exception {
        File source = Files.createTempDirectory(new File("build").toPath(), "cancelled-region-").toFile();
        File file = new File(source, "region/r.0.0.mca");
        Files.createDirectories(file.toPath().getParent());
        net.minecraft.world.chunk.storage.RegionFile region = new net.minecraft.world.chunk.storage.RegionFile(file);
        try (java.io.DataOutputStream out = region.getChunkDataOutputStream(0, 0)) {
            // An invalid NBT type makes decoding fail if cancellation is checked too late.
            out.writeByte(99);
        } finally {
            region.close();
        }
        Files.createDirectories(BackupTask.BACKUP_TEMP_FOLDER.toPath());
        java.util.Set<String> previousFiles = new java.util.HashSet<>(
                java.util.Arrays.asList(BackupTask.BACKUP_TEMP_FOLDER.list()));
        boolean previousEntire = ServerUtilitiesConfig.backups.backup_entire_regions_with_claims;
        ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = false;
        java.lang.reflect.Method reconstruct = ThreadBackup.class.getDeclaredMethod(
                "backupRegions",
                Map.class,
                File.class,
                java.util.Set.class,
                ICompress.class,
                Map.class,
                int.class);
        reconstruct.setAccessible(true);
        try {
            Thread.currentThread().interrupt();
            java.lang.reflect.InvocationTargetException failure = org.junit.Assert.assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> reconstruct.invoke(
                            null,
                            new java.util.HashMap<>(),
                            source,
                            Collections.singleton(new serverutils.lib.math.ChunkDimPos(0, 0, 0)),
                            mock(ICompress.class),
                            Collections.singletonMap(0, source),
                            0));
            assertTrue(
                    "Cancellation must precede chunk decoding",
                    failure.getCause() instanceof java.io.InterruptedIOException);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = previousEntire;
            FileUtils.delete(source);
        }
        assertEquals(
                previousFiles,
                new java.util.HashSet<>(java.util.Arrays.asList(BackupTask.BACKUP_TEMP_FOLDER.list())));
    }

    @Test
    public void forcedClaimFilteringIncludesUnloadedDimensionsAndReconstructsChunks() throws Exception {
        File source = new File("build/test-claimed-backup-source");
        File regions = new File(source, "region");
        assertTrue(regions.mkdirs() || regions.isDirectory());
        File metadata = new File(source, "level.dat");
        Files.write(metadata.toPath(), new byte[] { 42 });
        File claimed = new File(regions, "r.0.0.mca");
        File unclaimed = new File(regions, "r.1.0.mca");
        writeRegionChunk(claimed, 0, 0, "overworld");
        writeRegionChunk(claimed, 1, 0, "unclaimed");
        writeRegionChunk(unclaimed, 32, 0, "unclaimed-region");
        File unloadedRegion = new File(source, "DIM7/region/r.-1.-1.mca");
        File customRegion = new File(source, "custom-moon/region/r.-1.-1.mca");
        writeRegionChunk(unloadedRegion, -1, -1, "unloaded");
        writeRegionChunk(unloadedRegion, -2, -1, "unclaimed");
        writeRegionChunk(customRegion, -1, -1, "custom-folder");
        writeRegionChunk(customRegion, -2, -1, "unclaimed");
        WorldServer world = mock(WorldServer.class);
        Field provider = net.minecraft.world.World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, mock(net.minecraft.world.WorldProvider.class));
        when(world.getChunkSaveLocation()).thenReturn(source);
        MinecraftServer server = mock(MinecraftServer.class);
        server.worldServers = new WorldServer[] { world };
        when(server.getConfigurationManager()).thenReturn(mock(ServerConfigurationManager.class));
        ISaveFormat saveFormat = mock(ISaveFormat.class);
        SaveHandler saveHandler = mock(SaveHandler.class);
        when(server.getActiveAnvilConverter()).thenReturn(saveFormat);
        when(saveFormat.getSaveLoader(server.getFolderName(), false)).thenReturn(saveHandler);
        when(saveHandler.getWorldDirectory()).thenReturn(source);
        setCurrentServer(server);
        ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
        ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = true;
        Field loaderInstance = cpw.mods.fml.common.Loader.class.getDeclaredField("instance");
        loaderInstance.setAccessible(true);
        Object previousLoader = loaderInstance.get(null);
        loaderInstance.set(null, mock(cpw.mods.fml.common.Loader.class));
        cpw.mods.fml.common.FMLCommonHandler fml = cpw.mods.fml.common.FMLCommonHandler.instance();
        Field sidedDelegate = cpw.mods.fml.common.FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        sidedDelegate.setAccessible(true);
        Object previousDelegate = sidedDelegate.get(fml);
        cpw.mods.fml.common.IFMLSidedHandler side = mock(cpw.mods.fml.common.IFMLSidedHandler.class);
        when(side.getServer()).thenReturn(server);
        sidedDelegate.set(fml, side);
        boolean previousClaiming = ServerUtilitiesConfig.world.chunk_claiming;
        serverutils.data.ClaimedChunks previousClaims = serverutils.data.ClaimedChunks.instance;
        net.minecraftforge.common.DimensionManager
                .registerProviderType(7, net.minecraft.world.WorldProviderSurface.class, false);
        net.minecraftforge.common.DimensionManager.registerDimension(7, 7);
        net.minecraftforge.common.DimensionManager.registerProviderType(8, CustomFolderProvider.class, false);
        net.minecraftforge.common.DimensionManager.registerDimension(8, 8);
        net.minecraftforge.common.DimensionManager.registerProviderType(9, BrokenFolderProvider.class, false);
        net.minecraftforge.common.DimensionManager.registerDimension(9, 9);
        try {
            Universe universe = new Universe(server);
            universe.dataFolder = new File(source, "serverutilities");
            serverutils.lib.data.ForgeTeam team = new serverutils.lib.data.ForgeTeam(
                    universe,
                    (short) 1,
                    "test",
                    serverutils.lib.data.TeamType.SERVER);
            universe.addTeam(team);
            java.lang.reflect.Constructor<serverutils.data.ServerUtilitiesTeamData> dataConstructor = serverutils.data.ServerUtilitiesTeamData.class
                    .getDeclaredConstructor(serverutils.lib.data.ForgeTeam.class);
            dataConstructor.setAccessible(true);
            serverutils.data.ServerUtilitiesTeamData teamData = dataConstructor.newInstance(team);
            ServerUtilitiesConfig.world.chunk_claiming = true;
            serverutils.data.ClaimedChunks.instance = new serverutils.data.ClaimedChunks(universe);
            for (boolean async : new boolean[] { false, true }) {
                for (boolean entire : new boolean[] { false, true }) {
                    serverutils.data.ClaimedChunks.instance.clear();
                    for (serverutils.lib.math.ChunkDimPos pos : new serverutils.lib.math.ChunkDimPos[] {
                            new serverutils.lib.math.ChunkDimPos(0, 0, 0),
                            new serverutils.lib.math.ChunkDimPos(-1, -1, 7),
                            new serverutils.lib.math.ChunkDimPos(-1, -1, 8) }) {
                        // Leave the claims queued: BackupTask must include newly claimed chunks too.
                        serverutils.data.ClaimedChunks.instance
                                .addChunk(new serverutils.data.ClaimedChunk(pos, teamData));
                    }
                    ServerUtilitiesConfig.backups.use_separate_thread = async;
                    ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = entire;
                    new BackupTask(mock(ICommandSender.class), "forced-claims", true).execute(universe);
                    if (async) waitForBackup();
                    new BackupTask(true).execute(universe);
                    assertFalse(world.levelSaving);
                    try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "forced-claims.zip"))) {
                        try (InputStream input = zip
                                .getInputStream(zip.getEntry(FileUtils.getRelativePath(metadata)))) {
                            org.junit.Assert.assertArrayEquals(new byte[] { 42 }, IOUtils.toByteArray(input));
                        }
                        assertArchivedChunks(zip, claimed, 0, 0, 1, "overworld", entire);
                        assertArchivedChunks(zip, unloadedRegion, -1, -1, -2, "unloaded", entire);
                        assertArchivedChunks(zip, customRegion, -1, -1, -2, "custom-folder", entire);
                        assertNull(zip.getEntry(FileUtils.getRelativePath(unclaimed)));
                    }
                    assertNull(net.minecraftforge.common.DimensionManager.getWorld(7));
                    assertNull(net.minecraftforge.common.DimensionManager.getWorld(8));
                }
            }

            // The asynchronous worker uses the paths captured before it starts, not the live provider registry.
            CustomFolderProvider.folder = "custom-moon";
            ThreadBackup captured = new ThreadBackup(
                    ICompress.createCompressor(),
                    source,
                    "captured-path",
                    Collections.singleton(new serverutils.lib.math.ChunkDimPos(-1, -1, 8)),
                    null,
                    true);
            CustomFolderProvider.folder = "wrong-folder";
            captured.start();
            captured.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(captured.isAlive());
            try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "captured-path.zip"))) {
                assertTrue(zip.getEntry(FileUtils.getRelativePath(customRegion)) != null);
            }
            CustomFolderProvider.folder = "custom-moon";

            // A removed provider can leave region files in a custom folder that can no longer be resolved.
            File removedModRegion = new File(source, "removed-mod/region/r.0.0.mca");
            writeRegionChunk(removedModRegion, 0, 0, "recoverable");
            File nestedRegion = new File(regions, "unowned/r.0.0.mca");
            writeRegionChunk(nestedRegion, 0, 0, "unknown-nested-folder");
            File brokenProviderRegion = new File(source, "DIM9/region/r.0.0.mca");
            writeRegionChunk(brokenProviderRegion, 0, 0, "broken-provider");
            for (boolean configured : new boolean[] { false, true }) {
                ServerUtilitiesConfig.backups.only_backup_claimed_chunks = configured;
                for (boolean entire : new boolean[] { false, true }) {
                    ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = entire;
                    for (boolean threaded : new boolean[] { false, true }) {
                        java.util.Set<serverutils.lib.math.ChunkDimPos> staleClaims = new java.util.HashSet<>();
                        staleClaims.add(new serverutils.lib.math.ChunkDimPos(0, 0, 999999));
                        staleClaims.add(new serverutils.lib.math.ChunkDimPos(0, 0, 0));
                        if (threaded) {
                            ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(source);
                            ThreadBackup stale = configured
                                    ? new ThreadBackup(
                                            ICompress.createCompressor(),
                                            source,
                                            "unregistered",
                                            staleClaims,
                                            snapshot)
                                    : new ThreadBackup(
                                            ICompress.createCompressor(),
                                            source,
                                            "unregistered",
                                            staleClaims,
                                            snapshot,
                                            true);
                            stale.start();
                            stale.join(TimeUnit.SECONDS.toMillis(5));
                            assertFalse(stale.isAlive());
                        } else if (configured) {
                            ThreadBackup.doBackup(ICompress.createCompressor(), source, "unregistered", staleClaims);
                        } else {
                            ThreadBackup.doBackup(
                                    ICompress.createCompressor(),
                                    source,
                                    "unregistered",
                                    staleClaims,
                                    null,
                                    true);
                        }
                        try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "unregistered.zip"))) {
                            assertArchivedChunks(zip, claimed, 0, 0, 1, "overworld", entire);
                            assertNull(zip.getEntry(FileUtils.getRelativePath(unclaimed)));
                            assertNull(zip.getEntry(FileUtils.getRelativePath(unloadedRegion)));
                            assertNull(zip.getEntry(FileUtils.getRelativePath(customRegion)));
                            for (File preserved : new File[] { removedModRegion, nestedRegion, brokenProviderRegion }) {
                                ZipEntry entry = zip.getEntry(FileUtils.getRelativePath(preserved));
                                assertTrue(entry != null);
                                try (InputStream in = zip.getInputStream(entry)) {
                                    org.junit.Assert.assertArrayEquals(
                                            Files.readAllBytes(preserved.toPath()),
                                            IOUtils.toByteArray(in));
                                }
                            }
                        }
                    }
                }
            }
            ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
            for (boolean empty : new boolean[] { false, true }) {
                java.util.Set<serverutils.lib.math.ChunkDimPos> claims = empty ? Collections.emptySet()
                        : Collections.singleton(new serverutils.lib.math.ChunkDimPos(0, 0, 0));
                ThreadBackup.doBackup(ICompress.createCompressor(), source, "forced-claims", claims, null, true);
                try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "forced-claims.zip"))) {
                    assertEquals(!empty, zip.getEntry(FileUtils.getRelativePath(claimed)) != null);
                    assertNull(zip.getEntry(FileUtils.getRelativePath(unclaimed)));
                    assertNull(zip.getEntry(FileUtils.getRelativePath(unloadedRegion)));
                    assertNull(zip.getEntry(FileUtils.getRelativePath(customRegion)));
                    assertTrue(zip.getEntry(FileUtils.getRelativePath(removedModRegion)) != null);
                    assertTrue(zip.getEntry(FileUtils.getRelativePath(nestedRegion)) != null);
                }
            }

            for (boolean entire : new boolean[] { false, true }) {
                ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = entire;
                for (int size : new int[] { 0, 4096, 8193 }) {
                    File malformed = new File(regions, "r.2.0.mca");
                    byte[] original = new byte[size];
                    java.util.Arrays.fill(original, (byte) 42);
                    Files.write(malformed.toPath(), original);
                    ThreadBackup.doBackup(
                            ICompress.createCompressor(),
                            source,
                            "malformed-region",
                            Collections.singleton(new serverutils.lib.math.ChunkDimPos(64, 0, 0)),
                            null,
                            true);
                    org.junit.Assert.assertArrayEquals(original, Files.readAllBytes(malformed.toPath()));
                    try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "malformed-region.zip"))) {
                        ZipEntry entry = zip.getEntry(FileUtils.getRelativePath(malformed));
                        assertTrue(entry != null);
                        try (InputStream in = zip.getInputStream(entry)) {
                            org.junit.Assert.assertArrayEquals(original, IOUtils.toByteArray(in));
                        }
                    }
                    Files.delete(malformed.toPath());
                }
            }

            for (boolean configured : new boolean[] { false, true }) {
                ServerUtilitiesConfig.backups.only_backup_claimed_chunks = configured;
                for (boolean empty : new boolean[] { false, true }) {
                    java.util.Set<serverutils.lib.math.ChunkDimPos> claims = empty ? Collections.emptySet()
                            : Collections.singleton(new serverutils.lib.math.ChunkDimPos(0, 0, 0));
                    for (boolean threaded : new boolean[] { false, true }) {
                        if (threaded) {
                            ThreadBackup legacy = new ThreadBackup(
                                    ICompress.createCompressor(),
                                    source,
                                    "legacy-api",
                                    claims);
                            legacy.start();
                            legacy.join(TimeUnit.SECONDS.toMillis(5));
                            assertFalse(legacy.isAlive());
                        } else {
                            ThreadBackup.doBackup(ICompress.createCompressor(), source, "legacy-api", claims);
                        }
                        try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "legacy-api.zip"))) {
                            assertTrue(zip.getEntry(FileUtils.getRelativePath(claimed)) != null);
                            assertEquals(
                                    !(configured && !empty),
                                    zip.getEntry(FileUtils.getRelativePath(unclaimed)) != null);
                        }
                    }
                }
            }
        } finally {
            BackupTask.stopBackupThread();
            serverutils.data.ClaimedChunks.instance = previousClaims;
            ServerUtilitiesConfig.world.chunk_claiming = previousClaiming;
            ServerUtilitiesConfig.backups.use_separate_thread = true;
            ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
            CustomFolderProvider.folder = "custom-moon";
            net.minecraftforge.common.DimensionManager.unregisterDimension(7);
            net.minecraftforge.common.DimensionManager.unregisterDimension(8);
            net.minecraftforge.common.DimensionManager.unregisterDimension(9);
            net.minecraftforge.common.DimensionManager.unregisterProviderType(7);
            net.minecraftforge.common.DimensionManager.unregisterProviderType(8);
            net.minecraftforge.common.DimensionManager.unregisterProviderType(9);
            sidedDelegate.set(fml, previousDelegate);
            loaderInstance.set(null, previousLoader);
            ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = false;
            setCurrentServer(null);
            FileUtils.delete(source);
        }
    }

    public static class BrokenFolderProvider extends net.minecraft.world.WorldProviderSurface {

        @Override
        public String getSaveFolder() {
            throw new IllegalStateException("needs a registered world");
        }
    }

    public static class CustomFolderProvider extends net.minecraft.world.WorldProviderSurface {

        static String folder = "custom-moon";

        @Override
        public String getSaveFolder() {
            return folder;
        }
    }

    private static void writeRegionChunk(File file, int x, int z, String value) throws Exception {
        Files.createDirectories(file.toPath().getParent());
        net.minecraft.world.chunk.storage.RegionFile region = new net.minecraft.world.chunk.storage.RegionFile(file);
        try (java.io.DataOutputStream out = region.getChunkDataOutputStream(x & 31, z & 31)) {
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
            tag.setString("value", value);
            net.minecraft.nbt.CompressedStreamTools.write(tag, out);
        } finally {
            region.close();
        }
    }

    private static void assertArchivedChunks(ZipFile zip, File source, int x, int z, int otherX, String value,
            boolean entire) throws Exception {
        ZipEntry entry = zip.getEntry(FileUtils.getRelativePath(source));
        assertTrue("Missing " + source, entry != null);
        java.nio.file.Path copy = Files.createTempFile("claimed-region-test-", ".mca");
        try {
            try (InputStream in = zip.getInputStream(entry)) {
                Files.copy(in, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            net.minecraft.world.chunk.storage.RegionFile region = new net.minecraft.world.chunk.storage.RegionFile(
                    copy.toFile());
            try {
                try (java.io.DataInputStream in = region.getChunkDataInputStream(x & 31, z & 31)) {
                    assertTrue("Missing claimed chunk", in != null);
                    assertEquals(value, net.minecraft.nbt.CompressedStreamTools.read(in).getString("value"));
                }
                try (java.io.DataInputStream in = region.getChunkDataInputStream(otherX & 31, z & 31)) {
                    assertEquals("Unexpected unclaimed chunk contents", entire, in != null);
                }
            } finally {
                region.close();
            }
        } finally {
            Files.deleteIfExists(copy);
        }
    }

    @Test
    public void nextBackupRestoresSavingBeforePreparingAgain() throws Exception {
        WorldServer world = mock(WorldServer.class);
        AtomicInteger saves = new AtomicInteger();
        doAnswer(invocation -> {
            if (!world.levelSaving) saves.incrementAndGet();
            return null;
        }).when(world).saveAllChunks(true, null);

        MinecraftServer server = mock(MinecraftServer.class);
        ServerConfigurationManager players = mock(ServerConfigurationManager.class);
        when(server.getConfigurationManager()).thenReturn(players);
        server.worldServers = new WorldServer[] { world };

        File source = new File("build/test-backup-source");
        assertTrue(source.mkdirs() || source.isDirectory());
        ISaveFormat saveFormat = mock(ISaveFormat.class);
        SaveHandler saveHandler = mock(SaveHandler.class);
        when(server.getActiveAnvilConverter()).thenReturn(saveFormat);
        when(saveFormat.getSaveLoader(server.getFolderName(), false)).thenReturn(saveHandler);
        when(saveHandler.getWorldDirectory()).thenReturn(source);
        setCurrentServer(server);

        try {
            Universe universe = new Universe(server);
            new BackupTask(mock(ICommandSender.class), "first").execute(universe);
            waitForBackup();
            new BackupTask(mock(ICommandSender.class), "second").execute(universe);
            waitForBackup();
            new BackupTask(true).execute(universe);
        } finally {
            setCurrentServer(null);
            FileUtils.delete(source);
        }

        assertFalse(world.levelSaving);
        assertTrue("Both backups must save the world", saves.get() == 2);
    }

    @Test
    public void preparationFailureRestoresAlreadyDisabledWorlds() throws Exception {
        WorldServer first = mock(WorldServer.class);
        WorldServer failing = mock(WorldServer.class);
        doThrow(new MinecraftException("injected failure")).when(failing).saveAllChunks(true, null);

        try {
            BackupTask.saveAndDisableWorldSaving(new WorldServer[] { first, failing });
        } catch (MinecraftException expected) {
            assertFalse(first.levelSaving);
            assertFalse(failing.levelSaving);
            return;
        }
        throw new AssertionError("Expected preparation to fail");
    }

    @Test
    public void failedUniverseSaveAbortsBeforeStartingTheWorker() throws Exception {
        MinecraftServer server = mock(MinecraftServer.class);
        ServerConfigurationManager manager = mock(ServerConfigurationManager.class);
        Field players = ServerConfigurationManager.class.getDeclaredField("playerEntityList");
        players.setAccessible(true);
        players.set(manager, Collections.emptyList());
        when(server.getConfigurationManager()).thenReturn(manager);
        WorldServer world = mock(WorldServer.class);
        server.worldServers = new WorldServer[] { world };
        Universe universe = new Universe(server) {

            @Override
            public void saveForBackup() throws IOException {
                throw new IOException("injected save failure");
            }
        };
        new BackupTask(mock(ICommandSender.class), "failed-universe").execute(universe);
        assertFalse(world.levelSaving);
        assertFalse(BackupTask.isWorldSavingSuspended());
        assertNull(BackupTask.thread);
        assertFalse(new File(BackupTask.BACKUP_FOLDER, "failed-universe.zip").exists());
    }

    @Test
    public void optionalSaverPropagatesFailuresAndAllowsOlderVersions() throws Exception {
        CompatibleSaver saver = CompatibleSaver.INSTANCE;
        try {
            saver.failure = null;
            BackupTask.flushWorldDataSaver(CompatibleSaver.class);
            IOException diskError = new IOException("disk failure");
            saver.failure = diskError;
            org.junit.Assert.assertSame(
                    diskError,
                    org.junit.Assert.assertThrows(
                            IOException.class,
                            () -> BackupTask.flushWorldDataSaver(CompatibleSaver.class)));
            saver.failure = new InterruptedException("cancelled");
            org.junit.Assert.assertThrows(
                    InterruptedException.class,
                    () -> BackupTask.flushWorldDataSaver(CompatibleSaver.class));
            BackupTask.flushWorldDataSaver(Object.class); // Older saver has no flush API.
        } finally {
            saver.failure = null;
        }
    }

    public static class CompatibleSaver {

        public static final CompatibleSaver INSTANCE = new CompatibleSaver();
        Exception failure;

        public void flush() throws Exception {
            if (failure != null) throw failure;
        }
    }

    @Test
    public void restoresCapturedWorldsByIdentity() throws Exception {
        WorldServer first = mock(WorldServer.class);
        WorldServer second = mock(WorldServer.class);
        WorldServer loadedLater = mock(WorldServer.class);
        second.levelSaving = true;

        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { first, second });
        BackupTask.restoreWorldSaving();

        assertFalse(first.levelSaving);
        assertTrue(second.levelSaving);
        assertFalse(loadedLater.levelSaving);
    }

    @Test
    public void stoppingWaitsForWorkerBeforeRestoringSaving() throws Exception {
        WorldServer world = mock(WorldServer.class);
        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { world });
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        ThreadBackup worker = new ThreadBackup(null, null, "", Collections.emptySet()) {

            @Override
            public void run() {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ignored) {
                    cancelled.countDown();
                }
                // Cancellation starts shutdown, but the worker still owns the world files until it exits.
                boolean finished = false;
                while (!finished) {
                    try {
                        finish.await();
                        finished = true;
                    } catch (InterruptedException ignored) {}
                }
            }
        };
        BackupTask.thread = worker;
        java.util.concurrent.ExecutorService stopper = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            worker.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            java.util.concurrent.Future<?> stopped = stopper.submit(BackupTask::stopBackupThread);
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
            org.junit.Assert.assertThrows(
                    java.util.concurrent.TimeoutException.class,
                    () -> stopped.get(200, TimeUnit.MILLISECONDS));
            assertTrue(worker.isAlive());
            assertTrue("Saving must stay suspended while the worker exits", world.levelSaving);
            assertTrue(BackupTask.isWorldSavingSuspended());

            finish.countDown();
            stopped.get(5, TimeUnit.SECONDS);
            assertFalse(worker.isAlive());
            assertFalse(world.levelSaving);
            assertNull(BackupTask.thread);
        } finally {
            finish.countDown();
            worker.interrupt();
            worker.join(TimeUnit.SECONDS.toMillis(5));
            stopper.shutdownNow();
            assertTrue(stopper.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void backupStorageNestedInTheWorldIsPrunedFromTheWalk() throws Exception {
        // BACKUP_TEMP_FOLDER lives under this directory, so walking it must prune staging partway down.
        File source = BackupTask.BACKUP_TEMP_FOLDER.getParentFile();
        File keep = new File(source, "nested-keep.dat");
        File staged = new File(BackupTask.BACKUP_TEMP_FOLDER, "snapshot/deep/staged.dat");
        assertTrue(staged.getParentFile().mkdirs() || staged.getParentFile().isDirectory());
        Files.write(keep.toPath(), new byte[] { 1 });
        Files.write(staged.toPath(), new byte[] { 2 });
        try {
            ThreadBackup.doBackup(ICompress.createCompressor(), source, "nested-storage", Collections.emptySet());
            try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "nested-storage.zip"))) {
                assertTrue(zip.getEntry(FileUtils.getRelativePath(keep)) != null);
                assertNull(zip.getEntry(FileUtils.getRelativePath(staged)));
            }
        } finally {
            Files.deleteIfExists(keep.toPath());
            ThreadBackup.deleteSnapshot();
        }
    }

    @Test
    public void fileSymlinkIntoBackupStorageIsPrunedFromTheWalk() throws Exception {
        File source = new File("build/test-backup-storage-link");
        File link = new File(source, "linked-backup.zip");
        File target = new File(BackupTask.BACKUP_FOLDER, "linked-target.zip");
        File archive = new File(BackupTask.BACKUP_FOLDER, "linked-storage.zip");
        assertTrue(source.mkdirs() || source.isDirectory());
        Files.write(target.toPath(), new byte[] { 1 });
        try {
            try {
                Files.createSymbolicLink(link.toPath(), target.toPath().toAbsolutePath());
            } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
                org.junit.Assume.assumeNoException(unsupported);
            }
            ThreadBackup.doBackup(ICompress.createCompressor(), source, "linked-storage", Collections.emptySet());
            try (ZipFile zip = new ZipFile(archive)) {
                assertNull(zip.getEntry(FileUtils.getRelativePath(link)));
            }
        } finally {
            FileUtils.delete(source);
            Files.deleteIfExists(target.toPath());
            Files.deleteIfExists(archive.toPath());
        }
    }

    @Test
    public void asynchronousBackupUsesPreparedPlayerSnapshot() throws Exception {
        File source = new File("build/test-snapshot-world");
        File player = new File(source, "playerdata/player.dat");
        assertTrue(player.getParentFile().mkdirs() || player.getParentFile().isDirectory());
        Files.write(player.toPath(), "before".getBytes(StandardCharsets.UTF_8));

        String entryName = FileUtils.getRelativePath(player);
        ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(source);
        Files.write(player.toPath(), "after".getBytes(StandardCharsets.UTF_8));
        String[] previousPatterns = ServerUtilitiesConfig.backups.additional_backup_files;
        File oldBackup = new File(BackupTask.BACKUP_FOLDER, "previous.zip");
        Files.write(oldBackup.toPath(), new byte[] { 1 });
        ServerUtilitiesConfig.backups.additional_backup_files = new String[] {
                FileUtils.getRelativePath(BackupTask.BACKUP_TEMP_FOLDER) + "/**",
                FileUtils.getRelativePath(BackupTask.BACKUP_FOLDER) + "/**" };

        try {
            // Wildcards and literal directories must both exclude backup-owned files.
            for (boolean wildcard : new boolean[] { true, false }) {
                if (!wildcard) {
                    ServerUtilitiesConfig.backups.additional_backup_files = new String[] {
                            BackupTask.BACKUP_TEMP_FOLDER.getPath(), BackupTask.BACKUP_FOLDER.getPath() };
                }
                ThreadBackup.doBackup(
                        ICompress.createCompressor(),
                        source,
                        "snapshot-test",
                        Collections.emptySet(),
                        snapshot);
                try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "snapshot-test.zip"))) {
                    ZipEntry entry = zip.getEntry(entryName);
                    assertTrue(entry != null);
                    try (InputStream in = zip.getInputStream(entry)) {
                        assertEquals("before", new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
                    }
                    java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        assertFalse(name.startsWith(FileUtils.getRelativePath(BackupTask.BACKUP_TEMP_FOLDER) + "/"));
                        assertFalse(name.startsWith(FileUtils.getRelativePath(BackupTask.BACKUP_FOLDER) + "/"));
                    }
                }
            }
        } finally {
            ServerUtilitiesConfig.backups.additional_backup_files = previousPatterns;
            Files.deleteIfExists(oldBackup.toPath());
            FileUtils.delete(source);
            ThreadBackup.deleteSnapshot();
        }
    }

    private static void waitForBackup() throws InterruptedException {
        ThreadBackup worker = BackupTask.thread;
        worker.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse("Backup worker did not stop", worker.isAlive());
    }

    @Test
    public void spoolKeepsCapturedBytesAcrossCompressorsAndCompressionLevels() throws Exception {
        File source = Files.createTempDirectory(new File("build").toPath(), "spool-world-").toFile();
        File player = new File(source, "player.dat");
        File empty = new File(source, "empty.dat");
        File large = new File(source, "ae2.dat");
        File region = new File(source, "region.mca");
        byte[] payload = new byte[180000];
        new java.util.Random(7).nextBytes(payload);
        Files.write(player.toPath(), new byte[] { 1, 2, 3 });
        Files.write(empty.toPath(), new byte[0]);
        Files.write(large.toPath(), payload);
        Files.write(region.toPath(), new byte[] { 4 });
        int previousLevel = ServerUtilitiesConfig.backups.compression_level;
        String[] previousIncludes = ServerUtilitiesConfig.backups.additional_backup_files;
        File archive = new File(BackupTask.BACKUP_FOLDER, "spool-roundtrip.zip");
        try {
            ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(source);
            assertEquals(3, snapshot.entries.size());
            assertEquals(payload.length + 3, snapshot.spool.length());
            try (java.util.stream.Stream<java.nio.file.Path> paths = Files.list(snapshot.spool.toPath().getParent())) {
                assertEquals("Only one staging file", 1, paths.count());
            }
            Files.write(player.toPath(), new byte[] { 9 });
            Files.delete(empty.toPath());
            Files.delete(large.toPath());
            // Regions remain live under the existing world-saving suspension; they are not copied into the spool.
            Files.write(region.toPath(), new byte[] { 5 });
            ServerUtilitiesConfig.backups.additional_backup_files = new String[] { source.getPath() };
            Map<File, byte[]> expectedFiles = new java.util.LinkedHashMap<>();
            expectedFiles.put(player, new byte[] { 1, 2, 3 });
            expectedFiles.put(empty, new byte[0]);
            expectedFiles.put(large, payload);
            expectedFiles.put(region, new byte[] { 5 });
            for (boolean legacy : new boolean[] { false, true }) {
                for (int level : new int[] { 0, 1, 9 }) {
                    ServerUtilitiesConfig.backups.compression_level = level;
                    ICompress compressor = legacy ? new serverutils.lib.util.compression.LegacyCompressor()
                            : new serverutils.lib.util.compression.CommonsCompressor();
                    Files.deleteIfExists(archive.toPath());
                    ThreadBackup.doBackup(compressor, source, "spool-roundtrip", Collections.emptySet(), snapshot);
                    try (ZipFile zip = new ZipFile(archive)) {
                        assertEquals(snapshot.files.size(), zip.size());
                        for (Map.Entry<File, byte[]> expected : expectedFiles.entrySet()) {
                            ZipEntry entry = zip.getEntry(FileUtils.getRelativePath(expected.getKey()));
                            assertTrue(entry != null);
                            assertEquals(level == 0 ? ZipEntry.STORED : ZipEntry.DEFLATED, entry.getMethod());
                            try (InputStream input = zip.getInputStream(entry)) {
                                org.junit.Assert.assertArrayEquals(expected.getValue(), IOUtils.toByteArray(input));
                            }
                        }
                    }
                }
            }
        } finally {
            ServerUtilitiesConfig.backups.compression_level = previousLevel;
            ServerUtilitiesConfig.backups.additional_backup_files = previousIncludes;
            ThreadBackup.deleteSnapshot();
            FileUtils.delete(source);
            Files.deleteIfExists(archive.toPath());
        }
    }

    @Test
    public void deferredFilesStayInWholeAndClaimedArchivesWhileLiveDataIsCaptured() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "deferred-world-");
        File archive = new File(BackupTask.BACKUP_FOLDER, "deferred-roundtrip.zip");
        java.lang.reflect.Method backup = ThreadBackup.class.getDeclaredMethod(
                "doBackup",
                ICompress.class,
                File.class,
                String.class,
                java.util.Set.class,
                ThreadBackup.Snapshot.class,
                boolean.class,
                Map.class);
        backup.setAccessible(true);
        String[] stable = { "AE2/spawndata/0_0_0.dat",
                "betterquesting/backup/3.8.75-GTNH/QuestDatabase_backup_3.8.75-GTNH.json" };
        String[] live = { "AE2/compass/0_0_0.dat", "AE2/settings.cfg", "buildcraft/zonemap/world/r0,0.nbt",
                "betterquesting/QuestDatabase.json", "QuestLoot.json", "AE2/spawndata/nested/unrecognized.dat",
                "AE2/spawndata-extra/0_0_0.dat", "betterquesting/backup/3.8.75-GTNH/other.json" };
        int previousLevel = ServerUtilitiesConfig.backups.compression_level;
        try {
            Map<File, byte[]> expected = new java.util.LinkedHashMap<>();
            for (String[] group : new String[][] { stable, live }) {
                for (String name : group) {
                    java.nio.file.Path file = root.resolve(name);
                    Files.createDirectories(file.getParent());
                    byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
                    Files.write(file, bytes);
                    expected.put(file.toFile(), bytes);
                }
            }
            ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(root.toFile());
            java.util.Set<String> captured = new java.util.HashSet<>();
            for (ZipEntry entry : snapshot.entries) captured.add(entry.getName());
            assertEquals(live.length, captured.size());
            for (String name : stable)
                assertFalse(captured.contains(FileUtils.getRelativePath(root.resolve(name).toFile())));
            for (String name : live) {
                assertTrue(captured.contains(FileUtils.getRelativePath(root.resolve(name).toFile())));
                Files.write(root.resolve(name), new byte[] { 99 });
            }
            for (boolean legacy : new boolean[] { false, true }) {
                for (boolean claimed : new boolean[] { false, true }) {
                    for (int level : new int[] { 0, 1 }) {
                        ServerUtilitiesConfig.backups.compression_level = level;
                        ICompress compressor = legacy ? new serverutils.lib.util.compression.LegacyCompressor()
                                : new serverutils.lib.util.compression.CommonsCompressor();
                        Files.deleteIfExists(archive.toPath());
                        backup.invoke(
                                null,
                                compressor,
                                root.toFile(),
                                "deferred-roundtrip",
                                Collections.emptySet(),
                                snapshot,
                                claimed,
                                Collections.singletonMap(0, root.toFile()));
                        try (ZipFile zip = new ZipFile(archive)) {
                            assertEquals(expected.size(), zip.size());
                            for (Map.Entry<File, byte[]> entry : expected.entrySet()) {
                                try (InputStream input = zip
                                        .getInputStream(zip.getEntry(FileUtils.getRelativePath(entry.getKey())))) {
                                    org.junit.Assert.assertArrayEquals(entry.getValue(), IOUtils.toByteArray(input));
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            ServerUtilitiesConfig.backups.compression_level = previousLevel;
            ThreadBackup.deleteSnapshot();
            FileUtils.delete(root.toFile());
            Files.deleteIfExists(archive.toPath());
        }
    }

    @Test
    public void linksNamedLikeStableDataStillUseCapturedTargetBytes() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "deferred-link-");
        java.nio.file.Path link = Files.createDirectories(root.resolve("AE2/spawndata")).resolve("0_0_0.dat");
        java.nio.file.Path target = Files.write(root.resolve("changing.dat"), new byte[] { 1, 2, 3 });
        try {
            try {
                Files.createSymbolicLink(link, target.toAbsolutePath());
            } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
                org.junit.Assume.assumeNoException(unsupported);
            }
            ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(root.toFile());
            assertEquals(2, snapshot.entries.size());
            assertEquals(6, snapshot.spool.length());
            for (ZipEntry entry : snapshot.entries) assertEquals(3, entry.getSize());
        } finally {
            Files.deleteIfExists(link);
            ThreadBackup.deleteSnapshot();
            FileUtils.delete(root.toFile());
        }
    }

    @Test
    public void failedSpoolArchivesPreservePreviousBackupAndCleanUp() throws Exception {
        File source = Files.createTempDirectory(new File("build").toPath(), "spool-failure-").toFile();
        Files.write(new File(source, "player.dat").toPath(), new byte[] { 1, 2, 3 });
        Files.write(new File(source, "level.dat").toPath(), new byte[] { 4, 5, 6 });
        File archive = new File(BackupTask.BACKUP_FOLDER, "spool-failure.zip");
        byte[] previous = { 7, 8, 9 };
        cpw.mods.fml.common.FMLCommonHandler fml = cpw.mods.fml.common.FMLCommonHandler.instance();
        Field delegate = cpw.mods.fml.common.FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        delegate.setAccessible(true);
        Object previousDelegate = delegate.get(fml);
        MinecraftServer server = mock(MinecraftServer.class);
        ServerConfigurationManager manager = mock(ServerConfigurationManager.class);
        Field players = ServerConfigurationManager.class.getDeclaredField("playerEntityList");
        players.setAccessible(true);
        players.set(manager, Collections.emptyList());
        when(server.getConfigurationManager()).thenReturn(manager);
        cpw.mods.fml.common.IFMLSidedHandler side = mock(cpw.mods.fml.common.IFMLSidedHandler.class);
        when(side.getServer()).thenReturn(server);
        delegate.set(fml, side);
        try {
            for (boolean legacy : new boolean[] { false, true }) {
                for (String failure : new String[] { "truncate", "corrupt", "append", "cancel", "close",
                        "missing-deferred" }) {
                    java.nio.file.Path deferred = source.toPath().resolve("AE2/spawndata/0_0_0.dat");
                    Files.createDirectories(deferred.getParent());
                    Files.write(deferred, new byte[] { 42 });
                    ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(source);
                    Files.write(archive.toPath(), previous);
                    if (failure.equals("missing-deferred")) Files.delete(deferred);
                    if (failure.equals("truncate")) Files.write(snapshot.spool.toPath(), new byte[0]);
                    if (failure.equals("corrupt")) {
                        byte[] bytes = Files.readAllBytes(snapshot.spool.toPath());
                        bytes[0] ^= 1;
                        Files.write(snapshot.spool.toPath(), bytes);
                    }
                    if (failure.equals("append"))
                        Files.write(snapshot.spool.toPath(), new byte[] { 0 }, java.nio.file.StandardOpenOption.APPEND);
                    ICompress compressor = org.mockito.Mockito.spy(
                            legacy ? new serverutils.lib.util.compression.LegacyCompressor()
                                    : new serverutils.lib.util.compression.CommonsCompressor());
                    if (failure.equals("cancel")) {
                        doAnswer(call -> {
                            call.callRealMethod();
                            Thread.currentThread().interrupt();
                            return null;
                        }).when(compressor).addStreamToArchive(
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any());
                    }
                    if (failure.equals("close")) {
                        doAnswer(call -> {
                            call.callRealMethod();
                            throw new IOException("injected close failure");
                        }).when(compressor).close();
                    }
                    ThreadBackup worker = new ThreadBackup(
                            compressor,
                            source,
                            "spool-failure",
                            Collections.emptySet(),
                            snapshot);
                    BackupTask.thread = worker;
                    java.util.concurrent.atomic.AtomicReference<Throwable> uncaught = new java.util.concurrent.atomic.AtomicReference<>();
                    worker.setUncaughtExceptionHandler((thread, error) -> uncaught.set(error));
                    worker.start();
                    waitForBackup();
                    assertNull("Backup failure should be handled by the worker", uncaught.get());
                    org.junit.Assert.assertArrayEquals(previous, Files.readAllBytes(archive.toPath()));
                    assertFalse(
                            "Worker must remove its snapshot after failure",
                            snapshot.spool.getParentFile().exists());
                    try (java.util.stream.Stream<java.nio.file.Path> paths = Files
                            .list(BackupTask.BACKUP_FOLDER.toPath())) {
                        assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".su-save-")));
                    }
                }
            }
            Thread.currentThread().interrupt();
            try {
                org.junit.Assert
                        .assertThrows(java.io.InterruptedIOException.class, () -> ThreadBackup.snapshotFiles(source));
                assertFalse(new File(BackupTask.BACKUP_TEMP_FOLDER, "snapshot").exists());
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
        } finally {
            BackupTask.stopBackupThread();
            delegate.set(fml, previousDelegate);
            ThreadBackup.deleteSnapshot();
            FileUtils.delete(source);
            Files.deleteIfExists(archive.toPath());
        }
    }

    @Test
    public void archiveReplacementKeepsPreviousBackupUntilCloseSucceeds() throws Exception {
        File source = Files.createTempDirectory(new File("build").toPath(), "archive-replace-").toFile();
        File payload = new File(source, "level.dat");
        Files.write(payload.toPath(), new byte[] { 42 });
        Files.createDirectories(BackupTask.BACKUP_FOLDER.toPath());
        File previous = new File(BackupTask.BACKUP_FOLDER, "reused-name.zip");
        byte[] original = { 1, 2, 3 };
        try {
            for (boolean failOnClose : new boolean[] { false, true }) {
                Files.write(previous.toPath(), original);
                ICompress compressor = new serverutils.lib.util.compression.LegacyCompressor() {

                    @Override
                    public void addFileToArchive(File file, String name) throws IOException {
                        super.addFileToArchive(file, name);
                        org.junit.Assert.assertArrayEquals(original, Files.readAllBytes(previous.toPath()));
                        if (!failOnClose) throw new java.io.InterruptedIOException("cancelled while writing");
                    }

                    @Override
                    public void close() throws Exception {
                        super.close();
                        if (failOnClose) throw new java.io.InterruptedIOException("cancelled during close");
                    }
                };
                ThreadBackup.doBackup(compressor, source, "reused-name", Collections.emptySet());
                org.junit.Assert.assertArrayEquals(original, Files.readAllBytes(previous.toPath()));
                try (java.util.stream.Stream<java.nio.file.Path> paths = Files
                        .list(BackupTask.BACKUP_FOLDER.toPath())) {
                    assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".su-save-")));
                }
            }
            ThreadBackup.doBackup(ICompress.createCompressor(), source, "reused-name", Collections.emptySet());
            try (ZipFile zip = new ZipFile(previous)) {
                try (InputStream in = zip.getInputStream(zip.getEntry(FileUtils.getRelativePath(payload)))) {
                    org.junit.Assert.assertArrayEquals(new byte[] { 42 }, IOUtils.toByteArray(in));
                }
            }
        } finally {
            FileUtils.delete(source);
            Files.deleteIfExists(previous.toPath());
        }
    }

    @Test
    public void startupReclaimsOnlyAbandonedArchiveFiles() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "archive-cleanup-");
        try {
            java.nio.file.Path orphan = Files.write(root.resolve(".su-save-123.tmp"), new byte[] { 1 });
            java.nio.file.Path archive = Files.write(root.resolve("completed.zip"), new byte[] { 2 });
            java.nio.file.Path unrelated = Files.write(root.resolve("other.tmp"), new byte[] { 3 });
            java.nio.file.Path directory = Files.createDirectory(root.resolve(".su-save-directory.tmp"));
            java.nio.file.Path contents = Files.write(directory.resolve("keep"), new byte[] { 4 });
            BackupTask.deleteAbandonedArchives(root.toFile());
            assertFalse(Files.exists(orphan));
            assertTrue(Files.exists(archive));
            assertTrue(Files.exists(unrelated));
            assertTrue(Files.exists(contents));
            BackupTask.deleteAbandonedArchives(root.toFile());
            assertTrue(Files.exists(archive));
        } finally {
            FileUtils.delete(root.toFile());
        }

        java.nio.file.Path active = FileUtils
                .createSaveTemporary(new File(BackupTask.BACKUP_FOLDER, "active.zip").toPath());
        int previousCount = ServerUtilitiesConfig.backups.backups_to_keep;
        boolean previousCustom = ServerUtilitiesConfig.backups.delete_custom_name_backups;
        try {
            ServerUtilitiesConfig.backups.backups_to_keep = 0;
            ServerUtilitiesConfig.backups.delete_custom_name_backups = true;
            BackupTask.clearOldBackups();
            assertTrue("Ordinary retention must leave worker-owned staging alone", Files.exists(active));
        } finally {
            ServerUtilitiesConfig.backups.backups_to_keep = previousCount;
            ServerUtilitiesConfig.backups.delete_custom_name_backups = previousCustom;
            Files.deleteIfExists(active);
        }
    }

    @Test
    public void globTraversalSkipsUnrelatedUnreadableDirectories() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "glob-traversal-");
        try {
            File selected = root.resolve("server.cfg").toFile();
            Files.write(selected.toPath(), new byte[] { 42 });
            File unreadable = mock(File.class);
            when(unreadable.toPath()).thenReturn(Files.createDirectory(root.resolve("private")));
            when(unreadable.listFiles()).thenReturn(null);
            File matchingDirectory = mock(File.class);
            when(matchingDirectory.toPath()).thenReturn(Files.createDirectory(root.resolve("private.cfg")));
            when(matchingDirectory.listFiles()).thenReturn(null);
            File directory = mock(File.class);
            when(directory.toPath()).thenReturn(root);
            when(directory.listFiles()).thenReturn(new File[] { selected, unreadable, matchingDirectory });
            String pattern = root.toString().replace('\\', '/') + "/*.cfg";
            Map<File, java.nio.file.attribute.BasicFileAttributes> files = new java.util.LinkedHashMap<>();
            ThreadBackup.collectOutsideBackupStorage(
                    files,
                    directory,
                    root.resolve("temp"),
                    root.resolve("backups"),
                    java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + pattern),
                    ThreadBackup.backupGlobTraversal(pattern));
            assertEquals(Collections.singleton(selected), files.keySet());
            assertEquals(1, files.get(selected).size());
            org.mockito.Mockito.verify(unreadable, org.mockito.Mockito.never()).listFiles();
            org.mockito.Mockito.verify(matchingDirectory, org.mockito.Mockito.never()).listFiles();
            org.junit.Assert.assertThrows(
                    IOException.class,
                    () -> ThreadBackup.collectOutsideBackupStorage(
                            files,
                            directory,
                            root.resolve("temp"),
                            root.resolve("backups"),
                            path -> true,
                            ThreadBackup.backupGlobTraversal(root.toString().replace('\\', '/') + "/**")));
        } finally {
            FileUtils.delete(root.toFile());
        }
    }

    @Test
    public void snapshotRejectsFilesChangedAfterListingAndCleansUp() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "cached-attributes-");
        File payload = root.resolve("player.dat").toFile();
        File laterDirectory = mock(File.class);
        when(laterDirectory.toPath()).thenReturn(Files.createDirectory(root.resolve("later")));
        File source = mock(File.class);
        when(source.toPath()).thenReturn(root);
        // Control traversal order so the source changes after its attributes have been collected.
        when(source.listFiles()).thenReturn(new File[] { payload, laterDirectory });
        try {
            for (int size : new int[] { 1, 8, -1 }) {
                Files.write(payload.toPath(), new byte[] { 1, 2, 3 });
                doAnswer(invocation -> {
                    if (size < 0) Files.delete(payload.toPath());
                    else Files.write(payload.toPath(), new byte[size]);
                    return new File[0];
                }).when(laterDirectory).listFiles();
                org.junit.Assert.assertThrows(IOException.class, () -> ThreadBackup.snapshotFiles(source));
                assertFalse(
                        "Failed capture must remove its spool",
                        new File(BackupTask.BACKUP_TEMP_FOLDER, "snapshot").exists());
            }
        } finally {
            ThreadBackup.deleteSnapshot();
            FileUtils.delete(root.toFile());
        }
    }

    @Test
    public void snapshotFollowsOrdinaryWindowsJunctionsAndExcludesBackupJunctions() throws Exception {
        org.junit.Assume.assumeTrue(File.separatorChar == '\\');
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "junction-world-")
                .toAbsolutePath();
        java.nio.file.Path world = Files.createDirectory(root.resolve("world"));
        java.nio.file.Path ordinary = Files.createDirectory(root.resolve("ordinary"));
        java.nio.file.Path included = Files.createDirectories(world.resolve("AE2")).resolve("spawndata");
        java.nio.file.Path excluded = world.resolve("excluded");
        java.nio.file.Path backup = BackupTask.BACKUP_FOLDER.toPath().toAbsolutePath();
        Files.createDirectories(backup);
        java.nio.file.Path marker = Files.createTempFile(backup, "junction-excluded-", ".dat");
        try {
            Files.write(ordinary.resolve("keep.dat"), new byte[] { 42 });
            for (java.nio.file.Path[] junction : new java.nio.file.Path[][] { { included, ordinary },
                    { excluded, backup } }) {
                Process process = new ProcessBuilder(
                        "cmd.exe",
                        "/c",
                        "mklink",
                        "/J",
                        junction[0].toString(),
                        junction[1].toString()).redirectErrorStream(true).start();
                String output;
                try (InputStream input = process.getInputStream()) {
                    output = new String(IOUtils.toByteArray(input), StandardCharsets.UTF_8);
                }
                assertEquals(output, 0, process.waitFor());
            }
            ThreadBackup.Snapshot snapshot = ThreadBackup.snapshotFiles(world.toFile());
            String expected = FileUtils.getRelativePath(included.resolve("keep.dat").toFile());
            assertEquals(Collections.singleton(expected), snapshot.files.keySet());
            assertEquals(1, snapshot.entries.size());
            org.junit.Assert.assertArrayEquals(new byte[] { 42 }, Files.readAllBytes(snapshot.spool.toPath()));
        } finally {
            // Remove the junctions themselves before recursive cleanup; their targets remain owned separately.
            Files.deleteIfExists(included);
            Files.deleteIfExists(excluded);
            Files.deleteIfExists(marker);
            ThreadBackup.deleteSnapshot();
            FileUtils.delete(root.toFile());
        }
    }

    @Test
    public void additionalGlobsIncludeOnlySelectedFilesInArchive() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "glob-archive-");
        String[] previous = ServerUtilitiesConfig.backups.additional_backup_files;
        File archive = new File(BackupTask.BACKUP_FOLDER, "glob-selection.zip");
        try {
            java.nio.file.Path world = Files.createDirectory(root.resolve("world"));
            Files.write(world.resolve("level.dat"), new byte[] { 1 });
            java.nio.file.Path config = Files.createDirectory(root.resolve("config"));
            java.nio.file.Path selected = Files.write(config.resolve("server.cfg"), new byte[] { 2 });
            java.nio.file.Path ignored = Files.createDirectory(config.resolve("private.cfg")).resolve("hidden.cfg");
            Files.write(ignored, new byte[] { 3 });
            java.nio.file.Path maps = Files.createDirectories(root.resolve("maps/user/world_1/dimension"));
            java.nio.file.Path selectedMap = Files.write(maps.resolve("map.dat"), new byte[] { 4 });
            java.nio.file.Path other = Files.createDirectories(root.resolve("maps/user/other_world"))
                    .resolve("map.dat");
            Files.write(other, new byte[] { 5 });
            String prefix = root.toString().replace('\\', '/');
            ServerUtilitiesConfig.backups.additional_backup_files = new String[] { prefix + "/config/*.cfg",
                    prefix + "/maps/*/world_*/**" };
            ThreadBackup
                    .doBackup(ICompress.createCompressor(), world.toFile(), "glob-selection", Collections.emptySet());
            try (ZipFile zip = new ZipFile(archive)) {
                assertTrue(zip.getEntry(FileUtils.getRelativePath(selected.toFile())) != null);
                assertTrue(zip.getEntry(FileUtils.getRelativePath(selectedMap.toFile())) != null);
                assertNull(zip.getEntry(FileUtils.getRelativePath(ignored.toFile())));
                assertNull(zip.getEntry(FileUtils.getRelativePath(other.toFile())));
            }
        } finally {
            ServerUtilitiesConfig.backups.additional_backup_files = previous;
            FileUtils.delete(root.toFile());
            Files.deleteIfExists(archive.toPath());
        }
    }

    @Test
    public void globTraversalKeepsMatchingAncestorsAndRecursivePatterns() {
        java.nio.file.PathMatcher selectedWorld = ThreadBackup
                .backupGlobTraversal("visualprospecting/client/*/world_*/**");
        for (String path : new String[] { "visualprospecting/client/user", "visualprospecting/client/user/world_1",
                "visualprospecting/client/user/world_1/dimension/data" }) {
            assertTrue(path, selectedWorld.matches(java.nio.file.Paths.get(path)));
        }
        assertFalse(selectedWorld.matches(java.nio.file.Paths.get("visualprospecting/client/user/other_world")));
        assertFalse(
                ThreadBackup.backupGlobTraversal("config/*.cfg").matches(java.nio.file.Paths.get("config/private")));
        assertTrue(
                ThreadBackup.backupGlobTraversal("config/**/settings.cfg")
                        .matches(java.nio.file.Paths.get("config/a/b")));
        assertTrue(
                ThreadBackup.backupGlobTraversal("config/name**tail/settings.cfg")
                        .matches(java.nio.file.Paths.get("config/name/a/b")));
        assertTrue(
                ThreadBackup.backupGlobTraversal("config/{one,two}/*/settings.cfg")
                        .matches(java.nio.file.Paths.get("config/two/sub")));
        assertTrue(
                ThreadBackup.backupGlobTraversal("config/{one/sub,two}/settings.cfg")
                        .matches(java.nio.file.Paths.get("config/one")));
        assertTrue(
                ThreadBackup.backupGlobTraversal("config/[ab]/*/settings.cfg")
                        .matches(java.nio.file.Paths.get("config/a/sub")));
    }

    @Test
    public void unreadableWorldDirectoryFailsButMissingOptionalIncludesAreAllowed() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "enumeration-");
        File unreadable = mock(File.class);
        when(unreadable.toPath()).thenReturn(root);
        when(unreadable.listFiles()).thenReturn(null);
        String[] previous = ServerUtilitiesConfig.backups.additional_backup_files;
        try {
            IOException failure = org.junit.Assert
                    .assertThrows(IOException.class, () -> ThreadBackup.snapshotFiles(unreadable));
            assertTrue(failure.getMessage().contains("Cannot list backup directory"));
            org.junit.Assert.assertThrows(
                    IOException.class,
                    () -> ThreadBackup.snapshotFiles(root.resolve("missing-world").toFile()));
            File payload = root.resolve("level.dat").toFile();
            Files.write(payload.toPath(), new byte[] { 42 });
            ServerUtilitiesConfig.backups.additional_backup_files = new String[] {
                    root.resolve("missing-optional").toString().replace('\\', '/') + "/**" };
            ThreadBackup
                    .doBackup(ICompress.createCompressor(), root.toFile(), "missing-optional", Collections.emptySet());
            try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "missing-optional.zip"))) {
                assertTrue(zip.getEntry(FileUtils.getRelativePath(payload)) != null);
            }
        } finally {
            ServerUtilitiesConfig.backups.additional_backup_files = previous;
            FileUtils.delete(root.toFile());
            ThreadBackup.deleteSnapshot();
        }
    }

    @Test
    public void publishedArchivesKeepOrdinaryAndExistingPosixPermissions() throws Exception {
        java.nio.file.Path root = Files.createTempDirectory(new File("build").toPath(), "backup-mode-");
        try {
            org.junit.Assume.assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
            File payload = root.resolve("level.dat").toFile();
            Files.write(payload.toPath(), new byte[] { 42 });
            java.nio.file.Path archive = new File(BackupTask.BACKUP_FOLDER, "posix-permissions.zip").toPath();
            try {
                ThreadBackup.doBackup(
                        ICompress.createCompressor(),
                        root.toFile(),
                        "posix-permissions",
                        Collections.emptySet());
                assertEquals(Files.getPosixFilePermissions(payload.toPath()), Files.getPosixFilePermissions(archive));
                java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions = java.nio.file.attribute.PosixFilePermissions
                        .fromString("rw-r-----");
                Files.setPosixFilePermissions(archive, permissions);
                ThreadBackup.doBackup(
                        ICompress.createCompressor(),
                        root.toFile(),
                        "posix-permissions",
                        Collections.emptySet());
                assertEquals(permissions, Files.getPosixFilePermissions(archive));
            } finally {
                Files.deleteIfExists(archive);
            }
        } finally {
            FileUtils.delete(root.toFile());
        }
    }

    private static void setCurrentServer(MinecraftServer server) throws ReflectiveOperationException {
        Field field = MinecraftServer.class.getDeclaredField("mcServer");
        field.setAccessible(true);
        field.set(null, server);
    }
}
