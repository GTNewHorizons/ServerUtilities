package serverutils.client.gui;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
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
import serverutils.lib.util.compression.ICompress;
import serverutils.lib.util.misc.MouseButton;
import serverutils.task.backup.BackupTask;

@EventBusSubscriber(side = Side.CLIENT)
public class GuiRestoreBackup extends GuiButtonListBase {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final Set<File> allBackupFiles = new ObjectOpenHashSet<>();
    private static Object2ObjectMap<String, List<File>> worldBackups;
    private final List<File> backupFiles;
    private final String title;
    private final Button backButton, recreateWorldButton;
    private final String worldName;

    public GuiRestoreBackup(String worldName, GuiSelectWorld selectWorld) {
        this.worldName = worldName;
        this.backupFiles = worldBackups.get(worldName);
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
        if (files == null) return false;
        return !allBackupFiles.containsAll(Arrays.asList(files));
    }

    private static void preProcess() {
        File[] files = BackupTask.BACKUP_FOLDER.listFiles();
        if (files == null) return;

        ICompress compressor = ICompress.createCompressor();
        for (File file : files) {
            allBackupFiles.add(file);
            try {
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
        for (File file : backupFiles) {
            ButtonContainer container = new ButtonContainer(panel, file.getName(), Icon.EMPTY);
            container.addSubButton(
                    new BackupEntryButton(
                            panel,
                            StatCollector.translateToLocal("serverutilities.gui.backup.restore"),
                            GuiIcons.ACCEPT,
                            file,
                            this::loadBackupWorld));
            container.addSubButton(
                    new BackupEntryButton(
                            panel,
                            StatCollector.translateToLocal("serverutilities.gui.backup.restore_global"),
                            GuiIcons.ACCEPT,
                            file,
                            this::loadBackupGlobal));
            container.addSubButton(
                    new BackupEntryButton(
                            panel,
                            StatCollector.translateToLocal("selectWorld.delete"),
                            GuiIcons.REMOVE,
                            file,
                            this::deleteBackup));
            container.setXOffset(9);
            panel.add(container);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void renameAdditionalFiles(File previousRoot, boolean includeGlobal) {
        for (String pattern : backups.additional_backup_files) {
            if (!pattern.contains("$WORLDNAME") && !includeGlobal) {
                continue;
            }
            pattern = pattern.replace("$WORLDNAME", worldName);

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

                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                List<File> fileCandidates = FileUtils.listTree(rootFolder.toFile());
                previousFiles = new ArrayList<>();
                for (File file : fileCandidates) {
                    if (matcher.matches(file.toPath())) {
                        previousFiles.add(file);
                    }
                }
            }

            // Move all old files into backup
            for (File file : previousFiles) {
                String pathRelative = FileUtils.getRelativePath(file);
                File destFile = new File(previousRoot, pathRelative);
                destFile.getParentFile().mkdirs();
                file.renameTo(destFile);
            }
        }
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

        worldDir.renameTo(saveCopy);

        try (ICompress compressor = ICompress.createCompressor()) {
            boolean isOldBackup = compressor.isOldBackup(file);
            if (!isOldBackup) {
                File previousRoot = new File("backups_before_restore/");
                previousRoot = new File(previousRoot, DATE_FORMAT.format(Calendar.getInstance().getTime()));
                renameAdditionalFiles(previousRoot, includeGlobal);
            }
            compressor.extractArchive(file, includeGlobal, isOldBackup);
            closeGui();
        } catch (Exception e) {
            ServerUtilities.LOGGER.error("Failed to restore backup", e);
            FileUtils.delete(worldDir);
            saveCopy.renameTo(worldDir);
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

    private static class BackupEntryButton extends SimpleTextButton {

        private final File file;
        private final Consumer<File> callback;

        public BackupEntryButton(Panel panel, String text, Icon icon, File file, Consumer<File> callback) {
            super(panel, text, icon);
            this.file = file;
            this.callback = callback;
        }

        @Override
        public void onClicked(MouseButton button) {
            callback.accept(file);
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
                enabled = worldBackups.containsKey(currentWorld);
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

}
