package serverutils.command.tp;

import static serverutils.ServerUtilitiesConfig.world;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdSpawn extends CmdBase {

    public CmdSpawn() {
        super("spawn", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (player.dimension != world.spawn_dimension
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.SPAWN_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.cross_dim");
        }
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, TeleportType.SPAWN);
        data.teleport(getSpawnTeleporter(), TeleportType.SPAWN, null);
    }

    private TeleporterDimPos getSpawnTeleporter() {
        World w = DimensionManager.getWorld(world.spawn_dimension);
        ChunkCoordinates spawnpoint = w.getSpawnPoint();

        while (w.getBlock(spawnpoint.posX, spawnpoint.posY, spawnpoint.posZ).isNormalCube()) {
            spawnpoint.posY += 2;
        }

        return new BlockDimPos(spawnpoint, world.spawn_dimension).teleporter();
    }
}
