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

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;

public class CmdSeekBlock extends CmdBase {

    private static final int MAX_RESULTS = 100;

    public CmdSeekBlock() {
        super("seek_block", Level.OP);
    }

    private final String COMMAND_USAGE = "/seek_block <blockID>:<meta | *> [maxResults]";

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return COMMAND_USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        if (args.length == 0 || args.length > 2) {
            String message = "Usage: " + COMMAND_USAGE;
            sender.addChatMessage(new ChatComponentText(message));
            ServerUtilities.LOGGER.info(message);
            return;
        }

        int override_max_results = MAX_RESULTS;
        if (args.length == 2) {
            try {
                override_max_results = Math.min(MAX_RESULTS, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                String message = "Invalid max results value. Must be a number.";
                sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
                ServerUtilities.LOGGER.info(message);
                return;
            }
        }

        try {
            String[] parts = args[0].split(":");

            if (parts.length != 2) {
                // Automatically set meta to 0 if it's not provided
                if (args[0].contains(":")) {
                    String message = "Invalid format. Use " + COMMAND_USAGE;
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
                    ServerUtilities.LOGGER.info(message);
                    return;
                }

                parts = new String[] { args[0], "0" }; // Default to meta 0
            }

            int targetBlockID = Integer.parseInt(parts[0]);
            int targetMeta = -1;
            boolean metaWildCard = false;

            if (parts[1].equals("*")) {
                metaWildCard = true;
            } else {
                targetMeta = Integer.parseInt(parts[1]);
            }

            String message;
            if (metaWildCard) {
                message = "--- Searching for Block ID " + targetBlockID + " with any metadata ---";
            } else {
                message = "--- Searching for Block ID " + targetBlockID + ":" + targetMeta + " ---";
            }
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            ServerUtilities.LOGGER.info(message);

            int foundCount = 0;
            for (int dimId : DimensionManager.getIDs()) {
                World world = DimensionManager.getWorld(dimId);
                if (world == null) continue;

                ChunkProviderServer chunkProvider = (ChunkProviderServer) world.getChunkProvider();
                if (!chunkProvider.loadedChunks.isEmpty() || sender instanceof MinecraftServer) {
                    message = "Dimension " + dimId + ":";
                    sender.addChatMessage(new ChatComponentText(EnumChatFormatting.BLUE + message));
                    ServerUtilities.LOGGER.info(message);
                }

                final ScanContext context = new ScanContext(
                        world,
                        sender,
                        targetBlockID,
                        targetMeta,
                        dimId,
                        foundCount,
                        override_max_results,
                        metaWildCard);

                for (Chunk chunkObj : chunkProvider.loadedChunks) {
                    foundCount += scanChunk(chunkObj, context);

                    if (foundCount >= override_max_results) {
                        message = "Search limit reached (" + override_max_results + " results).";
                        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
                        ServerUtilities.LOGGER.info(message);
                        return;
                    }
                }
            }

            message = "Search complete! Found " + foundCount + " matches.";
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + message));
            ServerUtilities.LOGGER.info(message);

        } catch (NumberFormatException e) {
            String message = "Invalid number format.";
            sender.addChatMessage(new ChatComponentText(message));
            ServerUtilities.LOGGER.info(message);
        }
    }

    private int scanChunk(Chunk chunk, ScanContext context) {
        int chunkX = chunk.xPosition * 16;
        int chunkZ = chunk.zPosition * 16;
        int count = 0;

        for (int x = chunkX; x < chunkX + 16; x++) {
            for (int z = chunkZ; z < chunkZ + 16; z++) {
                for (int y = 0; y < context.world.getHeight(); y++) {
                    Block block = chunk.getBlock(x & 15, y, z & 15);
                    int foundBlockID = Block.getIdFromBlock(block);
                    int foundMeta = context.world.getBlockMetadata(x, y, z);

                    if (foundBlockID == context.targetBlockID
                            && (context.metaWildCard || foundMeta == context.targetMeta)) {
                        String message = "Found " + foundBlockID
                                + ":"
                                + foundMeta
                                + " in "
                                + DimensionManager.getProvider(context.dimId).getDimensionName()
                                + " (Dim ID: "
                                + context.dimId
                                + ") at "
                                + "("
                                + x
                                + ", "
                                + y
                                + ", "
                                + z
                                + ")";
                        ServerUtilities.LOGGER.info(message);
                        context.sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + message));
                        count++;

                        if (context.foundCount + count >= context.overrideMaxResults) {
                            return count;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static class ScanContext {

        World world;
        ICommandSender sender;
        int targetBlockID;
        int targetMeta;
        int dimId;
        int foundCount;
        int overrideMaxResults;
        boolean metaWildCard;

        ScanContext(World world, ICommandSender sender, int targetBlockID, int targetMeta, int dimId, int foundCount,
                int overrideMaxResults, boolean metaWildCard) {
            this.world = world;
            this.sender = sender;
            this.targetBlockID = targetBlockID;
            this.targetMeta = targetMeta;
            this.dimId = dimId;
            this.foundCount = foundCount;
            this.overrideMaxResults = overrideMaxResults;
            this.metaWildCard = metaWildCard;
        }
    }
}
