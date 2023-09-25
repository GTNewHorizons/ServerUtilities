package serverutils.utils.gui;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkCoordIntPair;

import org.lwjgl.opengl.GL11;

import serverutils.lib.lib.client.CachedVertexData;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.gui.Button;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.gui.GuiIcons;
import serverutils.lib.lib.gui.Panel;
import serverutils.lib.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.lib.gui.misc.GuiChunkSelectorBase;
import serverutils.lib.lib.icon.Icon;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.lib.lib.util.misc.MouseButton;
import serverutils.utils.net.MessageClaimedChunksModify;
import serverutils.utils.net.MessageClaimedChunksRequest;

public class GuiClaimedChunks extends GuiChunkSelectorBase {

    public static GuiClaimedChunks instance;
    public static final ClientClaimedChunks.ChunkData[] chunkData = new ClientClaimedChunks.ChunkData[ChunkSelectorMap.TILES_GUI
            * ChunkSelectorMap.TILES_GUI];
    public static int claimedChunks, loadedChunks, maxClaimedChunks, maxLoadedChunks;
    private static final ClientClaimedChunks.ChunkData NULL_CHUNK_DATA = new ClientClaimedChunks.ChunkData(
            new ClientClaimedChunks.Team((short) 0),
            0);

    public static final CachedVertexData AREA = new CachedVertexData(GL11.GL_QUADS, false, true, false);

    @Nullable
    public static ClientClaimedChunks.ChunkData getAt(int x, int y) {
        int i = x + y * ChunkSelectorMap.TILES_GUI;
        return i < 0 || i >= chunkData.length ? null : chunkData[i];
    }

    public static boolean hasBorder(ClientClaimedChunks.ChunkData data, @Nullable ClientClaimedChunks.ChunkData with) {
        if (with == null) {
            with = NULL_CHUNK_DATA;
        }

        return (data.flags != with.flags || data.team != with.team) && !with.isLoaded();
    }

    private static abstract class ButtonSide extends Button {

        public ButtonSide(Panel panel, String text, Icon icon) {
            super(panel, text, icon);
            setSize(20, 20);
        }
    }

    private final String currentDimName;

    public GuiClaimedChunks() {
        currentDimName = ServerUtils.getDimensionName(Minecraft.getMinecraft().theWorld.provider.dimensionId)
                .getFormattedText();
    }

    @Override
    public void onPostInit() {
        new MessageClaimedChunksRequest(startX, startZ).sendToServer();
        ChunkSelectorMap.getMap().resetMap(startX, startZ);
    }

    @Override
    public int getSelectionMode(MouseButton button) {
        boolean claim = !isShiftKeyDown();
        boolean flag = button.isLeft();

        if (isCtrlKeyDown()) {
            blockMode = true;
        }

        if (flag) {
            return claim ? MessageClaimedChunksModify.CLAIM : MessageClaimedChunksModify.LOAD;
        } else {
            return claim ? MessageClaimedChunksModify.UNCLAIM : MessageClaimedChunksModify.UNLOAD;
        }
    }

    @Override
    public void onChunksSelected(Collection<ChunkCoordIntPair> chunks) {
        new MessageClaimedChunksModify(startX, startZ, currentSelectionMode, chunks).sendToServer();
    }

    @Override
    public void drawArea(Tessellator tessellator) {
        AREA.draw(tessellator);
    }

    @Override
    public void addCornerButtons(Panel panel) {
        panel.add(new ButtonSide(panel, I18n.format("gui.close"), GuiIcons.ACCEPT) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                getGui().closeGui();
            }
        });

        panel.add(new ButtonSide(panel, I18n.format("selectServer.refresh"), GuiIcons.REFRESH) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                new MessageClaimedChunksRequest(startX, startZ).sendToServer();
                ChunkSelectorMap.getMap().resetMap(startX, startZ);
            }
        });

        if (maxClaimedChunks >= 0) {
            panel.add(
                    new ButtonSide(
                            panel,
                            I18n.format("serverutilities.lang.chunks.unclaim_all_dim", currentDimName),
                            GuiIcons.REMOVE) {

                        @Override
                        public void onClicked(MouseButton button) {
                            GuiHelper.playClickSound();
                            String s = I18n.format("serverutilities.lang.chunks.unclaim_all_dim_q", currentDimName);
                            openYesNo(
                                    s,
                                    "",
                                    () -> ClientUtils.execClientCommand(
                                            "/chunks unclaim_all "
                                                    + Minecraft.getMinecraft().theWorld.provider.dimensionId));
                        }
                    });

            panel.add(new ButtonSide(panel, I18n.format("serverutilities.lang.chunks.unclaim_all"), GuiIcons.REMOVE) {

                @Override
                public void onClicked(MouseButton button) {
                    GuiHelper.playClickSound();
                    String s = I18n.format("serverutilities.lang.chunks.unclaim_all_q");
                    openYesNo(s, "", () -> ClientUtils.execClientCommand("/chunks unclaim_all"));
                }
            });
        }

        panel.add(new ButtonSide(panel, I18n.format("gui.info"), GuiIcons.INFO) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                handleClick("https://github.com/GTNewHorizons/ServerUtilities/wiki");
            }
        });
    }

    @Override
    public void addCornerText(List<String> list, Corner corner) {
        if (maxClaimedChunks < 0) {
            if (corner == Corner.BOTTOM_RIGHT) {
                if (maxClaimedChunks == -2) {
                    list.add(EnumChatFormatting.RED + I18n.format("serverutilitieslib.lang.team.error.no_team"));
                } else {
                    list.add(EnumChatFormatting.RED + I18n.format("feature_disabled_server"));
                }
            }

            return;
        }

        switch (corner) {
            case BOTTOM_RIGHT:
                list.add(
                        I18n.format(
                                "serverutilities.lang.chunks.claimed_count",
                                claimedChunks,
                                maxClaimedChunks == Integer.MAX_VALUE ? "\u221E" : Integer.toString(maxClaimedChunks)));
                list.add(
                        I18n.format(
                                "serverutilities.lang.chunks.loaded_count",
                                loadedChunks,
                                maxLoadedChunks == Integer.MAX_VALUE ? "\u221E" : Integer.toString(maxLoadedChunks)));
                break;
        }
    }

    @Override
    public void addButtonText(GuiChunkSelectorBase.MapButton button, List<String> list) {
        ClientClaimedChunks.ChunkData data = chunkData[button.index];

        if (data != null) {
            list.add(data.team.nameComponent.getFormattedText());
            list.add(EnumChatFormatting.GREEN + I18n.format("serverutilities.lang.chunks.claimed_area"));

            if (data.isLoaded()) {
                list.add(EnumChatFormatting.RED + I18n.format("serverutilities.lang.chunks.upgrade.loaded"));
            }
        } else {
            list.add(EnumChatFormatting.DARK_GREEN + I18n.format("serverutilities.lang.chunks.wilderness"));
        }

        if (isCtrlKeyDown()) {
            list.add(button.chunkPos.toString());
        }
    }
}
