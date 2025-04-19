package serverutils.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesNotifications;
import serverutils.ServerUtilitiesPermissions;
import serverutils.events.chunks.ChunkModifiedEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.net.MessageClaimedChunksUpdate;

public class ClaimedChunks {

    public static ClaimedChunks instance;

    public static boolean isActive() {
        return instance != null && ServerUtilitiesConfig.world.chunk_claiming;
    }

    public final Universe universe;
    private final Collection<ClaimedChunk> pendingChunks = new HashSet<>();
    private final Map<ChunkDimPos, ClaimedChunk> map = new HashMap<>();
    public long nextChunkloaderUpdate;
    private boolean isDirty = true;
    private static boolean forceSave = false;

    public ClaimedChunks(Universe u) {
        universe = u;
    }

    @Nullable
    public ForgeTeam getChunkTeam(ChunkDimPos pos) {
        ClaimedChunk chunk = getChunk(pos);
        return chunk == null ? null : chunk.getTeam();
    }

    public void markDirty() {
        isDirty = true;
    }

    public void clear() {
        pendingChunks.clear();
        map.clear();
        nextChunkloaderUpdate = 0L;
        isDirty = true;
        forceSave = false;
    }

    public void processQueue() {
        if (!pendingChunks.isEmpty()) {
            for (ClaimedChunk chunk : pendingChunks) {
                ClaimedChunk prevChunk = map.put(chunk.getPos(), chunk);

                if (prevChunk != null && prevChunk != chunk) {
                    prevChunk.setInvalid();
                }

                markDirty();
            }

            pendingChunks.clear();
        }

        Iterator<ClaimedChunk> iterator = map.values().iterator();

        while (iterator.hasNext()) {
            ClaimedChunk chunk = iterator.next();

            if (chunk.isInvalid()) {
                ServerUtilitiesLoadedChunkManager.INSTANCE.unforceChunk(chunk);
                iterator.remove();
            }
        }
    }

    public void update(Universe universe, long now) {
        if (nextChunkloaderUpdate <= now) {
            nextChunkloaderUpdate = now + Ticks.MINUTE.millis();
            markDirty();
        }

        if (isDirty) {
            processQueue();

            if (ServerUtilitiesConfig.world.chunk_loading) {
                for (ForgeTeam team : universe.getTeams()) {
                    ServerUtilitiesTeamData.get(team).canForceChunks = ServerUtilitiesLoadedChunkManager.INSTANCE
                            .canForceChunks(team);
                }

                for (ClaimedChunk chunk : getAllChunks()) {
                    boolean force = chunk.getData().canForceChunks && chunk.isLoaded();

                    if (chunk.forced == null || chunk.forced != force) {
                        if (force) {
                            ServerUtilitiesLoadedChunkManager.INSTANCE.forceChunk(universe.server, chunk);
                        } else {
                            ServerUtilitiesLoadedChunkManager.INSTANCE.unforceChunk(chunk);
                        }
                    }
                }
            }

            for (EntityPlayerMP player : universe.server.getConfigurationManager().playerEntityList) {
                ChunkDimPos playerPos = new ChunkDimPos(player);
                int startX = playerPos.posX - ChunkSelectorMap.TILES_GUI2;
                int startZ = playerPos.posZ - ChunkSelectorMap.TILES_GUI2;
                new MessageClaimedChunksUpdate(startX, startZ, player).sendTo(player);
                ServerUtilitiesNotifications.updateChunkMessage(player, playerPos);
            }

            isDirty = false;
        }
    }

    @Nullable
    public ClaimedChunk getChunk(ChunkDimPos pos) {
        if (ServerUtilitiesConfig.world.blockDimension(pos.dim)) {
            return null;
        }

        ClaimedChunk chunk = map.get(pos);
        return chunk == null || chunk.isInvalid() ? null : chunk;
    }

    public void removeChunk(ChunkDimPos pos) {
        ClaimedChunk prevChunk = map.get(pos);

        if (prevChunk != null) {
            prevChunk.setInvalid();
            markDirty();
        }
    }

    public void addChunk(ClaimedChunk chunk) {
        pendingChunks.add(chunk);
        chunk.getTeam().claimedChunks.add(chunk);
        chunk.getTeam().markDirty();
        markDirty();
    }

    public Collection<ClaimedChunk> getAllChunks() {
        return map.isEmpty() ? Collections.emptyList() : map.values();
    }

    public Set<ChunkDimPos> getAllClaimedPositions() {
        return map.isEmpty() ? Collections.emptySet() : map.keySet();
    }

    public Set<ClaimedChunk> getTeamChunks(@Nullable ForgeTeam team, OptionalInt dimension, boolean includePending) {
        if (team == null) {
            return Collections.emptySet();
        }

        Set<ClaimedChunk> set;
        if (dimension.isPresent()) {
            set = new HashSet<>();
            for (ClaimedChunk chunk : team.claimedChunks) {
                if (chunk.getPos().dim == dimension.getAsInt()) {
                    set.add(chunk);
                }
            }
        } else {
            set = new HashSet<>(team.claimedChunks);
        }

        if (includePending) {
            for (ClaimedChunk chunk : pendingChunks) {
                if (team.equalsTeam(chunk.getTeam())
                        && (!dimension.isPresent() || dimension.getAsInt() == chunk.getPos().dim)) {
                    set.add(chunk);
                }
            }
        }

        return set;
    }

    public Set<ClaimedChunk> getTeamChunks(@Nullable ForgeTeam team, OptionalInt dimension) {
        return getTeamChunks(team, dimension, false);
    }

    public static boolean canAttackEntity(EntityPlayer player, Entity target) {
        if (!isActive() || player.worldObj == null || !(player instanceof EntityPlayerMP)) {
            return true;
        } else if (target instanceof EntityPlayer) {
            if (ServerUtilitiesConfig.world.safe_spawn && player.worldObj.provider.dimensionId == 0
                    && ServerUtilitiesUniverseData.isInSpawn(instance.universe.server, new ChunkDimPos(target))) {
                return false;
            } else if (ServerUtilitiesConfig.world.enable_pvp.isDefault()) {
                return ServerUtilitiesPlayerData.get(instance.universe.getPlayer(player)).enablePVP()
                        && ServerUtilitiesPlayerData.get(instance.universe.getPlayer((EntityPlayer) target))
                                .enablePVP();
            }

            return ServerUtilitiesConfig.world.enable_pvp.isTrue();
        } else if (!(target instanceof IMob)) {
            ClaimedChunk chunk = instance.getChunk(new ChunkDimPos(target));

            return chunk == null
                    || PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_ATTACK_ANIMALS)
                    || chunk.getTeam()
                            .hasStatus(instance.universe.getPlayer(player), chunk.getData().getAttackEntitiesStatus());
        }

        return true;
    }

    public static boolean blockBlockEditing(EntityPlayer player, int x, int y, int z, int meta) {
        if (!isActive() || player.worldObj == null
                || !(player instanceof EntityPlayerMP)
                || !ServerUtilitiesConfig.teams.grief_protection) {
            return false;
        }

        if (meta == 0) {
            meta = player.worldObj.getBlockMetadata(x, y, z);
        }

        Block block = player.worldObj.getBlock(x, y, z);

        ClaimedChunk chunk = instance.getChunk(new ChunkDimPos(x, y, z, player.dimension));

        return chunk != null && !ServerUtilitiesPermissions.hasBlockEditingPermission(player, block)
                && !chunk.getTeam()
                        .hasStatus(instance.universe.getPlayer(player), chunk.getData().getEditBlocksStatus());
    }

    public static boolean blockBlockInteractions(EntityPlayer player, int x, int y, int z, int meta) {
        if (!isActive() || player.worldObj == null
                || !(player instanceof EntityPlayerMP)
                || !ServerUtilitiesConfig.teams.interaction_protection) {
            return false;
        }

        if (meta == 0) {
            meta = player.worldObj.getBlockMetadata(x, y, z);
        }

        Block block = player.worldObj.getBlock(x, y, z);

        ClaimedChunk chunk = instance.getChunk(new ChunkDimPos(x, y, z, player.dimension));
        return chunk != null && !ServerUtilitiesPermissions.hasBlockInteractionPermission(player, block)
                && !chunk.getTeam()
                        .hasStatus(instance.universe.getPlayer(player), chunk.getData().getInteractWithBlocksStatus());
    }

    public static boolean blockItemUse(EntityPlayer player, int x, int y, int z) {
        if (!isActive() || player.worldObj == null
                || !(player instanceof EntityPlayerMP)
                || player.getHeldItem() == null
                || !ServerUtilitiesConfig.teams.grief_protection) {
            return false;
        }

        ClaimedChunk chunk = instance.getChunk(new ChunkDimPos(x, y, z, player.dimension));
        return chunk != null && !ServerUtilitiesPermissions.hasItemUsePermission(player, player.getHeldItem().getItem())
                && !chunk.getTeam().hasStatus(instance.universe.getPlayer(player), chunk.getData().getUseItemsStatus());
    }

    public boolean canPlayerModify(ForgePlayer player, ChunkDimPos pos, String perm) {
        ClaimedChunk chunk = getChunk(pos);

        if (chunk == null) {
            return true;
        } else if (ServerUtilitiesConfig.world.blockDimension(pos.dim)) {
            return false;
        }

        return player.hasTeam() && chunk.getTeam().equalsTeam(player.team) || perm.isEmpty()
                || player.hasPermission(perm);
    }

    public ClaimResult claimChunk(ForgePlayer player, ChunkDimPos pos) {
        return claimChunk(player, pos, true);
    }

    public ClaimResult claimChunk(ForgePlayer player, ChunkDimPos pos, boolean checkLimits) {
        if (!player.hasTeam()) {
            return ClaimResult.NO_TEAM;
        } else if (checkLimits && ServerUtilitiesConfig.world.blockDimension(pos.dim)) {
            return ClaimResult.DIMENSION_BLOCKED;
        }

        ServerUtilitiesTeamData data = ServerUtilitiesTeamData.get(player.team);

        if (checkLimits && !player.hasPermission(ServerUtilitiesPermissions.CLAIMS_BYPASS_LIMITS)) {
            int max = data.getMaxClaimChunks();
            if (max == 0 || getTeamChunks(data.team, OptionalInt.empty(), true).size() >= max) {
                return ClaimResult.NO_POWER;
            }
        }

        ClaimedChunk chunk = getChunk(pos);

        if (chunk != null) {
            return ClaimResult.ALREADY_CLAIMED;
        }

        if (checkLimits && new ChunkModifiedEvent.Claim(pos, player).post()) {
            return ClaimResult.BLOCKED;
        }

        chunk = new ClaimedChunk(pos, data);
        addChunk(chunk);
        new ChunkModifiedEvent.Claimed(chunk, player).post();
        return ClaimResult.SUCCESS;
    }

    public boolean unclaimChunk(@Nullable ForgePlayer player, ChunkDimPos pos) {
        ClaimedChunk chunk = map.get(pos);

        if (chunk != null && !chunk.isInvalid()) {
            if (chunk.isLoaded()) {
                new ChunkModifiedEvent.Unloaded(chunk, player).post();
            }

            chunk.setLoaded(false);
            new ChunkModifiedEvent.Unclaimed(chunk, player).post();
            removeChunk(pos);
            return true;
        }

        return false;
    }

    public void unclaimAllChunks(@Nullable ForgePlayer player, ForgeTeam team, OptionalInt dim) {
        for (ClaimedChunk chunk : getTeamChunks(team, dim)) {
            ChunkDimPos pos = chunk.getPos();

            if (chunk.isLoaded()) {
                new ChunkModifiedEvent.Unloaded(chunk, player).post();
            }

            chunk.setLoaded(false);
            new ChunkModifiedEvent.Unclaimed(chunk, player).post();
            removeChunk(pos);
        }
    }

    public boolean loadChunk(@Nullable ForgePlayer player, ForgeTeam team, ChunkDimPos pos) {
        ClaimedChunk chunk = getChunk(pos);

        if (chunk == null || chunk.isLoaded()) {
            return false;
        }

        int max = ServerUtilitiesTeamData.get(team).getMaxChunkloaderChunks();

        if (max == 0) {
            return false;
        }

        int loadedChunks = 0;

        for (ClaimedChunk c : getTeamChunks(team, OptionalInt.empty())) {
            if (c.isLoaded()) {
                loadedChunks++;

                if (loadedChunks >= max) {
                    return false;
                }
            }
        }

        if (chunk.setLoaded(true)) {
            new ChunkModifiedEvent.Loaded(chunk, player).post();
        }

        return true;
    }

    public boolean unloadChunk(@Nullable ForgePlayer player, ChunkDimPos pos) {
        ClaimedChunk chunk = getChunk(pos);

        if (chunk == null || !chunk.isLoaded()) {
            return false;
        }

        new ChunkModifiedEvent.Unloaded(chunk, player).post();
        chunk.setLoaded(false);
        return true;
    }

    public void forceSave() {
        forceSave = true;
    }

    public static boolean isForcedToSave() {
        return instance != null && forceSave;
    }
}
