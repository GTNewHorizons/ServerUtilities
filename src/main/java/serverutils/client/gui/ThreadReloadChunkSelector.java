package serverutils.client.gui;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import serverutils.lib.client.GlStateManager;
import serverutils.lib.client.PixelBuffer;
import serverutils.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.icon.Color4I;
import serverutils.lib.math.MathUtils;

public class ThreadReloadChunkSelector extends Thread {

    private static ByteBuffer pixelBuffer = null;
    private static final int PIXEL_SIZE = ChunkSelectorMap.TILES_TEX * 16;
    private static final PixelBuffer PIXELS = new PixelBuffer(PIXEL_SIZE, PIXEL_SIZE);
    private static final Map<Pair<Block, Integer>, Color4I> COLOR_CACHE = new HashMap<>();
    private static final ChunkCoordinates CURRENT_BLOCK_POS = new ChunkCoordinates(0, 0, 0);
    private static World world = null;
    private static final Function<Pair<Block, Integer>, Color4I> COLOR_GETTER = state1 -> Color4I
            .rgb(getBlockColor0(state1));
    private static ThreadReloadChunkSelector instance;
    private static int textureID = -1;
    private static final int[] HEIGHT_MAP = new int[PIXEL_SIZE * PIXEL_SIZE];

    static int getTextureId() {
        if (textureID == -1) {
            textureID = TextureUtil.glGenTextures();
        }
        return textureID;
    }

    static void updateTexture() {
        if (pixelBuffer != null) {
            // boolean hasBlur = false;
            // int filter = hasBlur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
            GlStateManager.bindTexture(getTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D,
                    0,
                    GL11.GL_RGBA8,
                    ChunkSelectorMap.TILES_TEX * 16,
                    ChunkSelectorMap.TILES_TEX * 16,
                    0,
                    GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE,
                    pixelBuffer);
            pixelBuffer = null;
        }
    }

    static void reloadArea(World w, int sx, int sz) {
        if (instance != null) {
            instance.cancelled = true;
            instance = null;
        }

        instance = new ThreadReloadChunkSelector(w, sx, sz);
        instance.cancelled = false;
        instance.start();
        COLOR_CACHE.clear();
    }

    private final int startX, startZ;
    private boolean cancelled = false;

    private ThreadReloadChunkSelector(World w, int sx, int sz) {
        super("ChunkSelectorAreaReloader");
        setDaemon(true);
        world = w;
        startX = sx;
        startZ = sz;
    }

    private static int getBlockColor0(Pair<Block, Integer> state) {
        Block b = state.getLeft();

        if (b == Blocks.sandstone || b == Blocks.end_stone) {
            return MapColor.sandColor.colorValue;
        } else if (b == Blocks.fire) {
            return MapColor.redColor.colorValue;
        } else if (b == Blocks.yellow_flower) {
            return MapColor.yellowColor.colorValue;
        } else if (b == Blocks.lava) {
            return MapColor.adobeColor.colorValue;
        } else if (b == Blocks.obsidian) {
            return 0x150047;
        } else if (b == Blocks.gravel) {
            return 0x8D979B;
        } else if (b == Blocks.grass) {
            return 0x549954;
        } else if (b == Blocks.torch) {
            return 0xFFA530;
        } else if (b == Blocks.netherrack || b == Blocks.quartz_ore) {
            return 0x7F1321;
        } else if (b == Blocks.red_flower) {
            switch (state.getRight()) { // "poppy", "blueOrchid", "allium", "houstonia", "tulipRed", "tulipOrange",
                                        // "tulipWhite", "tulipPink", "oxeyeDaisy"
                case 0: // poppy
                    return MapColor.redColor.colorValue;
                case 1: // blue orchid
                    return MapColor.lightBlueColor.colorValue;
                case 2: // allium
                    return MapColor.magentaColor.colorValue;
                case 3: // houstonia
                    return MapColor.silverColor.colorValue;
                case 4: // tulipRed
                    return MapColor.redColor.colorValue;
                case 5: // tulipOrange
                    return MapColor.adobeColor.colorValue;
                case 6: // tulipWhite
                    return MapColor.snowColor.colorValue;
                case 7: // tulipPink
                    return MapColor.pinkColor.colorValue;
                case 8: // oxeyeDaisy
                    return MapColor.silverColor.colorValue;
            }
        } else if (b == Blocks.planks) {
            switch (state.getRight()) {
                case 0: // oak
                    return 0xC69849;
                case 1: // Spruce
                    return 0x7C5E2E;
                case 2: // Birch
                    return 0xF2E093;
                case 3: // Jungle
                    return 0xC67653;
                case 4: // Acacia
                    return 0xE07F3E;
                case 5: // Dark Oak
                    return 0x512D14;
            }
        }

        return state.getLeft().getMapColor(state.getRight()).colorValue;
    }

    private static int getHeight(int x, int z) {
        int index = x + z * PIXEL_SIZE;
        return index < 0 || index >= HEIGHT_MAP.length ? -1 : HEIGHT_MAP[index];
    }

    @Override
    public void run() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        Arrays.fill(PIXELS.getPixels(), Color4I.rgb(world.getSkyColor(player, 0)).rgba());
        Arrays.fill(HEIGHT_MAP, -1);
        pixelBuffer = PIXELS.toByteBuffer(false);

        Chunk chunk;
        int x, z, wi, wx, wz, by, topY;
        Color4I color;
        Pair<Block, Integer> state;

        int startY = (int) player.getPosition(1).yCoord;

        try {
            for (int index = 0; index < ChunkSelectorMap.TILES_GUI * ChunkSelectorMap.TILES_GUI + 1; index++) {
                World w = world;

                if (w == null) {
                    break;
                }

                ChunkCoordIntPair pos = MathUtils.getSpiralPoint(index);
                if (pos == null) continue;
                int cx = pos.chunkXPos + ChunkSelectorMap.TILES_GUI2;
                int cz = pos.chunkZPos + ChunkSelectorMap.TILES_GUI2;

                chunk = w.getChunkProvider().provideChunk(startX + cx, startZ + cz);

                if (chunk != null) {
                    x = (startX + cx) << 4;
                    z = (startZ + cz) << 4;
                    topY = (w.provider.dimensionId == -1) ? startY + 5
                            : Math.max(w.getActualHeight(), chunk.getTopFilledSegment() + 15);

                    for (wi = 0; wi < 256; wi++) {
                        wx = wi % 16;
                        wz = wi / 16;

                        for (by = topY; by > 0; --by) {
                            if (cancelled) {
                                return;
                            }

                            CURRENT_BLOCK_POS.set(x + wx, by, z + wz);
                            state = Pair.of(chunk.getBlock(wx, by, wz), chunk.getBlockMetadata(wx, by, wz));

                            if (state.getLeft() != Blocks.tallgrass && !state.getLeft()
                                    .isAir(w, CURRENT_BLOCK_POS.posX, CURRENT_BLOCK_POS.posY, CURRENT_BLOCK_POS.posZ)) {
                                HEIGHT_MAP[(cx * 16 + wx) + (cz * 16 + wz) * PIXEL_SIZE] = by;
                                break;
                            }
                        }
                    }
                }
            }

            for (int index = 0; index < ChunkSelectorMap.TILES_GUI * ChunkSelectorMap.TILES_GUI + 1; index++) {
                World w = world;

                if (w == null) {
                    break;
                }

                ChunkCoordIntPair pos = MathUtils.getSpiralPoint(index);
                if (pos == null) continue;
                int cx = pos.chunkXPos + ChunkSelectorMap.TILES_GUI2;
                int cz = pos.chunkZPos + ChunkSelectorMap.TILES_GUI2;

                chunk = w.getChunkProvider().provideChunk(startX + cx, startZ + cz);

                if (chunk == null) {
                    continue;
                }

                x = (startX + cx) << 4;
                z = (startZ + cz) << 4;

                for (wi = 0; wi < 256; wi++) {
                    wx = wi % 16;
                    wz = wi / 16;
                    by = getHeight(cx * 16 + wx, cz * 16 + wz);

                    if (by < 0) {
                        continue;
                    }

                    CURRENT_BLOCK_POS.set(x + wx, by, z + wz);
                    state = Pair.of(chunk.getBlock(wx, by, wz), chunk.getBlockMetadata(wx, by, wz));

                    color = COLOR_CACHE.computeIfAbsent(state, COLOR_GETTER)
                            .addBrightness(MathUtils.RAND.nextFloat() * 0.04F);

                    int bn = getHeight(cx * 16 + wx, cz * 16 + wz - 1);
                    int bw = getHeight(cx * 16 + wx - 1, cz * 16 + wz);

                    if (by > bn && bn != -1 || by > bw && bw != -1) {
                        color = color.addBrightness(0.1F);
                    }

                    if (by < bn && bn != -1 || by < bw && bw != -1) {
                        color = color.addBrightness(-0.1F);
                    }

                    PIXELS.setRGB(cx * 16 + wx, cz * 16 + wz, color.rgba());
                }

                pixelBuffer = PIXELS.toByteBuffer(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        pixelBuffer = PIXELS.toByteBuffer(false);
        world = null;
        instance = null;
    }
}
