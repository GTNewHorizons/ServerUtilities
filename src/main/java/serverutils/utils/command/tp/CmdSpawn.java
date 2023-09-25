package serverutils.utils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.math.BlockDimPos;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.data.ServerUtilitiesPlayerData;

public class CmdSpawn extends CmdBase {

    public CmdSpawn() {
        super("spawn", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, ServerUtilitiesPlayerData.Timer.SPAWN);
        ServerUtilitiesPlayerData.Timer.SPAWN.teleport(player, playerMP -> {
            World w = playerMP.mcServer.worldServerForDimension(ServerUtilitiesConfig.world.spawn_dimension);
            ChunkCoordinates spawnpoint = w.getSpawnPoint();

            while (w.getBlock(spawnpoint.posX, spawnpoint.posY, spawnpoint.posZ).isNormalCube()) {
                spawnpoint.posY += 2;
            }

            return new BlockDimPos(spawnpoint, ServerUtilitiesConfig.world.spawn_dimension).teleporter();
        }, null);
    }
}
