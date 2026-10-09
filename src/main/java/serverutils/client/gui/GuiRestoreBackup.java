package serverutils.client.gui;

import static serverutils.ServerUtilitiesConfig.backups;


import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.storage.SaveFormatComparator;
import net.minecraftforge.client.event.GuiScreenEvent;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.ButtonContainer;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.WidgetLayout;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.TimeUtil;
import serverutils.lib.util.backup.Snapshot;
import serverutils.lib.util.backup.SnapshotManifest;
import serverutils.lib.util.backup.SnapshotRemover;
import serverutils.lib.util.backup.SnapshotRestorer;
import serverutils.lib.util.backup.SnapshotStore;
import serverutils.lib.util.compression.ICompress;
import serverutils.lib.util.misc.MouseButton;
import serverutils.task.backup.BackupTask;
import serverutils.task.backup.ThreadBackup;

@EventBusSubscriber(side = Side.CLIENT)
public class GuiRestoreBackup extends GuiButtonListBase {

    // Root locale, BackupTask.BACKUP_NAME_PATTERN only matches ASCII digits.
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ROOT);
    private static final Map<File, BasicFileAttributes> allBackupFiles = new Object2ObjectOpenHashMap<>();
    private static Object2ObjectMap<String, List<File>> worldBackups;
    private final List<File> backupFiles;
    private final String title;
    private final Button backButton, recreateWorldButton;
    private final String worldName;
    private SnapshotStore snapshotStore;

    private static final Set<String> worldsWithSnapshots = new ObjectOpenHashSet<>();

    public GuiRestoreBackup(String worldName, GuiSelectWorld selectWorld) {
        File snapshotDir = SnapshotStore.backupDirectoryFor(BackupTask.BACKUP_FOLDER, new File(worldName));

        try {
            snapshotStore = SnapshotStore.load(snapshotDir);
        } catch (IOException e) {
            ServerUtilities.LOGGER.error("Failed to load snapshot store for world {}", worldName, e);
        }

        this.worldName = worldName;
        this.backupFiles = worldBackups.getOrDefault(worldName, new ObjectArrayList<>());
        this.title = StatCollector.translateToLocalFormatted("serverutilities.gui.backup.title", worldName);
        backupFiles.sort(Comparator.comparing(File::lastModified).reversed());
        backButton = new SimpleTextButton(this, StatCollector.translateToLocal("gui.cancel"), GuiIcons.CANCEL) {

            @Override
            public void onClicked(MouseButton button) {
                closeGui();
            }
        };

        recreateWorldButton = new SimpleTextButton(
                this,
                StatCollector.translateToLocal("selectWorld.recreate"),
                GuiIcons.REFRESH) {

            @Override
            public void onClicked(MouseButton button) {
                try {
                    // Button doesn't matter, it just needs to have an id of 7
                    ReflectionHelper.findMethod(
                            GuiSelectWorld.class,
                            null,
                            new String[] { "func_146284_a", "actionPerformed" },
                            GuiButton.class).invoke(selectWorld, new GuiButton(7, 0, 0, 0, 0, ""));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @EventBusSubscriber.Condition
    public static boolean shouldEventBusSubscribe() {
        return ServerUtilitiesConfig.backups.enable_backups;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SuppressWarnings("unchecked")
    public static void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (worldBackups == null) {
            worldBackups = new Object2ObjectOpenHashMap<>();
            preProcess();
        }

        if (event.gui instanceof GuiSelectWorld gui) {
            findWorldsWithSnapshots();
            if (needsRefresh()) {
                worldBackups.clear();
                allBackupFiles.clear();
                preProcess();
            }

            // Don't add the button if it's too big to fit on the screen
            if (event.gui.width / 2 + 248 > event.gui.width) return;

            event.buttonList.add(
                    new GuiRestoreButton(
                            event.gui.width - 90,
                            event.gui.height - 52,
                            82,
                            20,
                            StatCollector.translateToLocal("serverutilities.gui.backup.button"),
                            gui));

            // Removes Aroma's "Backup" button
            gui.buttonList.removeIf(button -> button.id == 50);
        }
    }

    private static boolean needsRefresh() {
        File[] files = BackupTask.BACKUP_FOLDER.listFiles();
        // A missing folder must still clear backups cached from before it disappeared.
        if (files == null) return !allBackupFiles.isEmpty();
        if (files.length != allBackupFiles.size()) return true;
        for (File file : files) {
            BasicFileAttributes previous = allBackupFiles.get(file);
            if (previous == null) return true;
            try {
                BasicFileAttributes current = Files
                        .readAttributes(file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (current.size() != previous.size() || !current.lastModifiedTime().equals(previous.lastModifiedTime())
                        || !Objects.equals(current.fileKey(), previous.fileKey()))
                    return true;
            } catch (IOException e) {
                return true;
            }
        }
        return false;
    }

    private static void findWorldsWithSnapshots() {
        worldsWithSnapshots.clear();
        File[] directories = BackupTask.BACKUP_FOLDER.listFiles(File::isDirectory);
        if (directories == null) return;

        for (File directory : directories) {
            File[] manifests = directory
                    .listFiles(f -> f.isFile() && Snapshot.FILE_PATTERN.matcher(f.getName()).matches());
            if (manifests != null && manifests.length > 0) worldsWithSnapshots.add(directory.getName());
        }
    }

    private static void preProcess() {
        File[] files = BackupTask.BACKUP_FOLDER.listFiles();
        if (files == null) return;

        ICompress compressor = ICompress.createCompressor();
        for (File file : files) {
            if (file.isDirectory()) continue; // snapshot store
            try {
                allBackupFiles.put(
                        file,
                        Files.readAttributes(file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS));
                String worldName = compressor.getWorldName(file);
                if (worldName == null) continue;
                worldBackups.computeIfAbsent(worldName, k -> new ObjectArrayList<>()).add(file);
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void onPostInit() {
        setFullscreen();
        alignWidgets();
    }

    @Override
    public void alignWidgets() {
        backButton.setPos(9, 2);
        backButton.setHeight(15);
        recreateWorldButton.setPos(9 + backButton.width, 2);
        recreateWorldButton.setHeight(15);
        panelButtons.setPosAndSize(9, 20, width - 20 - scrollBar.width, height - 20);
        super.alignWidgets();
    }

    @Override
    public boolean onClosedByKey(int key) {
        if (super.onClosedByKey(key)) {
            closeGui();
            return true;
        }

        return false;
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        add(backButton);
        add(recreateWorldButton);
    }

    @Override
    public void addButtons(Panel panel) {
        addZipBackupButtons(panel);
        addSnapshotButtons(panel);
    }

    private void addZipBackupButtons(Panel panel) {
        for (File file : backupFiles) {
            BackupEntryRow row = new BackupEntryRow(panel, file.getName(), action -> {
                if (action == Action.Restore) {
                    loadBackupWorld(file);
                } else if (action == Action.RestoreGlobal) {
                    loadBackupGlobal(file);
                } else if (action == Action.Delete) {
                    deleteBackup(file);
                }
            });
            panel.add(row);
        }
    }

    private void addSnapshotButtons(Panel panel) {
        if (snapshotStore == null) return;
        for (SnapshotManifest manifest : this.snapshotStore.listManifest()) {
            String text = String.format(
                    "%s (%s)",
                    TimeUtil.format(manifest.getCreatedAt()),
                    TimeUtil.timeAgo(manifest.getCreatedAt().toInstant()));
            BackupEntryRow row = new BackupEntryRow(panel, text, action -> {
                Snapshot snapshot = snapshotStore.get(manifest);
                if (action == Action.Restore) {
                    restoreSnapshot(snapshot);
                } else if (action == Action.RestoreGlobal) {

                } else if (action == Action.Delete) {
                    deleteSnapshot(snapshot);
                }
            });
            panel.add(row);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void renameAdditionalFiles(File previousRoot, boolean includeGlobal, Map<File, File> moved, File archive,
            File preservedWorld) throws IOException {
        Path archivePath = FileUtils.resolveRealPath(archive.toPath());
        Path recoveryPath = FileUtils.resolveRealPath(previousRoot.getParentFile().toPath());
        Path preservedWorldPath = FileUtils.resolveRealPath(preservedWorld.toPath());
        Path recoveryStorage = FileUtils.resolveRealPath(Paths.get("backups_before_restore"));
        for (String pattern : backups.additional_backup_files) {
            if (!pattern.contains("$WORLDNAME") && !includeGlobal) {
                continue;
            }
            pattern = FileUtils.normalizeBackupPattern(pattern.replace("$WORLDNAME", worldName));

            // Gather list of all old files
            List<File> previousFiles;
            int firstWildcardIndex = pattern.indexOf('*');
            if (firstWildcardIndex == -1) {
                previousFiles = FileUtils.listTree(new File(pattern));
            } else {
                Path rootFolder = Paths.get(pattern.substring(0, firstWildcardIndex));

                // If wildcard was not at the start of a directory, get the parent
                if (firstWildcardIndex != 0 && (pattern.charAt(firstWildcardIndex - 1) != '/')) {
                    rootFolder = rootFolder.getParent();
                }
                if (rootFolder == null || rootFolder.toString().isEmpty()) rootFolder = Paths.get(".");

                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                List<File> fileCandidates = FileUtils.listTree(rootFolder.toFile());
                previousFiles = new ArrayList<>();
                for (File file : fileCandidates) {
                    if (matcher.matches(file.toPath().normalize())) {
                        previousFiles.add(file);
                    }
                }
            }

            // Move all old files into backup
            for (File file : previousFiles) {
                Path path = FileUtils.resolveRealPath(file.toPath());
                if (path.equals(archivePath) || path.startsWith(recoveryPath)
                        || path.startsWith(preservedWorldPath)
                        || path.startsWith(recoveryStorage)
                        || ThreadBackup.isBackupStorage(file))
                    continue;
                String pathRelative = FileUtils.getRelativePath(file);
                File destFile = new File(previousRoot, pathRelative);
                Files.createDirectories(destFile.toPath().getParent());
                Files.move(file.toPath(), destFile.toPath());
                moved.put(file, destFile);
            }
        }
    }

    private void restoreSnapshot(Snapshot snapshot) {
        openYesNo(
            StatCollector.translateToLocal("serverutilities.gui.backup.restore_confirm"),
            StatCollector.translateToLocal("serverutilities.gui.backup.restore_confirm_desc"),
            () -> {
                File savesDir = new File("saves/");
                File worldDir = new File(savesDir, worldName);
                try {
                    SnapshotRestorer.restore(snapshotStore, snapshot, worldDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                closeGui();
            });
    }

    private void loadBackupWorld(File file) {
        openYesNo(
                StatCollector.translateToLocal("serverutilities.gui.backup.restore_confirm"),
                StatCollector.translateToLocal("serverutilities.gui.backup.restore_confirm_desc"),
                () -> { loadBackup(file, false); });
    }

    private void loadBackupGlobal(File file) {
        openYesNo(
                StatCollector.translateToLocal("serverutilities.gui.backup.restore_global_confirm"),
                StatCollector.translateToLocal("serverutilities.gui.backup.restore_global_confirm_desc"),
                () -> { loadBackup(file, true); });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void loadBackup(File file, boolean includeGlobal) {
        File savesDir = new File("saves/");
        File worldDir = new File(savesDir, worldName);
        File saveCopy = new File(savesDir, worldName + "_old");

        while (saveCopy.exists()) {
            saveCopy = new File(savesDir, saveCopy.getName() + "_old");
        }
        File preservedWorld = saveCopy;

        boolean[] worldMoved = { false };
        Map<File, File> moved = new LinkedHashMap<>();
        File previousRoot = null;

        try (ICompress compressor = ICompress.createCompressor()) {
            boolean isOldBackup = compressor.isOldBackup(file);
            ICompress.validateRestoreTargets(file, worldName, isOldBackup, includeGlobal);
            Path worldPath = FileUtils.resolveRealPath(worldDir.toPath());
            Path archivePath = FileUtils.resolveRealPath(file.toPath());
            File movedArchive = archivePath.startsWith(worldPath)
                    ? new File(preservedWorld, worldPath.relativize(archivePath).toString())
                    : file;
            File recoveryStorage = new File("backups_before_restore/");
            Files.createDirectories(recoveryStorage.toPath());
            // One directory per restore, with separate roots for displaced extras and replaced globals.
            previousRoot = Files.createTempDirectory(
                    recoveryStorage.toPath(),
                    DATE_FORMAT.format(Calendar.getInstance().getTime()) + "-").toFile();
            File recovery = previousRoot;
            compressor.extractArchive(file, includeGlobal, isOldBackup, preservedWorld, recovery, () -> {
                Files.move(worldDir.toPath(), preservedWorld.toPath());
                worldMoved[0] = true;
                if (!isOldBackup) {
                    renameAdditionalFiles(
                            new File(recovery, "additional"),
                            includeGlobal,
                            moved,
                            movedArchive,
                            preservedWorld);
                }
                return null;
            });
            previousRoot.delete();
            closeGui();
        } catch (Exception e) {

            if (worldMoved[0]) {
                try {
                    if (worldDir.exists() && !FileUtils.delete(worldDir))
                        throw new IOException("Could not remove partial restored world");
                    Files.move(preservedWorld.toPath(), worldDir.toPath());
                } catch (IOException recovery) {
                    e.addSuppressed(recovery);
                }
            }
            for (Map.Entry<File, File> entry : moved.entrySet()) {
                try {
                    Files.move(entry.getValue().toPath(), entry.getKey().toPath());
                } catch (IOException recovery) {
                    e.addSuppressed(recovery);
                }
            }
            if (previousRoot != null) previousRoot.delete();
            ServerUtilities.LOGGER.error("Failed to restore backup; original files retained if recovery failed", e);
            Minecraft.getMinecraft().displayGuiScreen(
                    new GuiErrorScreen(
                            StatCollector.translateToLocal("serverutilities.gui.backup.error"),
                            EnumChatFormatting.RED + e.getMessage()));
        }
    }

    private void deleteBackup(File file) {
        openYesNo(StatCollector.translateToLocal("serverutilities.gui.backup.delete_confirm"), "", () -> {
            FileUtils.delete(file);
            backupFiles.remove(file);
        });
    }

    private void deleteSnapshot(Snapshot snapshot) {
        openYesNo(StatCollector.translateToLocal("serverutilities.gui.backup.delete_confirm"), "", () -> {
            try {
                SnapshotRemover.remove(snapshotStore, snapshot);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        super.drawBackground(theme, x, y, w, h);
        theme.drawString(
                EnumChatFormatting.BOLD + title,
                x + (width - theme.getStringWidth(title)) / 2,
                2 + theme.getFontHeight(),
                Theme.SHADOW);
    }

    @Override
    protected Panel createButtonPanel() {
        return new Panel(this) {

            @Override
            public void addWidgets() {
                addButtons(this);
            }

            @Override
            public void alignWidgets() {
                setY(21);

                for (Widget w : widgets) {
                    w.setX(0);
                    w.setWidth(width);
                }

                scrollBar.setPosAndSize(posX + width + 6, posY - 1, 16, height + 2);
                scrollBar.setMaxValue(align(new WidgetLayout.Vertical(0, 0, 0)));

                getGui().setWidth(scrollBar.posX + scrollBar.width + 8);
                getGui().setHeight(height + 18);
            }

            @Override
            public void drawBackground(Theme theme, int x, int y, int w, int h) {
                theme.drawPanelBackground(x, y, w, h);
            }
        };
    }

    private static class BackupEntryRow extends ButtonContainer {

        public BackupEntryRow(Panel panel, String txt, Consumer<Action> callback) {
            super(panel, txt, Icon.EMPTY);
            addSubButton(
                    new BackupEntryButton(
                            panel,
                            StatCollector.translateToLocal("serverutilities.gui.backup.restore"),
                            GuiIcons.ACCEPT,
                            () -> callback.accept(Action.Restore)));

            addSubButton(
                    new BackupEntryButton(
                            panel,
                            StatCollector.translateToLocal("serverutilities.gui.backup.restore_global"),
                            GuiIcons.ACCEPT,
                            () -> callback.accept(Action.RestoreGlobal)));

            addSubButton(
                    new BackupEntryButton(
                            panel,
                            StatCollector.translateToLocal("selectWorld.delete"),
                            GuiIcons.REMOVE,
                            () -> callback.accept(Action.Delete)));

            setXOffset(9);
        }
    }

    private static class BackupEntryButton extends SimpleTextButton {

        private final Runnable callback;

        public BackupEntryButton(Panel panel, String text, Icon icon, Runnable callback) {
            super(panel, text, icon);
            this.callback = callback;
        }

        @Override
        public void onClicked(MouseButton button) {
            callback.run();
        }
    }

    private static class GuiRestoreButton extends GuiButton {

        private final GuiSelectWorld gui;
        private String currentWorld;

        public GuiRestoreButton(int x, int y, int widthIn, int heightIn, String buttonText, GuiSelectWorld gui) {
            super(111, x, y, widthIn, heightIn, buttonText);
            this.gui = gui;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            int worldIndex = gui.field_146640_r;

            if (worldIndex == -1) {
                enabled = false;
            } else {
                currentWorld = ((SaveFormatComparator) gui.field_146639_s.get(worldIndex)).getFileName();
                enabled = worldBackups.containsKey(currentWorld) || worldsWithSnapshots.contains(currentWorld);
            }

            super.drawButton(mc, mouseX, mouseY);
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            if (!super.mousePressed(mc, mouseX, mouseY)) {
                return false;
            }

            new GuiRestoreBackup(currentWorld, gui).openGui();

            return true;
        }
    }

    private enum Action {
        Restore,
        RestoreGlobal,
        Delete
    }
}
