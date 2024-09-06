package serverutils.integration.navigator;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.ChunkCoordIntPair;

import org.lwjgl.input.Keyboard;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.gtnewhorizons.navigator.api.util.Util;

import serverutils.client.gui.ClientClaimedChunks;
import serverutils.lib.EnumTeamColor;
import serverutils.net.MessageClaimedChunksModify;
import serverutils.net.MessageNavigatorRequest;

public class ClaimsLocation implements IWaypointAndLocationProvider {

    private final int blockX;
    private final int blockZ;
    private final int dimensionId;
    private final ClientClaimedChunks.ChunkData chunkData;

    public ClaimsLocation(int chunkX, int chunkZ, int dim, ClientClaimedChunks.ChunkData data) {
        blockX = Util.coordChunkToBlock(chunkX);
        blockZ = Util.coordChunkToBlock(chunkZ);
        dimensionId = dim;
        chunkData = data;
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
        return getTeamColor().getEnumChatFormatting() + getTeam().nameComponent.getUnformattedText();
    }

    public EnumTeamColor getTeamColor() {
        return getTeam().color;
    }

    public boolean isOwnTeam() {
        return getTeam().isMember;
    }

    public boolean isAlly() {
        return getTeam().isAlly;
    }

    public ClientClaimedChunks.Team getTeam() {
        return chunkData.team;
    }

    public boolean isLoaded() {
        return chunkData.isLoaded();
    }

    public String loadedHint() {
        return isLoaded() ? EnumChatFormatting.GREEN + I18n.format("serverutilities.lang.chunks.chunk_loaded") : "";
    }

    public String teamHint() {
        if (isOwnTeam()) {
            return EnumChatFormatting.DARK_AQUA + I18n.format("serverutilities.jm.own_team");
        } else if (isAlly()) {
            return EnumChatFormatting.YELLOW + I18n.format("serverutilities.lang.team_status.ally");
        } else {
            return "";
        }
    }

    public String claimHint() {
        return EnumChatFormatting.DARK_GRAY + I18n.format("serverutilities.jm.claim");
    }

    public String toggleLoadHint() {
        return EnumChatFormatting.DARK_GRAY + I18n.format("serverutilities.jm.load_hint");
    }

    public String unclaimHint() {
        return EnumChatFormatting.DARK_GRAY + I18n
                .format("serverutilities.jm.unclaim_hint", Keyboard.getKeyName(NavigatorApi.ACTION_KEY.getKeyCode()));
    }

    public void toggleLoaded() {
        // Double click loads/unloads the chunk
        int selectionMode = isLoaded() ? MessageClaimedChunksModify.UNLOAD : MessageClaimedChunksModify.LOAD;
        int chunkX = getChunkX();
        int chunkZ = getChunkZ();
        Collection<ChunkCoordIntPair> chunks = Collections.singleton(new ChunkCoordIntPair(chunkX, chunkZ));
        new MessageClaimedChunksModify(chunkX, chunkZ, selectionMode, chunks).sendToServer();
        new MessageNavigatorRequest(chunkX, chunkX, chunkZ, chunkZ).sendToServer();
        chunkData.setLoaded(!isLoaded());
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
