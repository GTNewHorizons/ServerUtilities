package serverutils.command;

import java.util.HashSet;
import java.util.Map;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import serverutils.lib.command.CmdBase;
import serverutils.lib.util.NBTUtils;

public class CmdDumpChunkloaders extends CmdBase {

    public CmdDumpChunkloaders() {
        super("dump_chunkloaders", Level.OP_OR_SP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        for (World world : DimensionManager.getWorlds()) {
            HashSet<ForgeChunkManager.Ticket> set = new HashSet<>();

            for (Map.Entry<ChunkCoordIntPair, ForgeChunkManager.Ticket> entry : ForgeChunkManager
                    .getPersistentChunksFor(world).entries()) {
                set.add(entry.getValue());
            }

            if (!set.isEmpty()) {
                sender.addChatMessage(new ChatComponentText("- DIM " + world.provider.dimensionId + ":"));

                for (ForgeChunkManager.Ticket ticket : set) {
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

                    IChatComponent chunks = new ChatComponentText("Chunks");

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
                            new ChatComponentText("").appendSibling(title).appendText(" | ").appendSibling(owner)
                                    .appendText(" | ").appendSibling(data).appendText(" | ").appendSibling(chunks));
                }
            }
        }
    }
}
