package serverutils.client.tab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import serverutils.ServerUtilitiesConfig;
import serverutils.client.gui.misc.GuiPlayerInfoWrapper;
import serverutils.lib.util.StringUtils;

public class ModernTabRenderer {

    public static final ModernTabRenderer INSTANCE = new ModernTabRenderer();

    private static final int MAX_PLAYERS = 80;
    private static final int MAX_ROWS = 20;
    private static final int ENTRY_HEIGHT = 9;
    private static final int HEAD_SIZE = 8;
    private static final int HEAD_PADDING = 9;
    private static final int PING_ICON_WIDTH = 10;
    private static final int PING_ICON_HEIGHT = 8;

    private static final int ENTRY_BG_COLOR = 553648127;
    private static final int OVERLAY_BG_COLOR = Integer.MIN_VALUE;

    private ModernTabRenderer() {}

    public void render(ScaledResolution resolution) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        NetHandlerPlayClient handler = mc.thePlayer.sendQueue;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.func_96539_a(0);

        int screenWidth = resolution.getScaledWidth();

        @SuppressWarnings("unchecked")
        List<GuiPlayerInfo> allPlayers = (List<GuiPlayerInfo>) handler.playerInfoList;
        int playerCount = Math.min(allPlayers.size(), MAX_PLAYERS);
        if (playerCount == 0 && objective == null) return;

        TabDisplayHandler displayHandler = TabDisplayHandler.INSTANCE;

        List<GuiPlayerInfo> players;
        Map<String, Integer> sortMap = displayHandler.getSortIndexMap();
        if (sortMap != null && !sortMap.isEmpty()) {
            players = new ArrayList<>(allPlayers.subList(0, playerCount));
            players.sort((a, b) -> {
                Integer idxA = sortMap.get(a.name);
                Integer idxB = sortMap.get(b.name);
                if (idxA == null && idxB == null) return a.name.compareToIgnoreCase(b.name);
                if (idxA == null) return 1;
                if (idxB == null) return -1;
                return Integer.compare(idxA, idxB);
            });
        } else {
            players = new ArrayList<>(allPlayers.subList(0, playerCount));
        }

        int maxNameWidth = 0;
        for (GuiPlayerInfo player : players) {
            int w = font.getStringWidth(resolveDisplayName(player, scoreboard, displayHandler));
            if (w > maxNameWidth) maxNameWidth = w;
        }

        int maxScoreWidth = 0;
        if (objective != null) {
            for (GuiPlayerInfo player : players) {
                Score score = objective.getScoreboard().func_96529_a(player.name, objective);
                String scoreStr = EnumChatFormatting.YELLOW + "" + score.getScorePoints();
                int w = font.getStringWidth(scoreStr);
                if (w > maxScoreWidth) maxScoreWidth = w;
            }
            maxScoreWidth += 5;
        }

        boolean showPingNumber = ServerUtilitiesConfig.tab.showPingNumber;
        boolean showPingBars = ServerUtilitiesConfig.tab.showPingBars;
        int maxPingTextWidth = 0;
        if (showPingNumber) {
            for (GuiPlayerInfo player : players) {
                String pingText = formatPing(player.responseTime);
                int w = font.getStringWidth(pingText);
                if (w > maxPingTextWidth) maxPingTextWidth = w;
            }
            maxPingTextWidth += 2;
        }

        int maxSuffixWidth = 0;
        for (GuiPlayerInfo player : players) {
            String suffix = displayHandler.getSuffix(player.name);
            if (suffix != null) {
                int w = font.getStringWidth(suffix);
                if (w > maxSuffixWidth) maxSuffixWidth = w;
            }
        }
        if (maxSuffixWidth > 0) maxSuffixWidth += 5;

        int columns = 1;
        int rows = playerCount;
        while (rows > MAX_ROWS) {
            columns++;
            rows = (playerCount + columns - 1) / columns;
        }

        boolean showHeads = ServerUtilitiesConfig.tab.showPlayerHeads;
        int headSpace = showHeads ? HEAD_PADDING : 0;

        int pingBarSpace = showPingBars ? PING_ICON_WIDTH + 1 : 0;
        int entryWidth = headSpace + maxNameWidth
                + maxScoreWidth
                + maxSuffixWidth
                + maxPingTextWidth
                + pingBarSpace
                + 3;
        int totalGridWidth = Math.min(columns * entryWidth, screenWidth - 50);
        int columnWidth = totalGridWidth / columns;
        int gridLeft = (screenWidth - (columnWidth * columns + (columns - 1) * 5)) / 2;
        int startY = 10;

        String headerText = resolveHeaderFooter(true);
        String footerText = resolveHeaderFooter(false);

        List<String> headerLines = wrapText(font, headerText, screenWidth - 50);
        List<String> footerLines = wrapText(font, footerText, screenWidth - 50);

        int headerMaxWidth = getMaxLineWidth(font, headerLines);
        int footerMaxWidth = getMaxLineWidth(font, footerLines);
        int gridWidth = columnWidth * columns + (columns - 1) * 5;
        int bgWidth = Math.max(gridWidth, Math.max(headerMaxWidth, footerMaxWidth));

        int centerX = screenWidth / 2;

        if (!headerLines.isEmpty()) {
            int headerHeight = headerLines.size() * font.FONT_HEIGHT;
            Gui.drawRect(
                    centerX - bgWidth / 2 - 1,
                    startY - 1,
                    centerX + bgWidth / 2 + 1,
                    startY + headerHeight,
                    OVERLAY_BG_COLOR);
            for (String line : headerLines) {
                int lineWidth = font.getStringWidth(line);
                font.drawStringWithShadow(line, centerX - lineWidth / 2, startY, -1);
                startY += font.FONT_HEIGHT;
            }
            startY += 1;
        }

        Gui.drawRect(
                centerX - bgWidth / 2 - 1,
                startY - 1,
                centerX + bgWidth / 2 + 1,
                startY + rows * ENTRY_HEIGHT,
                OVERLAY_BG_COLOR);

        Set<String> activeNames = new HashSet<>();

        for (int i = 0; i < playerCount; i++) {
            int col = i / rows;
            int row = i % rows;
            int x = gridLeft + col * (columnWidth + 5);
            int y = startY + row * ENTRY_HEIGHT;

            GuiPlayerInfo player = players.get(i);
            activeNames.add(player.name);

            String displayName = resolveDisplayName(player, scoreboard, displayHandler);

            Gui.drawRect(x, y, x + columnWidth, y + 8, ENTRY_BG_COLOR);

            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);

            int textX = x;

            if (showHeads) {
                ResourceLocation skinLoc = TabSkinCache.INSTANCE.getOrLoadSkin(player.name);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getMinecraft().getTextureManager().bindTexture(skinLoc);
                Gui.func_152125_a(x, y, 8.0F, 8.0F, 8, 8, HEAD_SIZE, HEAD_SIZE, 64.0F, 32.0F);
                Gui.func_152125_a(x, y, 40.0F, 8.0F, 8, 8, HEAD_SIZE, HEAD_SIZE, 64.0F, 32.0F);
                textX += HEAD_PADDING;
            }

            font.drawStringWithShadow(displayName, textX, y, -1);

            if (objective != null) {
                int nameEndX = textX + font.getStringWidth(displayName) + 5;
                int scoreEndX = x + columnWidth - pingBarSpace - maxPingTextWidth - maxSuffixWidth - 2;
                if (scoreEndX - nameEndX > 5) {
                    Score score = objective.getScoreboard().func_96529_a(player.name, objective);
                    String scoreStr = EnumChatFormatting.YELLOW + "" + score.getScorePoints();
                    font.drawStringWithShadow(scoreStr, scoreEndX - font.getStringWidth(scoreStr), y, -1);
                }
            }

            String suffix = displayHandler.getSuffix(player.name);
            if (suffix != null) {
                int suffixRight = x + columnWidth - pingBarSpace - maxPingTextWidth - 2;
                font.drawStringWithShadow(suffix, suffixRight - font.getStringWidth(suffix), y, -1);
            }

            if (showPingNumber) {
                String pingText = formatPing(player.responseTime);
                int pingColor = getPingColor(player.responseTime);
                int pingTextX = x + columnWidth - pingBarSpace - font.getStringWidth(pingText) - 2;
                font.drawStringWithShadow(pingText, pingTextX, y, pingColor);
            }

            if (showPingBars) {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
                int pingLevel = getPingLevel(player.responseTime);
                Gui.func_146110_a(
                        x + columnWidth - PING_ICON_WIDTH - 1,
                        y,
                        0,
                        176 + pingLevel * 8,
                        PING_ICON_WIDTH,
                        PING_ICON_HEIGHT,
                        256,
                        256);
            }
        }

        if (!footerLines.isEmpty()) {
            int footerY = startY + rows * ENTRY_HEIGHT + 1;
            int footerHeight = footerLines.size() * font.FONT_HEIGHT;
            Gui.drawRect(
                    centerX - bgWidth / 2 - 1,
                    footerY - 1,
                    centerX + bgWidth / 2 + 1,
                    footerY + footerHeight,
                    OVERLAY_BG_COLOR);
            for (String line : footerLines) {
                int lineWidth = font.getStringWidth(line);
                font.drawStringWithShadow(line, centerX - lineWidth / 2, footerY, -1);
                footerY += font.FONT_HEIGHT;
            }
        }

        TabSkinCache.INSTANCE.cleanup(activeNames);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static String resolveDisplayName(GuiPlayerInfo player, Scoreboard scoreboard,
            TabDisplayHandler displayHandler) {
        String proxyName = displayHandler.getDisplayName(player.name);
        if (proxyName != null) return proxyName;
        if (player instanceof GuiPlayerInfoWrapper wrapper && wrapper.displayName != null) return wrapper.displayName;
        ScorePlayerTeam team = scoreboard.getPlayersTeam(player.name);
        return ScorePlayerTeam.formatPlayerName(team, player.name);
    }

    private static String resolveHeaderFooter(boolean isHeader) {
        String text;
        TabChannelHandler ch = TabChannelHandler.INSTANCE;
        if (ch.hasServerData()) {
            text = isHeader ? ch.getHeader() : ch.getFooter();
        } else {
            text = isHeader ? ServerUtilitiesConfig.tab.headerText : ServerUtilitiesConfig.tab.footerText;
        }
        if (text == null || text.isEmpty()) return "";
        return StringUtils.addFormatting(text.replace("\\n", "\n"));
    }

    private static List<String> wrapText(FontRenderer font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        for (String segment : text.split("\n", -1)) {
            if (segment.isEmpty()) {
                lines.add("");
                continue;
            }
            @SuppressWarnings("unchecked")
            List<String> wrapped = font.listFormattedStringToWidth(segment, maxWidth);
            lines.addAll(wrapped);
        }
        return lines;
    }

    private static int getMaxLineWidth(FontRenderer font, List<String> lines) {
        int max = 0;
        for (String line : lines) {
            int w = font.getStringWidth(line);
            if (w > max) max = w;
        }
        return max;
    }

    private static int getPingLevel(int ping) {
        if (ping < 0) return 5;
        if (ping < 150) return 0;
        if (ping < 300) return 1;
        if (ping < 600) return 2;
        if (ping < 1000) return 3;
        return 4;
    }

    private static String formatPing(int ping) {
        return ping < 0 ? "?" : ping + "ms";
    }

    private static int getPingColor(int ping) {
        if (ping < 0) return 0xAAAAAA;
        if (ping < 150) return 0x55FF55;
        if (ping < 300) return 0xAAFF55;
        if (ping < 600) return 0xFFFF55;
        if (ping < 1000) return 0xFF5555;
        return 0xAA0000;
    }
}
