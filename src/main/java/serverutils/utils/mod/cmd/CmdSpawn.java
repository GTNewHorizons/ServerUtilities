package serverutils.utils.mod.cmd;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.*;
import net.minecraft.world.World;

import serverutils.lib.*;
import serverutils.lib.api.cmd.*;
import serverutils.utils.mod.ServerUtilities;

public class CmdSpawn extends CommandLM {

    public CmdSpawn() {
        super("spawn", CommandLevel.ALL);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        World w = LMDimUtils.getWorld(0);
        ChunkCoordinates spawnpoint = w.getSpawnPoint();

        while (w.getBlock(spawnpoint.posX, spawnpoint.posY, spawnpoint.posZ).isOpaqueCube()) spawnpoint.posY += 2;

        LMDimUtils.teleportPlayer(ep, new BlockDimPos(spawnpoint, 0));
        return ServerUtilities.mod.chatComponent("cmd.spawn_tp");
    }
}
