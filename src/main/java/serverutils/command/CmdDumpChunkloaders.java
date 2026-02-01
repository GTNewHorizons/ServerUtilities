package serverutils.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.util.NBTUtils;

public class CmdDumpChunkloaders extends CmdBase {

    public CmdDumpChunkloaders() {
        super("dump_chunkloaders", Level.OP_OR_SP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, CommandUtils.getDimensionNames());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            processCommandDumpList(sender);
        } else {
            OptionalInt dimension = CommandUtils.parseDimension(sender, args, 0);

            if (dimension.isPresent()) {
                int worldId = dimension.getAsInt();
                World world = DimensionManager.getWorld(worldId);
                if (world == null) {
                    throw ServerUtilities.error(sender, "commands.dump_chunkloaders.unknown_dimension", worldId);
                }

                processCommandDumpRegions(sender, Arrays.asList(world));
            } else {
                processCommandDumpRegions(sender, Arrays.asList(DimensionManager.getWorlds()));
            }
        }
    }

    private void processCommandDumpList(ICommandSender sender) throws CommandException {
        for (World world : DimensionManager.getWorlds()) {
            HashSet<ForgeChunkManager.Ticket> set = new HashSet<>();

            for (Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> entry : ForgeChunkManager
                    .getPersistentChunksFor(world).entries()) {
                set.add(entry.getValue());
            }

            if (!set.isEmpty()) {
                sender.addChatMessage(buildWorldDescription(world));

                for (ForgeChunkManager.Ticket ticket : set) {
                    IChatComponent ticketDescription = buildTicketDescription(ticket);

                    IChatComponent chunks = new ChatComponentText("Chunks");

                    IChatComponent list = new ChatComponentText("Regions");
                    String listCommand = "/dump_chunkloaders " + world.provider.dimensionId;
                    list.getChatStyle().setChatHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(listCommand)));
                    list.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, listCommand));

                    int minX = Integer.MAX_VALUE;
                    int minZ = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE;
                    int maxZ = Integer.MIN_VALUE;

                    for (ChunkCoordIntPair pos : ticket.getChunkList()) {
                        if (pos.chunkXPos < minX) {
                            minX = pos.chunkXPos;
                        }

                        if (pos.chunkZPos < minZ) {
                            minZ = pos.chunkZPos;
                        }

                        if (pos.chunkXPos > maxX) {
                            maxX = pos.chunkXPos;
                        }

                        if (pos.chunkZPos > maxZ) {
                            maxZ = pos.chunkZPos;
                        }
                    }
                    int x = (minX + maxX) * 8 + 8;
                    int z = (minZ + maxZ) * 8 + 8;
                    world.getChunkFromBlockCoords(x, z);
                    int y = world.getTopSolidOrLiquidBlock(x, z);
                    chunks.getChatStyle().setChatHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    new ChatComponentText(
                                            "(" + x
                                                    + ','
                                                    + y
                                                    + ','
                                                    + z
                                                    + ")"
                                                    + " ; "
                                                    + ticket.getChunkList().toString())));
                    chunks.getChatStyle().setChatClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + x + " " + y + " " + z));

                    sender.addChatMessage(
                            ticketDescription.appendText(" | ").appendSibling(chunks).appendText(" | ")
                                    .appendSibling(list));
                }
            }
        }
    }

    private void processCommandDumpRegions(ICommandSender sender, Iterable<World> worlds) throws CommandException {
        for (World world : worlds) {
            HashSet<ForgeChunkManager.Ticket> set = new HashSet<>();

            for (Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> entry : ForgeChunkManager
                    .getPersistentChunksFor(world).entries()) {
                set.add(entry.getValue());
            }

            if (!set.isEmpty()) {
                sender.addChatMessage(buildWorldDescription(world));

                for (ForgeChunkManager.Ticket ticket : set) {
                    IChatComponent ticketDescription = buildTicketDescription(ticket);

                    sender.addChatMessage(ticketDescription.appendText(" | List of regions:"));

                    RegionGraph regionGraph = new RegionGraph();
                    for (ChunkCoordIntPair chunk : ticket.getChunkList()) {
                        regionGraph.addChunk(chunk);
                    }

                    for (Region region : regionGraph.getRegions()) {
                        List<ChunkCoordIntPair> chunks = region.getChunks();
                        int regionSize = chunks.size();

                        ChunkCoordIntPair headChunk = chunks.get(0);
                        IChatComponent regionDescription = new ChatComponentText(
                                String.format("  * %s (size: %d)", headChunk, regionSize));

                        int x = headChunk.chunkXPos * 16 + 8;
                        int z = headChunk.chunkZPos * 16 + 8;
                        world.getChunkFromBlockCoords(x, z);
                        int y = world.getTopSolidOrLiquidBlock(x, z);

                        IChatComponent teleport = new ChatComponentText("Teleport");
                        String teleportCommand = "/tp " + x + " " + y + " " + z;
                        teleport.getChatStyle().setChatHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(teleportCommand)));
                        teleport.getChatStyle()
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand));

                        sender.addChatMessage(regionDescription.appendText(" | ").appendSibling(teleport));
                    }
                }
            }
        }
    }

    @NotNull
    private IChatComponent buildWorldDescription(World world) {
        IChatComponent worldId = new ChatComponentText(String.valueOf(world.provider.dimensionId));
        worldId.getChatStyle().setColor(EnumChatFormatting.AQUA);

        return new ChatComponentText("- DIM ").appendSibling(worldId).appendText(":");
    }

    @NotNull
    private IChatComponent buildTicketDescription(ForgeChunkManager.Ticket ticket) {
        IChatComponent title = new ChatComponentText(String.format("#%08x", ticket.hashCode()));
        title.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(ticket.getChunkList().size() + " chunks")));

        IChatComponent owner = new ChatComponentText("Owner");
        owner.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(ticket.getModId() + " : " + ticket.getEntity())));

        IChatComponent data = new ChatComponentText("Data");
        data.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(NBTUtils.getColoredNBTString(ticket.getModData()))));

        return new ChatComponentText("").appendSibling(title).appendText(" | ").appendSibling(owner).appendText(" | ")
                .appendSibling(data);
    }

    private static class RegionGraph {

        private final Set<Region> regions = new LinkedHashSet<>();
        private final Map<ChunkCoordIntPair, Region> chunkToRegion = new HashMap<>();

        @NotNull
        @Unmodifiable
        public List<Region> getRegions() {
            List<Region> sortedRegions = new ArrayList<>(regions);
            sortedRegions.sort(Comparator.comparingInt(region -> -region.getChunks().size()));
            return Collections.unmodifiableList(sortedRegions);
        }

        public void addChunk(ChunkCoordIntPair chunk) {
            Region newRegion = new Region(chunk);
            regions.add(newRegion);
            chunkToRegion.put(chunk, newRegion);

            for (ChunkCoordIntPair neighbour : getNeighbours(chunk)) {
                Region region = chunkToRegion.get(neighbour);
                if (region != null) {
                    newRegion = merge(region, newRegion);
                }
            }
        }

        @NotNull
        private Region merge(Region region1, Region region2) {
            if (region1 == region2) {
                return region1;
            }

            if (region1.chunks.size() < region2.chunks.size()) {
                return merge(region2, region1);
            }

            for (ChunkCoordIntPair chunk : region2.chunks) {
                chunkToRegion.put(chunk, region1);
            }
            region1.chunks.addAll(region2.chunks);
            region2.chunks.clear();

            regions.remove(region2);

            return region1;
        }

        @NotNull
        @Unmodifiable
        private List<ChunkCoordIntPair> getNeighbours(ChunkCoordIntPair chunk) {
            return Collections.unmodifiableList(
                    Arrays.asList(
                            new ChunkCoordIntPair(chunk.chunkXPos - 1, chunk.chunkZPos),
                            new ChunkCoordIntPair(chunk.chunkXPos + 1, chunk.chunkZPos),
                            new ChunkCoordIntPair(chunk.chunkXPos, chunk.chunkZPos - 1),
                            new ChunkCoordIntPair(chunk.chunkXPos, chunk.chunkZPos + 1)));
        }
    }

    private static class Region {

        private final List<ChunkCoordIntPair> chunks = new LinkedList<>();

        public Region(ChunkCoordIntPair chunk) {
            chunks.add(chunk);
        }

        @NotNull
        @UnmodifiableView
        public List<ChunkCoordIntPair> getChunks() {
            return Collections.unmodifiableList(chunks);
        }
    }
}
