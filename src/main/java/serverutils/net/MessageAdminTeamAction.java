package serverutils.net;

import java.util.OptionalInt;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunks;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.data.Universe;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;

public class MessageAdminTeamAction extends MessageToServer {

    private static final Predicate<EnumTeamStatus> MEMBERS_PREDICATE = status -> status
            .isEqualOrGreaterThan(EnumTeamStatus.MEMBER);

    public static final String SETTINGS = "settings";
    public static final String OWNER = "owner";
    public static final String MODERATORS = "moderators";
    public static final String MEMBERS = "members";
    public static final String CLAIMS = "claims";

    private String teamId;
    private String action;
    private NBTTagCompound nbt;

    public MessageAdminTeamAction() {}

    public MessageAdminTeamAction(String teamId, String action, NBTTagCompound data) {
        this.teamId = teamId;
        this.action = action;
        nbt = data;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(teamId);
        data.writeString(action);
        data.writeNBT(nbt);
    }

    @Override
    public void readData(DataIn data) {
        teamId = data.readString();
        action = data.readString();
        nbt = data.readNBT();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        ForgePlayer p = Universe.get().getPlayer(player);

        if (!p.hasPermission(ServerUtilitiesPermissions.TEAM_EDIT)) {
            return;
        }

        ForgeTeam team = Universe.get().getTeam(teamId);

        if (!team.isValid()) {
            return;
        }

        switch (action) {
            case SETTINGS -> ServerUtilitiesAPI.editServerConfig(player, team.getSettings(), team);
            case OWNER -> {
                if (nbt.hasNoTags()) {
                    new MessageAdminTeamPlayerList(team, OWNER, MEMBERS_PREDICATE).sendTo(player);
                } else {
                    ForgePlayer target = Universe.get().getPlayer(nbt.getString("player"));

                    if (target != null && team.isMember(target)) {
                        team.setStatus(target, EnumTeamStatus.OWNER);
                    }
                }
            }
            case MODERATORS -> {
                if (nbt.hasNoTags()) {
                    new MessageAdminTeamPlayerList(team, MODERATORS, MEMBERS_PREDICATE).sendTo(player);
                } else {
                    ForgePlayer target = Universe.get().getPlayer(nbt.getString("player"));

                    if (target != null && team.isMember(target)) {
                        team.setStatus(target, nbt.getBoolean("add") ? EnumTeamStatus.MOD : EnumTeamStatus.MEMBER);
                    }
                }
            }
            case MEMBERS -> {
                if (nbt.hasNoTags()) {
                    new MessageAdminTeamPlayerList(team, MEMBERS, status -> true).sendTo(player);
                } else {
                    ForgePlayer target = Universe.get().getPlayer(nbt.getString("player"));

                    if (target != null) {
                        if (nbt.getBoolean("add")) {
                            team.forceAddMember(target);
                        } else {
                            team.removeMember(target);
                        }
                    }
                }
            }
            case CLAIMS -> {
                if (!ClaimedChunks.isActive()) {
                    return;
                }

                if (nbt.hasNoTags()) {
                    new MessageAdminTeamClaimsList(team).sendTo(player);
                } else if (nbt.getBoolean("all")) {
                    ClaimedChunks.instance.unclaimAllChunks(p, team, OptionalInt.empty());
                } else {
                    ChunkDimPos pos = new ChunkDimPos(nbt.getInteger("x"), nbt.getInteger("z"), nbt.getInteger("dim"));
                    ClaimedChunks.instance.unclaimChunk(p, pos);
                }
            }
        }
    }
}
