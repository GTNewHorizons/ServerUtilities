package serverutils.data;

import serverutils.lib.data.ForgeTeam;
import serverutils.lib.math.ChunkDimPos;

public final class ClaimedChunk {

    private final ChunkDimPos pos;
    private final ServerUtilitiesTeamData teamData;
    private boolean loaded;
    private boolean invalid;
    public boolean preDecay;
    public Boolean forced;

    public ClaimedChunk(ChunkDimPos c, ServerUtilitiesTeamData t) {
        pos = c;
        teamData = t;
        loaded = false;
        preDecay = false;
        invalid = false;
        forced = null;
    }

    public boolean isInvalid() {
        return invalid || !getTeam().isValid();
    }

    public void setInvalid() {
        if (!invalid) {
            invalid = true;
            getTeam().claimedChunks.remove(this);
            getTeam().markDirty();
        }
    }

    public ChunkDimPos getPos() {
        return pos;
    }

    public ForgeTeam getTeam() {
        return teamData.team;
    }

    public ServerUtilitiesTeamData getData() {
        return teamData;
    }

    public boolean setLoaded(boolean v) {
        if (loaded != v) {
            loaded = v;

            if (ClaimedChunks.isActive()) {
                ClaimedChunks.instance.markDirty();
            }

            getTeam().markDirty();
            return true;
        }

        return false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean hasExplosions() {
        return teamData.hasExplosions();
    }

    public String toString() {
        return pos.toString() + '+' + loaded;
    }

    public int hashCode() {
        return pos.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o != null && o.getClass() == ClaimedChunk.class) {
            return pos.equalsChunkDimPos(((ClaimedChunk) o).pos);
        }

        return false;
    }
}
