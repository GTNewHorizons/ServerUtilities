package serverutils.utils.world.claims;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import latmod.lib.Bits;
import latmod.lib.LMJsonUtils;
import latmod.lib.LMMapUtils;
import latmod.lib.LMUtils;
import latmod.lib.MathHelperLM;
import latmod.lib.util.EnumEnabled;
import serverutils.lib.BlockDimPos;
import serverutils.lib.LMDimUtils;
import serverutils.lib.LMNBTUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.item.LMInvUtils;
import serverutils.utils.mod.config.ServerUtilitiesConfigGeneral;
import serverutils.utils.world.LMPlayer;
import serverutils.utils.world.LMPlayerServer;
import serverutils.utils.world.LMWorld;
import serverutils.utils.world.LMWorldServer;
import serverutils.utils.world.PersonalSettings;

public class ClaimedChunks {

    public final Map<Integer, Map<Long, ClaimedChunk>> chunks;

    public ClaimedChunks() {
        chunks = new HashMap<>();
    }

    public List<ClaimedChunk> getAllChunks() {
        ArrayList<ClaimedChunk> l = new ArrayList<>();
        for (Map<Long, ClaimedChunk> m : chunks.values()) l.addAll(m.values());
        return l;
    }

    public void load(NBTTagCompound tag) {
        chunks.clear();

        Map<String, NBTTagCompound> tag1 = LMNBTUtils.toMapWithType(tag.getCompoundTag("ClaimedChunks"));

        for (Map.Entry<String, NBTTagCompound> e : tag1.entrySet()) {
            try {
                int dim = Integer.parseInt(e.getKey());
                Map<Long, ClaimedChunk> map = new HashMap<>();
                Map<String, NBTTagList> tag2 = LMNBTUtils.toMapWithType(e.getValue());

                for (Map.Entry<String, NBTTagList> e1 : tag2.entrySet()) {
                    NBTTagList chunksList = e1.getValue();

                    for (int k = 0; k < chunksList.tagCount(); k++) {
                        int[] ai = chunksList.func_150306_c(k);
                        ClaimedChunk c = new ClaimedChunk(Integer.parseInt(e1.getKey()), dim, ai[0], ai[1]);
                        if (ai.length >= 3 && ai[2] == 1) c.isChunkloaded = true;
                        map.put(Long.valueOf(Bits.intsToLong(ai[0], ai[1])), c);
                    }
                }

                chunks.put(dim, map);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void load(JsonObject group) {
        for (Map.Entry<String, JsonElement> e : group.entrySet()) {
            int dim = Integer.parseInt(e.getKey());

            Map<Long, ClaimedChunk> map = new HashMap<>();

            for (Map.Entry<String, JsonElement> e1 : e.getValue().getAsJsonObject().entrySet()) {
                try {
                    LMPlayerServer p = LMWorldServer.inst.getPlayer(LMUtils.fromString(e1.getKey()));

                    if (p != null) {
                        JsonArray chunksList = e1.getValue().getAsJsonArray();

                        for (int k = 0; k < chunksList.size(); k++) {
                            int[] ai = LMJsonUtils.fromIntArray(chunksList.get(k));

                            if (ai != null) {
                                ClaimedChunk c = new ClaimedChunk(p.getPlayerID(), dim, ai[0], ai[1]);
                                if (ai.length >= 3 && ai[2] == 1) c.isChunkloaded = true;
                                map.put(Bits.intsToLong(ai[0], ai[1]), c);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            chunks.put(dim, map);
        }
    }

    public void save(JsonObject group) {
        Comparator<Map.Entry<Integer, Map<Long, ClaimedChunk>>> comparator = LMMapUtils.byKeyNumbers();
        Comparator<Map.Entry<Long, ClaimedChunk>> comparator2 = LMMapUtils.byKeyNumbers();

        for (Map.Entry<Integer, Map<Long, ClaimedChunk>> e : LMMapUtils.sortedEntryList(chunks, comparator)) {
            JsonObject o1 = new JsonObject();

            for (ClaimedChunk c : LMMapUtils.values(e.getValue(), comparator2)) {
                LMPlayerServer p = c.getOwnerS();

                if (p != null) {
                    String id = p.getStringUUID();
                    if (!o1.has(id)) o1.add(id, new JsonArray());

                    JsonArray a = o1.get(id).getAsJsonArray();

                    JsonArray a1 = new JsonArray();
                    a1.add(new JsonPrimitive(c.posX));
                    a1.add(new JsonPrimitive(c.posZ));
                    if (c.isChunkloaded) a1.add(new JsonPrimitive(1));
                    a.add(a1);
                }
            }

            group.add(e.getKey().toString(), o1);
        }
    }

    public ClaimedChunk getChunk(int dim, int cx, int cz) {
        if (!chunks.containsKey(dim)) return null;
        return chunks.get(dim).get(Long.valueOf(Bits.intsToLong(cx, cz)));
    }

    public List<ClaimedChunk> getChunks(LMPlayerServer p, Integer dim) {
        List<ClaimedChunk> list = new ArrayList<>();
        if (p == null || p.isFake()) return list;

        if (dim == null) {
            for (Map<Long, ClaimedChunk> map : chunks.values()) {
                for (ClaimedChunk c : map.values()) {
                    if (c != null && c.ownerID == p.getPlayerID()) list.add(c);
                }
            }
        } else {
            Map<Long, ClaimedChunk> map = chunks.get(dim);
            if (map == null) return list;

            for (ClaimedChunk c : map.values()) {
                if (c != null && c.ownerID == p.getPlayerID()) list.add(c);
            }
        }

        return list;
    }

    public boolean put(ClaimedChunk c) {
        if (c == null) return false;
        Map<Long, ClaimedChunk> map = chunks.get(c.dim);
        if (map == null) chunks.put(c.dim, map = new HashMap<>());
        return map.put(c.getLongPos(), c) == null;
    }

    public ClaimedChunk remove(int dim, int cx, int cz) {
        Map<Long, ClaimedChunk> map = chunks.get(dim);
        if (map != null) {
            ClaimedChunk chunk = map.remove(Long.valueOf(Bits.intsToLong(cx, cz)));

            if (chunk != null) {
                if (map.isEmpty()) chunks.remove(dim);
                return chunk;
            }
        }

        return null;
    }

    public ChunkType getType(int dim, int cx, int cz) {
        World w = LMDimUtils.getWorld(dim);
        if (w == null || !w.getChunkProvider().chunkExists(cx, cz)) return ChunkType.UNLOADED;
        if (isInSpawn(dim, cx, cz)) return ChunkType.SPAWN;
        if (LMWorldServer.inst.settings.getWB(dim).isOutside(cx, cz)) return ChunkType.WORLD_BORDER;
        ClaimedChunk c = getChunk(dim, cx, cz);
        if (c == null) return ChunkType.WILDERNESS;
        return new ChunkType.PlayerClaimed(LMWorldServer.inst.getPlayer(c.ownerID));
    }

    public ChunkType getTypeD(int dim, ChunkCoordinates pos) {
        return getType(dim, MathHelperLM.chunk(pos.posX), MathHelperLM.chunk(pos.posZ));
    }

    public static ChunkType getChunkTypeFromI(int i) {
        if (i <= 0) return ChunkType.UNCLAIMED_VALUES[-i];
        LMPlayer p = LMWorld.getWorld().getPlayer(i);
        return (p == null) ? ChunkType.WILDERNESS : new ChunkType.PlayerClaimed(p);
    }

    public static boolean isInSpawn(int dim, int cx, int cz) {
        if (dim != 0 || (!ServerUtilitiesLib.isDedicatedServer()
                && !ServerUtilitiesConfigGeneral.spawn_area_in_sp.getAsBoolean()))
            return false;
        int radius = ServerUtilitiesLib.getServer().getSpawnProtectionSize();
        if (radius <= 0) return false;
        BlockDimPos c = LMDimUtils.getSpawnPoint(0);
        int minX = MathHelperLM.chunk(c.x + 0.5D - radius);
        int minZ = MathHelperLM.chunk(c.z + 0.5D - radius);
        int maxX = MathHelperLM.chunk(c.x + 0.5D + radius);
        int maxZ = MathHelperLM.chunk(c.z + 0.5D + radius);
        return cx >= minX && cx <= maxX && cz >= minZ && cz <= maxZ;
    }

    public static boolean isInSpawnD(int dim, double x, double z) {
        return dim == 0 && isInSpawn(dim, MathHelperLM.chunk(x), MathHelperLM.chunk(z));
    }

    public boolean allowExplosion(int dim, int cx, int cz) {
        if (dim == 0 && ServerUtilitiesConfigGeneral.safe_spawn.getAsBoolean() && isInSpawn(dim, cx, cz)) return false;
        else if (LMWorldServer.inst.settings.getWB(dim).isOutside(cx, cz)) return false;
        else {
            ClaimedChunk c = getChunk(dim, cx, cz);
            if (c != null) {
                LMPlayerServer p = c.getOwnerS();

                if (p != null) {
                    EnumEnabled fe = p.getRank().config.forced_explosions.get();
                    if (fe == null) return p.getSettings().get(PersonalSettings.EXPLOSIONS);
                    else return fe.isEnabled();
                }
            }
        }

        return true;
    }

    public static boolean canPlayerInteract(EntityPlayer ep, ChunkCoordinates pos, boolean leftClick) {
        if (ep == null || ep.worldObj == null || ep.worldObj.isRemote) return true;

        LMPlayerServer p = LMWorldServer.inst.getPlayer(ep);

        if (p == null) return true;
        else if (!p.isFake() && p.allowInteractSecure()) return true;
        else if (LMWorldServer.inst.settings.getWB(ep.dimension).isOutsideD(pos.posX, pos.posZ)) return false;

        if (leftClick) {
            if (p.getRank().config.break_whitelist.getAsStringList()
                    .contains(LMInvUtils.getRegName(ep.worldObj.getBlock(pos.posX, pos.posY, pos.posZ))))
                return true;
        }

        ChunkType type = LMWorldServer.inst.claimedChunks.getTypeD(ep.dimension, pos);
        return type.canInteract(p, leftClick);
    }
}
