package serverutils.utils.data;

import java.io.File;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.util.Constants;

import com.feed_the_beast.ftblib.events.team.ForgeTeamConfigEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamDeletedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamLoadedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamSavedEvent;
import com.feed_the_beast.ftblib.lib.EnumTeamStatus;
import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.TeamData;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftblib.lib.util.FileUtils;
import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesPermissions;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * @author LatvianModder
 */
public class FTBUtilitiesTeamData extends TeamData {

    public static FTBUtilitiesTeamData get(ForgeTeam team) {
        return team.getData().get(ServerUtilities.MOD_ID);
    }

    @SubscribeEvent
    public void onTeamSaved(ForgeTeamSavedEvent event) {
        if (!ClaimedChunks.isActive()) {
            return;
        }

        NBTTagCompound nbt = new NBTTagCompound();

        Int2ObjectMap<NBTTagList> claimedChunks = new Int2ObjectOpenHashMap<>();

        for (ClaimedChunk chunk : ClaimedChunks.instance.getTeamChunks(event.getTeam(), OptionalInt.empty())) {
            ChunkDimPos pos = chunk.getPos();

            NBTTagList list = claimedChunks.get(pos.dim);

            if (list == null) {
                list = new NBTTagList();
                claimedChunks.put(pos.dim, list);
            }

            NBTTagCompound chunkNBT = new NBTTagCompound();
            chunkNBT.setInteger("x", pos.posX);
            chunkNBT.setInteger("z", pos.posZ);

            if (chunk.isLoaded()) {
                chunkNBT.setBoolean("loaded", true);
            }

            list.appendTag(chunkNBT);
        }

        NBTTagCompound claimedChunksTag = new NBTTagCompound();

        for (Map.Entry<Integer, NBTTagList> entry : claimedChunks.entrySet()) {
            claimedChunksTag.setTag(entry.getKey().toString(), entry.getValue());
        }

        if (!claimedChunksTag.hasNoTags()) {
            nbt.setTag("ClaimedChunks", claimedChunksTag);
        }

        File file = event.getTeam().getDataFile("claimedchunks");

        if (nbt.hasNoTags()) {
            FileUtils.deleteSafe(file);
        } else {
            NBTUtils.writeNBTSafe(file, nbt);
        }
    }

    @SubscribeEvent
    public void onTeamLoaded(ForgeTeamLoadedEvent event) {
        if (!ClaimedChunks.isActive()) {
            return;
        }

        NBTTagCompound nbt = NBTUtils.readNBT(event.getTeam().getDataFile("claimedchunks"));

        if (nbt == null) {
            return;
        }

        FTBUtilitiesTeamData data = get(event.getTeam());

        NBTTagCompound claimedChunksTag = nbt.getCompoundTag("ClaimedChunks");

        for (String dim : (Set<String>) claimedChunksTag.func_150296_c()) {
            NBTTagList list = claimedChunksTag.getTagList(dim, Constants.NBT.TAG_COMPOUND);
            int dimInt = Integer.parseInt(dim);

            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound chunkNBT = list.getCompoundTagAt(i);
                ClaimedChunk chunk = new ClaimedChunk(
                        new ChunkDimPos(
                                new ChunkCoordIntPair(chunkNBT.getInteger("x"), chunkNBT.getInteger("z")),
                                dimInt),
                        data);
                chunk.setLoaded(chunkNBT.getBoolean("loaded"));
                ClaimedChunks.instance.addChunk(chunk);
            }
        }
    }

    @SubscribeEvent
    public void getTeamSettings(ForgeTeamConfigEvent event) {
        get(event.getTeam()).addConfig(event.getConfig());
    }

    @SubscribeEvent
    public void onTeamDeleted(ForgeTeamDeletedEvent event) {
        if (ClaimedChunks.isActive()) {
            ClaimedChunks.instance.unclaimAllChunks(event.getTeam().getOwner(), event.getTeam(), OptionalInt.empty());
        }
    }

    private EnumTeamStatus editBlocks = EnumTeamStatus.ALLY;
    private EnumTeamStatus interactWithBlocks = EnumTeamStatus.ALLY;
    private EnumTeamStatus attackEntities = EnumTeamStatus.ALLY;
    private EnumTeamStatus useItems = EnumTeamStatus.ALLY;
    private boolean explosions = false;
    public boolean canForceChunks = false;
    private int cachedMaxClaimChunks, cachedMaxChunkloaderChunks;

    FTBUtilitiesTeamData(ForgeTeam t) {
        super(t);
    }

    @Override
    public String getId() {
        return ServerUtilities.MOD_ID;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("Explosions", explosions);
        nbt.setString("EditBlocks", editBlocks.getName());
        nbt.setString("InteractWithBlocks", interactWithBlocks.getName());
        nbt.setString("AttackEntities", attackEntities.getName());
        nbt.setString("UseItems", useItems.getName());
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        explosions = nbt.getBoolean("Explosions");
        editBlocks = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("EditBlocks"));
        interactWithBlocks = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("InteractWithBlocks"));
        attackEntities = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("AttackEntities"));
        useItems = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("UseItems"));

        if (ClaimedChunks.isActive() && nbt.hasKey("ClaimedChunks")) {
            team.markDirty();
            NBTTagCompound claimedChunksTag = nbt.getCompoundTag("ClaimedChunks");

            for (String dim : (Set<String>) claimedChunksTag.func_150296_c()) {
                NBTTagList list = claimedChunksTag.getTagList(dim, Constants.NBT.TAG_COMPOUND);
                int dimInt = Integer.parseInt(dim);

                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound chunkNBT = list.getCompoundTagAt(i);
                    ClaimedChunk chunk = new ClaimedChunk(
                            new ChunkDimPos(
                                    new ChunkCoordIntPair(chunkNBT.getInteger("x"), chunkNBT.getInteger("z")),
                                    dimInt),
                            this);
                    chunk.setLoaded(chunkNBT.getBoolean("loaded"));
                    ClaimedChunks.instance.addChunk(chunk);
                }
            }
        }
    }

    private void addConfig(ConfigGroup main) {
        ConfigGroup group = main.getGroup(ServerUtilities.MOD_ID);
        group.setDisplayName(new ChatComponentText(ServerUtilities.MOD_NAME));

        group.addBool("explosions", () -> explosions, v -> explosions = v, false);
        group.addEnum("blocks_edit", () -> editBlocks, v -> editBlocks = v, EnumTeamStatus.NAME_MAP_PERMS);
        group.addEnum(
                "blocks_interact",
                () -> interactWithBlocks,
                v -> interactWithBlocks = v,
                EnumTeamStatus.NAME_MAP_PERMS);
        group.addEnum("attack_entities", () -> attackEntities, v -> attackEntities = v, EnumTeamStatus.NAME_MAP_PERMS);
        group.addEnum("use_items", () -> useItems, v -> useItems = v, EnumTeamStatus.NAME_MAP_PERMS);
    }

    public EnumTeamStatus getEditBlocksStatus() {
        return editBlocks;
    }

    public EnumTeamStatus getInteractWithBlocksStatus() {
        return interactWithBlocks;
    }

    public EnumTeamStatus getAttackEntitiesStatus() {
        return attackEntities;
    }

    public EnumTeamStatus getUseItemsStatus() {
        return useItems;
    }

    public boolean hasExplosions() {
        return explosions;
    }

    public int getMaxClaimChunks() {
        if (!ClaimedChunks.isActive()) {
            return -1;
        } else if (!team.isValid()) {
            return -2;
        } else if (cachedMaxClaimChunks >= 0) {
            return cachedMaxClaimChunks;
        }

        cachedMaxClaimChunks = 0;

        for (ForgePlayer player : team.getMembers()) {
            cachedMaxClaimChunks += player.getRankConfig(ServerUtilitiesPermissions.CLAIMS_MAX_CHUNKS).getInt();
        }

        return cachedMaxClaimChunks;
    }

    public int getMaxChunkloaderChunks() {
        if (!ClaimedChunks.isActive()) {
            return -1;
        } else if (!team.isValid()) {
            return -2;
        } else if (cachedMaxChunkloaderChunks >= 0) {
            return cachedMaxChunkloaderChunks;
        }

        cachedMaxChunkloaderChunks = 0;

        for (ForgePlayer player : team.getMembers()) {
            cachedMaxChunkloaderChunks += player.getRankConfig(ServerUtilitiesPermissions.CHUNKLOADER_MAX_CHUNKS).getInt();
        }

        return cachedMaxChunkloaderChunks;
    }

    @Override
    public void clearCache() {
        cachedMaxClaimChunks = -1;
        cachedMaxChunkloaderChunks = -1;
    }
}
