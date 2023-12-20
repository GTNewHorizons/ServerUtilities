package serverutils.integration.vp;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkCoordIntPair;

import org.lwjgl.input.Keyboard;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.VP;
import com.sinthoras.visualprospecting.integration.model.locations.IWaypointAndLocationProvider;
import com.sinthoras.visualprospecting.integration.model.waypoints.Waypoint;

import serverutils.client.gui.ClientClaimedChunks;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageClaimedChunksModify;
import serverutils.net.MessageJourneyMapRequest;

public class VPClaimsLocation implements IWaypointAndLocationProvider {

    private final int blockX;
    private final int blockZ;
    private final int dimensionId;
    private final String teamName;
    private final boolean loaded;
    private final boolean ally;
    private final boolean member;
    private final EnumTeamColor color;

    public VPClaimsLocation(ChunkDimPos chunk, ClientClaimedChunks.ChunkData data) {
        blockX = Utils.coordChunkToBlock(chunk.posX);
        blockZ = Utils.coordChunkToBlock(chunk.posZ);
        dimensionId = chunk.dim;
        teamName = data.team.nameComponent.getUnformattedText();
        loaded = data.isLoaded();
        color = data.team.color;
        ally = data.team.isAlly;
        member = data.team.isMember;
    }

    public double getBlockX() {
        return blockX + 0.5;
    }

    public double getBlockZ() {
        return blockZ + 0.5;
    }

    public int getDimensionId() {
        return dimensionId;
    }

    public String getTeamName() {
        return color.getEnumChatFormatting() + teamName;
    }

    public EnumTeamColor getTeamColor() {
        return color;
    }

    public boolean getOwnTeam() {
        return member;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String loadedHint() {
        return isLoaded() ? EnumChatFormatting.GREEN + I18n.format("serverutilities.lang.chunks.chunk_loaded") : "";
    }

    public String teamHint() {
        if (member) {
            return EnumChatFormatting.DARK_AQUA + I18n.format("serverutilities.jm.own_team");
        } else if (ally) {
            return EnumChatFormatting.YELLOW + I18n.format("serverutilities.lang.team_status.ally");
        } else {
            return "";
        }
    }

    public String toggleLoadHint() {
        return EnumChatFormatting.DARK_GRAY + I18n.format("serverutilities.jm.load_hint");
    }

    public String unclaimHint() {
        return EnumChatFormatting.DARK_GRAY
                + I18n.format("serverutilities.jm.unclaim_hint", Keyboard.getKeyName(VP.keyAction.getKeyCode()));
    }

    public void toggleLoaded() {
        // Double click loads/unloads the chunk
        int selectionMode = isLoaded() ? MessageClaimedChunksModify.UNLOAD : MessageClaimedChunksModify.LOAD;
        Collection<ChunkCoordIntPair> chunks = Collections
                .singleton(new ChunkCoordIntPair(Utils.coordBlockToChunk(blockX), Utils.coordBlockToChunk(blockZ)));
        new MessageClaimedChunksModify(
                Utils.coordBlockToChunk(blockX),
                Utils.coordBlockToChunk(blockZ),
                selectionMode,
                chunks).sendToServer();
        new MessageJourneyMapRequest(blockX, blockX, blockZ, blockZ).sendToServer();
    }

    public void removeClaim() {
        // Deplete/VP Action key unclaims the chunk
        int selectionMode = MessageClaimedChunksModify.UNCLAIM;
        Collection<ChunkCoordIntPair> chunks = Collections
                .singleton(new ChunkCoordIntPair(Utils.coordBlockToChunk(blockX), Utils.coordBlockToChunk(blockZ)));
        new MessageClaimedChunksModify(
                Utils.coordBlockToChunk(blockX),
                Utils.coordBlockToChunk(blockZ),
                selectionMode,
                chunks).sendToServer();
        new MessageJourneyMapRequest(blockX, blockX, blockZ, blockZ).sendToServer();
    }

    @Override
    public Waypoint toWaypoint() {
        toggleLoaded();
        return null;
    }

    @Override
    public boolean isActiveAsWaypoint() {
        return false;
    }

    @Override
    public void onWaypointCleared() {}

    @Override
    public void onWaypointUpdated(Waypoint waypoint) {}
}
