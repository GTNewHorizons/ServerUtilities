package serverutils.command;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;

import cpw.mods.fml.common.FMLLog;
import serverutils.lib.command.CmdBase;

public class CmdSeekBlock extends CmdBase {

    private static final int MAX_RESULTS = 100;

    public CmdSeekBlock() {
        super("seek_block", Level.OP);
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/seek_block <blockID>:<meta>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        if (args.length != 1) {
            String message = "Usage: /seekblock <blockID>:<meta>";
            sender.addChatMessage(new ChatComponentText(message));
            FMLLog.info(message);
            return;
        }

        String[] parts = args[0].split(":");
        if (parts.length != 2) {
            String message = "Invalid format. Use /seekblock <blockID>:<meta>";
            sender.addChatMessage(new ChatComponentText(message));
            FMLLog.info(message);
            return;
        }

        try {
            int targetBlockID = Integer.parseInt(parts[0]);
            int targetMeta = Integer.parseInt(parts[1]);

            String message = "--- Searching for Block ID " + targetBlockID + ":" + targetMeta + " ---";
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            FMLLog.info(message);

            int foundCount = 0;

            for (int dimId : DimensionManager.getIDs()) {
                World world = DimensionManager.getWorld(dimId);
                if (world == null) continue;

                ChunkProviderServer chunkProvider = (ChunkProviderServer) world.getChunkProvider();
                if (!chunkProvider.loadedChunks.isEmpty() || sender instanceof MinecraftServer) {
                    message = "Dimension " + dimId + ":";
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BLUE + message));
                    FMLLog.info(message);
                }

                for (Chunk chunkObj : chunkProvider.loadedChunks) {
                    foundCount += scanChunk(chunkObj, world, sender, targetBlockID, targetMeta, dimId, foundCount);

                    if (foundCount >= MAX_RESULTS) {
                        message = "Search limit reached (" + MAX_RESULTS + " results).";
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
                        FMLLog.info(message);
                        return;
                    }
                }
            }

            message = "Search complete! Found " + foundCount + " matches.";
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            FMLLog.info(message);
        } catch (NumberFormatException e) {
            String message = "Invalid number format.";
            sender.addChatMessage(new ChatComponentText(message));
            FMLLog.info(message);
        }
    }

    private int scanChunk(Chunk chunk, World world, ICommandSender sender, int blockID, int meta, int dimension,
            int foundCount) {
        int chunkX = chunk.xPosition * 16;
        int chunkZ = chunk.zPosition * 16;
        int count = 0;

        for (int x = chunkX; x < chunkX + 16; x++) {
            for (int z = chunkZ; z < chunkZ + 16; z++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    Block block = chunk.getBlock(x & 15, y, z & 15);
                    int foundBlockID = Block.getIdFromBlock(block);
                    int foundMeta = world.getBlockMetadata(x, y, z);

                    if (foundBlockID == blockID && foundMeta == meta) {
                        String message = "Found at Dim " + dimension + " (" + x + ", " + y + ", " + z + ")";
                        FMLLog.info(message);
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + message));
                        count++;

                        if (foundCount + count >= MAX_RESULTS) {
                            return count;
                        }
                    }
                }
            }
        }
        return count;
    }
}
