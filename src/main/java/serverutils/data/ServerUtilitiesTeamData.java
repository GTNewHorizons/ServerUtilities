package serverutils.data;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.util.Constants;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.events.team.ForgeTeamConfigEvent;
import serverutils.events.team.ForgeTeamDeletedEvent;
import serverutils.events.team.ForgeTeamLoadedEvent;
import serverutils.events.team.ForgeTeamSavedEvent;
import serverutils.lib.EnumTeamStatus;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.TeamData;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.NBTUtils;

public class ServerUtilitiesTeamData extends TeamData {

    public static ServerUtilitiesTeamData get(ForgeTeam team) {
        return team.getData().get(ServerUtilities.MOD_ID);
    }

    @SubscribeEvent
    public void onTeamSaved(ForgeTeamSavedEvent event) {
        if (!ClaimedChunks.isActive() && !ClaimedChunks.isForcedToSave()) {
            return;
        }

        NBTTagCompound nbt = new NBTTagCompound();

        Int2ObjectMap<NBTTagList> claimedChunks = new Int2ObjectOpenHashMap<>();

        for (ClaimedChunk chunk : ClaimedChunks.instance
                .getTeamChunks(event.getTeam(), OptionalInt.empty(), ClaimedChunks.isForcedToSave())) {
            ChunkDimPos pos = chunk.getPos();

            NBTTagList list = claimedChunks.get(pos.dim);

            if (list == null) {
                list = new NBTTagList();
                claimedChunks.put(pos.dim, list);
            }

            NBTTagCompound chunkNBT = new NBTTagCompound();
            chunkNBT.setInteger("x", pos.posX);
            chunkNBT.setInteger("z", pos.posZ);
            chunkNBT.setBoolean("preDecay", chunk.preDecay);

            if (chunk.isLoaded()) {
                chunkNBT.setBoolean("loaded", true);
            }

            list.appendTag(chunkNBT);
        }

        NBTTagCompound claimedChunksTag = new NBTTagCompound();

        for (Map.Entry<Integer, NBTTagList> entry : claimedChunks.int2ObjectEntrySet()) {
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

        ServerUtilitiesTeamData data = get(event.getTeam());

        NBTTagCompound claimedChunksTag = nbt.getCompoundTag("ClaimedChunks");

        for (String dim : claimedChunksTag.func_150296_c()) {
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
                chunk.preDecay = chunkNBT.getBoolean("preDecay");
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
    private boolean endermen = false;
    public boolean canForceChunks = false;
    private int cachedMaxClaimChunks, cachedMaxChunkloaderChunks;
    public boolean chunkloadsDecayed;

    ServerUtilitiesTeamData(ForgeTeam t) {
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
        nbt.setBoolean("Endermen", endermen);
        nbt.setString("EditBlocks", editBlocks.getName());
        nbt.setString("InteractWithBlocks", interactWithBlocks.getName());
        nbt.setString("AttackEntities", attackEntities.getName());
        nbt.setString("UseItems", useItems.getName());
        nbt.setBoolean("DecayedChunkloads", chunkloadsDecayed);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        explosions = nbt.getBoolean("Explosions");
        endermen = nbt.getBoolean("Endermen");
        editBlocks = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("EditBlocks"));
        interactWithBlocks = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("InteractWithBlocks"));
        attackEntities = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("AttackEntities"));
        useItems = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("UseItems"));
        chunkloadsDecayed = nbt.getBoolean("DecayedChunkloads");

        if (ClaimedChunks.isActive() && nbt.hasKey("ClaimedChunks")) {
            team.markDirty();
            NBTTagCompound claimedChunksTag = nbt.getCompoundTag("ClaimedChunks");

            for (String dim : claimedChunksTag.func_150296_c()) {
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

        group.addBool("explosions", () -> explosions, v -> explosions = v, false)
                .setCanEdit(ServerUtilitiesConfig.world.enable_explosions.isDefault());
        group.addBool("endermen", () -> endermen, v -> endermen = v, false)
                .setCanEdit(ServerUtilitiesConfig.world.enable_endermen.isDefault());
        group.addEnum("blocks_edit", () -> editBlocks, v -> editBlocks = v, EnumTeamStatus.NAME_MAP_PERMS)
                .setCanEdit(ServerUtilitiesConfig.teams.grief_protection);
        group.addEnum(
                "blocks_interact",
                () -> interactWithBlocks,
                v -> interactWithBlocks = v,
                EnumTeamStatus.NAME_MAP_PERMS).setCanEdit(ServerUtilitiesConfig.teams.interaction_protection);;
        group.addEnum("attack_entities", () -> attackEntities, v -> attackEntities = v, EnumTeamStatus.NAME_MAP_PERMS);
        group.addEnum("use_items", () -> useItems, v -> useItems = v, EnumTeamStatus.NAME_MAP_PERMS)
                .setCanEdit(ServerUtilitiesConfig.teams.grief_protection);
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

    public boolean forbidsEndermanGriefing() {
        return !endermen;
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
            cachedMaxChunkloaderChunks += player.getRankConfig(ServerUtilitiesPermissions.CHUNKLOADER_MAX_CHUNKS)
                    .getInt();
        }

        return cachedMaxChunkloaderChunks;
    }

    public Set<ClaimedChunk> getTeamChunks() {
        if (!ClaimedChunks.isActive()) {
            return Collections.emptySet();
        }
        return ClaimedChunks.instance.getTeamChunks(team, OptionalInt.empty());
    }

    @Override
    public void clearCache() {
        cachedMaxClaimChunks = -1;
        cachedMaxChunkloaderChunks = -1;
    }

    public void decayChunkloads() {
        chunkloadsDecayed = true;
        for (ClaimedChunk chunk : getTeamChunks()) {
            if (chunk.isLoaded()) {
                chunk.preDecay = true;
                chunk.setLoaded(false);
            }
        }
        team.markDirty();
    }

    public void unDecayChunkloads() {
        chunkloadsDecayed = false;
        for (ClaimedChunk chunk : getTeamChunks()) {
            if (chunk.preDecay) {
                chunk.preDecay = false;
                chunk.setLoaded(true);
            }
        }
        team.markDirty();
    }
}
