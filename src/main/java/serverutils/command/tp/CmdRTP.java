package serverutils.command.tp;

import static serverutils.ServerUtilitiesConfig.world;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdRTP extends CmdBase {

    public CmdRTP() {
        super("rtp", Level.ALL);
    }

    private static final List<Block> UNSAFE_BLOCKS = Arrays
            .asList(Blocks.cactus, Blocks.fire, Blocks.lava, Blocks.water, Blocks.flowing_lava, Blocks.flowing_water);

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        if (player.dimension != world.spawn_dimension
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.RTP_CROSS_DIM)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.cross_dim");
        }
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, TeleportType.RTP);
        TeleporterDimPos pos = findBlockPos(
                player.mcServer.worldServerForDimension(ServerUtilitiesConfig.world.spawn_dimension),
                player,
                0);
        data.teleport(pos, TeleportType.RTP, null);
    }

    private TeleporterDimPos findBlockPos(World world, EntityPlayerMP player, int depth) {
        if (++depth > ServerUtilitiesConfig.world.rtp_max_tries) {
            player.addChatMessage(ServerUtilities.lang(player, "serverutilities.lang.rtp.fail"));
            return TeleporterDimPos.of(player);
        }

        double dist = ServerUtilitiesConfig.world.rtp_min_distance + world.rand.nextDouble()
                * (ServerUtilitiesConfig.world.rtp_max_distance - ServerUtilitiesConfig.world.rtp_min_distance);
        double angle = world.rand.nextDouble() * Math.PI * 2D;

        int x = MathHelper.floor_double(Math.cos(angle) * dist);
        int y = 256;
        int z = MathHelper.floor_double(Math.sin(angle) * dist);

        if (!isInsideWorldBorder(world, x, y, z)) {
            return findBlockPos(world, player, depth);
        }

        if (ClaimedChunks.instance != null
                && ClaimedChunks.instance.getChunk(new ChunkDimPos(x >> 4, z >> 4, world.provider.dimensionId))
                        != null) {
            return findBlockPos(world, player, depth);
        }

        // TODO: Find a better way to check for biome without loading the chunk
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        if (biome.biomeName.contains("Ocean")) {
            return findBlockPos(world, player, depth);
        }

        while (y > 0) {
            y--;

            Block blockFeet = world.getBlock(x, y, z);
            Block blockHead = world.getBlock(x, y + 2, z);
            if (!blockFeet.equals(Blocks.air)) {
                if (blockHead.equals(Blocks.air) && !UNSAFE_BLOCKS.contains(blockFeet)) {
                    return TeleporterDimPos.of(x + 0.5D, y + 2.5D, z + 0.5D, world.provider.dimensionId);
                }
            }
        }

        return findBlockPos(world, player, depth);
    }

    private boolean isInsideWorldBorder(World world, double x, double y, double z) {
        return x > -30000000 && x < 30000000 && z > -30000000 && z < 30000000 && y > -30000000 && y < 30000000;
    }

}
