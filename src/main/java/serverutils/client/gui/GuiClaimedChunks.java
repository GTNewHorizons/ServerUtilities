package serverutils.client.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkCoordIntPair;

import org.lwjgl.opengl.GL11;

import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.client.CachedVertexData;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.gui.misc.GuiChunkSelectorBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageClaimedChunksModify;
import serverutils.net.MessageClaimedChunksRequest;
import serverutils.net.MessageClaimedChunksUpdate;

public class GuiClaimedChunks extends GuiChunkSelectorBase {

    public static final ClientClaimedChunks.ChunkData[] chunkData = new ClientClaimedChunks.ChunkData[ChunkSelectorMap.TILES_GUI
            * ChunkSelectorMap.TILES_GUI];
    private static final ClientClaimedChunks.ChunkData NULL_CHUNK_DATA = new ClientClaimedChunks.ChunkData(
            new ClientClaimedChunks.Team((short) 0),
            0);
    public static final CachedVertexData AREA = new CachedVertexData(GL11.GL_QUADS, false, true, false);
    public static final CachedVertexData LINES = new CachedVertexData(GL11.GL_LINES, false, true, false);
    public static GuiClaimedChunks instance;
    public static int claimedChunks, loadedChunks, maxClaimedChunks, maxLoadedChunks;
    private final String currentDimName;

    public GuiClaimedChunks() {
        currentDimName = ServerUtils.getDimensionName(Minecraft.getMinecraft().theWorld.provider.dimensionId)
                .getFormattedText();
    }

    @Nullable
    public static ClientClaimedChunks.ChunkData getAt(int x, int y) {
        int i = x + y * ChunkSelectorMap.TILES_GUI;
        return i < 0 || i >= chunkData.length ? null : chunkData[i];
    }

    public static boolean hasBorder(ClientClaimedChunks.ChunkData data, @Nullable ClientClaimedChunks.ChunkData with) {
        if (with == null) {
            with = NULL_CHUNK_DATA;
        }

        return (data.getFlags() != with.getFlags() || data.team != with.team) && !with.isLoaded();
    }

    @Override
    public void onPostInit() {
        new MessageClaimedChunksRequest(startX, startZ).sendToServer();
        ChunkSelectorMap.getMap().resetMap(startX, startZ);
    }

    public static void onChunkDataUpdate(MessageClaimedChunksUpdate m) {
        claimedChunks = m.claimedChunks;
        loadedChunks = m.loadedChunks;
        maxClaimedChunks = m.maxClaimedChunks;
        maxLoadedChunks = m.maxLoadedChunks;
        Arrays.fill(chunkData, null);

        for (ClientClaimedChunks.Team team : m.teams.values()) {
            for (Map.Entry<Integer, ClientClaimedChunks.ChunkData> entry : team.chunks.int2ObjectEntrySet()) {
                int x = entry.getKey() % ChunkSelectorMap.TILES_GUI;
                int z = entry.getKey() / ChunkSelectorMap.TILES_GUI;
                chunkData[x + z * ChunkSelectorMap.TILES_GUI] = entry.getValue();
            }
        }

        AREA.reset();
        LINES.reset();
        EnumTeamColor prevCol = null;
        ClientClaimedChunks.ChunkData data;

        for (int i = 0; i < chunkData.length; i++) {
            data = chunkData[i];

            if (data == null) {
                continue;
            }

            if (prevCol != data.team.color) {
                prevCol = data.team.color;
                AREA.color.set(data.team.color.getColor(), 150);
            }

            AREA.rect(
                    (i % ChunkSelectorMap.TILES_GUI) * TILE_SIZE,
                    (i / ChunkSelectorMap.TILES_GUI) * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE);
        }

        boolean borderU, borderD, borderL, borderR;

        for (int i = 0; i < chunkData.length; i++) {
            data = chunkData[i];

            if (data == null) {
                continue;
            }

            int x = i % ChunkSelectorMap.TILES_GUI;
            int dx = x * TILE_SIZE;
            int y = i / ChunkSelectorMap.TILES_GUI;
            int dy = y * TILE_SIZE;

            borderU = y > 0 && hasBorder(data, getAt(x, y - 1));
            borderD = y < (ChunkSelectorMap.TILES_GUI - 1) && hasBorder(data, getAt(x, y + 1));
            borderL = x > 0 && hasBorder(data, getAt(x - 1, y));
            borderR = x < (ChunkSelectorMap.TILES_GUI - 1) && hasBorder(data, getAt(x + 1, y));

            if (data.isLoaded()) {
                AREA.color.set(255, 80, 80, 230);
                if (ServerUtilitiesClientConfig.show_dotted_lines) {
                    int offset = TILE_SIZE / 2;
                    LINES.color.set(0, 0, 0, 90);

                    LINES.pos(dx, dy);
                    LINES.pos(dx + TILE_SIZE, dy + TILE_SIZE);

                    LINES.pos(dx, dy + offset);
                    LINES.pos(dx + offset, dy + TILE_SIZE);

                    LINES.pos(dx + offset, dy);
                    LINES.pos(dx + TILE_SIZE, dy + offset);

                }
            } else {
                AREA.color.set(80, 80, 80, 230);
            }

            if (borderU) {
                AREA.rect(dx, dy, TILE_SIZE, 1, 1);
            }

            if (borderD) {
                AREA.rect(dx, dy + TILE_SIZE - 1, TILE_SIZE, 1, 1);
            }

            if (borderL) {
                AREA.rect(dx, dy, 1, TILE_SIZE, 1);
            }

            if (borderR) {
                AREA.rect(dx + TILE_SIZE - 1, dy, 1, TILE_SIZE, 1);
            }
        }
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

        GL11.glEnable(GL11.GL_LINE_STIPPLE);
        GL11.glLineStipple(3, (short) 0xAAAA);
        GL11.glLineWidth(0.8F);

        LINES.draw(tessellator);

        GL11.glDisable(GL11.GL_LINE_STIPPLE);
        GL11.glLineWidth(1F);
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
                    list.add(EnumChatFormatting.RED + I18n.format("serverutilities.lang.team.error.no_team"));
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

    private static abstract class ButtonSide extends Button {

        public ButtonSide(Panel panel, String text, Icon icon) {
            super(panel, text, icon);
            setSize(20, 20);
        }
    }
}
