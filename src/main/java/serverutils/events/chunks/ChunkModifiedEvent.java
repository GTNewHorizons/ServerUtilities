package serverutils.events.chunks;

import javax.annotation.Nullable;

import cpw.mods.fml.common.eventhandler.Cancelable;
import serverutils.data.ClaimedChunk;
import serverutils.events.ServerUtilitiesEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.math.ChunkDimPos;

public abstract class ChunkModifiedEvent extends ServerUtilitiesEvent {

    private final ClaimedChunk chunk;
    private final ForgePlayer player;

    public ChunkModifiedEvent(ClaimedChunk c, @Nullable ForgePlayer p) {
        chunk = c;
        player = p;
    }

    public ClaimedChunk getChunk() {
        return chunk;
    }

    @Nullable
    public ForgePlayer getPlayer() {
        return player;
    }

    @Cancelable
    public static class Claim extends ServerUtilitiesEvent {

        private final ChunkDimPos chunkDimPos;
        private final ForgePlayer player;

        public Claim(ChunkDimPos c, ForgePlayer p) {
            chunkDimPos = c;
            player = p;
        }

        public ChunkDimPos getChunkDimPos() {
            return chunkDimPos;
        }

        public ForgePlayer getPlayer() {
            return player;
        }
    }

    public static class Claimed extends ChunkModifiedEvent {

        public Claimed(ClaimedChunk c, @Nullable ForgePlayer p) {
            super(c, p);
        }
    }

    public static class Unclaimed extends ChunkModifiedEvent {

        public Unclaimed(ClaimedChunk c, @Nullable ForgePlayer p) {
            super(c, p);
        }
    }

    public static class Loaded extends ChunkModifiedEvent {

        public Loaded(ClaimedChunk c, @Nullable ForgePlayer p) {
            super(c, p);
        }
    }

    public static class Unloaded extends ChunkModifiedEvent {

        public Unloaded(ClaimedChunk c, @Nullable ForgePlayer p) {
            super(c, p);
        }
    }
}
