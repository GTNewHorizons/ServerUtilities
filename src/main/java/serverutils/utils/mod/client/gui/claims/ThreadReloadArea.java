package serverutils.utils.mod.client.gui.claims;

import java.nio.ByteBuffer;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.LMColorUtils;
import latmod.lib.MathHelperLM;
import latmod.lib.PixelBuffer;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;

@SideOnly(Side.CLIENT)
public class ThreadReloadArea extends Thread {

    private static final short defHeight = -1;
    public static final PixelBuffer pixels = new PixelBuffer(
            GuiClaimChunks.tiles_tex * 16,
            GuiClaimChunks.tiles_tex * 16);
    public static final short[] heightMap = new short[pixels.pixels.length];

    public final World worldObj;
    public final GuiClaimChunks gui;

    public Chunk chunkMC;
    public short maxHeight = 0;
    public boolean isNether;

    public ThreadReloadArea(World w, GuiClaimChunks m) {
        super("LM_MapReloader");
        setDaemon(true);
        worldObj = w;
        gui = m;
        Arrays.fill(heightMap, defHeight);
        Arrays.fill(pixels.pixels, 0);
        isNether = worldObj.provider.dimensionId == -1;
    }

    public void run() {
        try {
            for (int cz = 0; cz < GuiClaimChunks.tiles_gui; cz++)
                for (int cx = 0; cx < GuiClaimChunks.tiles_gui; cx++) {
                    if (worldObj.getChunkProvider().chunkExists(gui.startX, gui.startY)) {
                        chunkMC = worldObj.getChunkFromChunkCoords(gui.startX, gui.startY);
                        maxHeight = (short) Math.max(255, chunkMC.getTopFilledSegment() + 15);

                        int x = (gui.startX + cx) * 16;
                        int y = (gui.startY + cz) * 16;

                        for (int i = 0; i < 256; i++) {
                            int bx = x + (i % 16);
                            int by = y + (i / 16);
                            int col = getBlockColor(bx, by);
                            pixels.setRGB(cx * 16 + (i % 16), cz * 16 + (i / 16), col);
                        }
                    }
                }

            ByteBuffer buffer = ServerUtilitiesLibraryClient.toByteBuffer(pixels.pixels, false);
            GuiClaimChunks.pixelBuffer = buffer;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getBlockColor(int bx, int bz) {
        short by = getTopY(bx, bz);
        if (by == defHeight || by > 255) return 0;

        Block block = worldObj.getBlock(bx, by, bz);

        if (!block.isAir(worldObj, bx, by, bz)) {
            int col = 0xFF000000 | getBlockColor(block, bx, by, bz);

            short bw = getTopY(bx - 1, bz);
            short be = getTopY(bx + 1, bz);
            short bn = getTopY(bx, bz - 1);
            short bs = getTopY(bx, bz + 1);

            if ((bw != defHeight && bw < by) || (bn != defHeight && bn < by))
                return LMColorUtils.addBrightness(col, 25);
            else if ((be != defHeight && be < by) || (bs != defHeight && bs < by))
                return LMColorUtils.addBrightness(col, -25);

            return col;
        }

        return 0;
    }

    private short getTopY(int bx, int bz) {
        int x = MathHelperLM.wrap(bx, 16);
        int z = MathHelperLM.wrap(bz, 16);

        Chunk c = chunkMC;
        short max = maxHeight;
        boolean mapValue = false;

        int cx = MathHelperLM.chunk(bx);
        int cz = MathHelperLM.chunk(bz);

        if (cx == gui.startX && cz == gui.startY) {
            mapValue = true;
            if (heightMap[x + z * 16] != defHeight) return heightMap[x + z * 16];
        } else {
            c = worldObj.getChunkFromChunkCoords(MathHelperLM.chunk(bx), MathHelperLM.chunk(bz));
            max = (short) Math.max(255, c.getTopFilledSegment() + 15);
        }

        for (short y = max; y > 0; --y) {
            Block block = c.getBlock(x, y, z);
            if (isNether && (block == Blocks.bedrock || block == Blocks.netherrack)) continue;
            // isNether = false;
            if (block == Blocks.tallgrass || block.isAir(worldObj, bx, y, bz)) continue;
            if (mapValue) heightMap[x + z * 16] = y;
            return y;
        }

        return defHeight;
    }

    private int getBlockColor(Block b, int x, int y, int z) {
        if (b == Blocks.sandstone) return MapColor.sandColor.colorValue;
        else if (b == Blocks.fire) return MapColor.redColor.colorValue;
        else if (b == Blocks.yellow_flower) return MapColor.yellowColor.colorValue;
        else if (b == Blocks.lava) return MapColor.adobeColor.colorValue;
        else if (b == Blocks.end_stone) return MapColor.sandColor.colorValue;
        else if (b == Blocks.obsidian) return 0xFF150047;
        else if (b == Blocks.gravel) return 0xFF8D979B;
        else if (b == Blocks.glass) return 0x33BCF9FF;
        else if (b == Blocks.netherrack) return 0xFFA72A40;
        else if (b == Blocks.bedrock) return 0xFF606060;
        else if (b == Blocks.soul_sand) return 0xFFA0403A;
        else if (b == Blocks.glowstone) return 0xFFFFD800;
        else if (b.getMaterial() == Material.water)
            return LMColorUtils.multiply(MapColor.waterColor.colorValue, b.colorMultiplier(worldObj, x, y, z), 200);

        if (b == Blocks.red_flower) {
            int m = worldObj.getBlockMetadata(x, y, z);

            if (m == 0) return MapColor.redColor.colorValue;
            else if (m == 1) return MapColor.lightBlueColor.colorValue;
            else if (m == 2) return MapColor.magentaColor.colorValue;
            else if (m == 3) return MapColor.silverColor.colorValue;
            else if (m == 4) return MapColor.redColor.colorValue;
            else if (m == 5) return MapColor.adobeColor.colorValue;
            else if (m == 6) return MapColor.snowColor.colorValue;
            else if (m == 7) return MapColor.pinkColor.colorValue;
            else if (m == 8) return MapColor.silverColor.colorValue;
        } else if (b == Blocks.planks) {
            int m = worldObj.getBlockMetadata(x, y, z);

            if (m == 0) return 0xFFC69849;
            else if (m == 1) return 0xFF7C5E2E;
            else if (m == 2) return 0xFFF2E093;
            else if (m == 3) return 0xFFC67653;
            else if (m == 4) return 0xFFE07F3E;
            else if (m == 5) return 0xFF512D14;
        }

        if (b == Blocks.leaves || b == Blocks.vine || b == Blocks.waterlily)
            return LMColorUtils.addBrightness(b.colorMultiplier(worldObj, x, y, z), -40);
        else if (b == Blocks.grass && worldObj.getBlockMetadata(x, y, z) == 0)
            return LMColorUtils.addBrightness(b.colorMultiplier(worldObj, x, y, z), -15);

        return b.getMapColor(worldObj.getBlockMetadata(x, y, z)).colorValue;
    }
}
