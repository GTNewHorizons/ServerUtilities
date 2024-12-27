package serverutils.command.chunks;

import static serverutils.ServerUtilitiesNotifications.CANT_MODIFY_CHUNK;
import static serverutils.ServerUtilitiesNotifications.CHUNK_MODIFIED;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesNotifications;
import serverutils.data.ClaimResult;
import serverutils.data.ClaimedChunks;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.math.ChunkDimPos;

public class CmdClaimAs extends CmdBase {

    public CmdClaimAs() {
        super("claim_as", Level.OP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();

            for (ForgeTeam team : Universe.get().getTeams()) {
                if (team.type.isServer) {
                    list.add(team.getId());
                }
            }

            return getListOfStringsFromIterableMatchingLastWord(args, list);
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (!ClaimedChunks.isActive()) {
            throw ServerUtilities.error(sender, "feature_disabled_server");
        }

        checkArgs(sender, args, 1);
        ForgeTeam team = CommandUtils.getTeam(sender, args[0]);

        if (!team.type.isServer) {
            throw ServerUtilities.error(sender, "commands.chunks.claim_as.team_not_server", args[0]);
        }

        int radius = 0;

        if (args.length >= 2) {
            radius = parseIntBounded(sender, args[1], 0, 30);
        }

        EntityPlayerMP player = getCommandSenderAsPlayer(sender);

        ForgePlayer p = new ForgePlayer(
                Universe.get(),
                UUID.nameUUIDFromBytes("FakePlayerClaimAs".getBytes(StandardCharsets.UTF_8)),
                "FakePlayerClaimAs");

        p.team = team;
        ChunkDimPos pos = new ChunkDimPos(player);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkDimPos pos1 = new ChunkDimPos(pos.posX + x, pos.posZ + z, pos.dim);
                ClaimResult result = ClaimedChunks.instance.claimChunk(p, pos1, false);

                if (x == 0 && z == 0) {
                    switch (result) {
                        case SUCCESS:
                            CHUNK_MODIFIED.send(player, "serverutilities.lang.chunks.chunk_claimed");
                            ServerUtilitiesNotifications.updateChunkMessage(player, pos);
                            break;
                        case DIMENSION_BLOCKED:
                            CANT_MODIFY_CHUNK.createNotification("serverutilities.lang.chunks.claiming_not_enabled_dim")
                                    .setError().send(player);
                            break;
                        case NO_POWER:
                            break;
                        default:
                            CANT_MODIFY_CHUNK.createNotification("serverutilities.lang.chunks.cant_modify_chunk")
                                    .setError().send(player);
                            break;
                    }
                }
            }
        }
    }
}
