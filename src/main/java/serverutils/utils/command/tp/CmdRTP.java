package serverutils.utils.command.tp;

import net.minecraft.block.material.Material;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.math.ChunkDimPos;
import serverutils.lib.lib.math.TeleporterDimPos;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.utils.data.ClaimedChunks;
import serverutils.utils.data.ServerUtilitiesPlayerData;

public class CmdRTP extends CmdBase {

    public CmdRTP() {
        super("rtp", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(player));
        data.checkTeleportCooldown(sender, ServerUtilitiesPlayerData.Timer.RTP);
        ServerUtilitiesPlayerData.Timer.RTP.teleport(
                player,
                playerMP -> findBlockPos(
                        playerMP.mcServer.worldServerForDimension(ServerUtilitiesConfig.world.spawn_dimension),
                        player,
                        0),
                null);
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
        if (biome.biomeName.contains("ocean")) {
            return findBlockPos(world, player, depth);
        }

        Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);

        while (y > 0) {
            y--;

            if (chunk.getBlock(x, y, z).getMaterial() != Material.air) {
                return TeleporterDimPos.of(x + 0.5D, y + 2.5D, z + 0.5D, world.provider.dimensionId);
            }
        }

        return findBlockPos(world, player, depth);
    }

    private boolean isInsideWorldBorder(World world, double x, double y, double z) {
        return x > -30000000 && x < 30000000 && z > -30000000 && z < 30000000 && y > -30000000 && y < 30000000;
    }

}
