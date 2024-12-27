package serverutils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilitiesConfig;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.TeleporterDimPos;

public class CmdSpawn extends CmdBase {

    public CmdSpawn() {
        super("spawn", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, TeleportType.SPAWN);
        data.teleport(getSpawnTeleporter(), TeleportType.SPAWN, null);
    }

    private TeleporterDimPos getSpawnTeleporter() {
        World w = DimensionManager.getWorld(ServerUtilitiesConfig.world.spawn_dimension);
        ChunkCoordinates spawnpoint = w.getSpawnPoint();

        while (w.getBlock(spawnpoint.posX, spawnpoint.posY, spawnpoint.posZ).isNormalCube()) {
            spawnpoint.posY += 2;
        }

        return new BlockDimPos(spawnpoint, ServerUtilitiesConfig.world.spawn_dimension).teleporter();
    }
}
