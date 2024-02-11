package serverutils.client.gui;

import java.util.Objects;

import net.minecraft.util.IChatComponent;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.io.Bits;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;

public class ClientClaimedChunks {

    public static class Team {

        private static Team currentTeam;

        public static final DataOut.Serializer<Team> SERIALIZER = (data, team) -> {
            data.writeShort(team.uid);
            data.writeTextComponent(team.nameComponent);
            EnumTeamColor.NAME_MAP.write(data, team.color);
            data.writeBoolean(team.isAlly);
            data.writeBoolean(team.isMember);
            data.writeMap(team.chunkPos, DataOut.CHUNK_DIM_POS, ChunkData.SERIALIZER);
            data.writeMap(team.chunks, DataOut.INT, ChunkData.SERIALIZER);
        };

        public static final DataIn.Deserializer<Team> DESERIALIZER = data -> {
            Team team = new Team(data.readShort());
            team.nameComponent = data.readTextComponent();
            team.color = EnumTeamColor.NAME_MAP.read(data);
            team.isAlly = data.readBoolean();
            team.isMember = data.readBoolean();
            currentTeam = team;
            data.readMap(team.chunkPos, DataIn.CHUNK_DIM_POS, ChunkData.DESERIALIZER);
            data.readMap(team.chunks, DataIn.INT, ChunkData.DESERIALIZER);
            return team;
        };

        public final short uid;
        public EnumTeamColor color;
        public IChatComponent nameComponent;
        public boolean isAlly;
        public boolean isMember;
        public final Int2ObjectMap<ChunkData> chunks = new Int2ObjectOpenHashMap<>();
        public final Object2ObjectMap<ChunkDimPos, ChunkData> chunkPos = new Object2ObjectOpenHashMap<>();

        public Team(short id) {
            uid = id;
        }
    }

    public static class ChunkData {

        public static final DataOut.Serializer<ChunkData> SERIALIZER = (data, d) -> data.writeInt(d.flags);
        public static final DataIn.Deserializer<ChunkData> DESERIALIZER = data -> new ChunkData(
                Team.currentTeam,
                data.readInt());

        public static final int LOADED = 1;

        public final Team team;
        private int flags;

        public ChunkData(Team t, int f) {
            team = t;
            flags = f;
        }

        public int hashCode() {
            return Objects.hash(team, flags);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o != null && o.getClass() == ChunkData.class) {
                ChunkData d = (ChunkData) o;
                return team.equals(d.team) && flags == d.flags;
            }
            return false;
        }

        public boolean isLoaded() {
            return Bits.getFlag(flags, LOADED);
        }

        public ChunkData setLoaded(boolean value) {
            flags = Bits.setFlag(flags, LOADED, value);
            return this;
        }

        public ChunkData copy() {
            return new ChunkData(team, flags);
        }

        public int getFlags() {
            return flags;
        }
    }
}
