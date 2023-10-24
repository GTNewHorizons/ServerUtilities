package serverutils;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;

public class ServerUtilitiesNotifications {

    public static final ResourceLocation CHUNK_MODIFIED = new ResourceLocation(
            ServerUtilities.MOD_ID,
            "chunk_modified");
    public static final ResourceLocation CHUNK_CHANGED = new ResourceLocation(ServerUtilities.MOD_ID, "chunk_changed");
    public static final ResourceLocation CHUNK_CANT_CLAIM = new ResourceLocation(
            ServerUtilities.MOD_ID,
            "cant_claim_chunk");
    public static final ResourceLocation UNCLAIMED_ALL = new ResourceLocation(ServerUtilities.MOD_ID, "unclaimed_all");
    public static final ResourceLocation TELEPORT = new ResourceLocation(ServerUtilities.MOD_ID, "teleport");
    public static final ResourceLocation RELOAD_SERVER = new ResourceLocation(ServerUtilities.MOD_ID, "reload_server");
    public static final ResourceLocation BACKUP_START = new ResourceLocation(ServerUtilities.MOD_ID, "backup_start");
    public static final ResourceLocation BACKUP_END1 = new ResourceLocation(ServerUtilities.MOD_ID, "backup_end1");
    public static final ResourceLocation BACKUP_END2 = new ResourceLocation(ServerUtilities.MOD_ID, "backup_end2");
    public static final ResourceLocation CONFIG_CHANGED = new ResourceLocation(
            ServerUtilities.MOD_ID,
            "config_changed");

    public static final Notification NO_TEAM = Notification.of(
            new ResourceLocation(ServerUtilities.MOD_ID, "no_team"),
            new ChatComponentTranslation("serverutilities.lang.team.error.no_team")).setError();

    public static void sendCantModifyChunk(MinecraftServer server, EntityPlayerMP player) {
        Notification
                .of(
                        new ResourceLocation(ServerUtilities.MOD_ID, "cant_modify_chunk"),
                        ServerUtilities.lang(player, "serverutilities.lang.chunks.cant_modify_chunk"))
                .setError().send(server, player);
    }

    public static void updateChunkMessage(EntityPlayerMP player, ChunkDimPos pos) {
        if (!ClaimedChunks.isActive()) {
            return;
        }

        ClaimedChunk chunk = ClaimedChunks.instance.getChunk(pos);
        ForgeTeam team = chunk == null ? null : chunk.getTeam();
        short teamID = team == null ? 0 : team.getUID();

        if (player.getEntityData().getShort(ServerUtilitiesPlayerData.TAG_LAST_CHUNK) != teamID) {
            if (teamID == 0) {
                player.getEntityData().removeTag(ServerUtilitiesPlayerData.TAG_LAST_CHUNK);
            } else {
                player.getEntityData().setShort(ServerUtilitiesPlayerData.TAG_LAST_CHUNK, teamID);
            }

            if (team != null) {
                Notification notification = Notification.of(CHUNK_CHANGED, team.getTitle());

                if (!team.getDesc().isEmpty()) {
                    notification.addLine(StringUtils.italic(new ChatComponentText(team.getDesc()), true));
                }

                notification.send(player.mcServer, player);
            } else {
                Notification.of(
                        CHUNK_CHANGED,
                        StringUtils.color(
                                ServerUtilities.lang(player, "serverutilities.lang.chunks.wilderness"),
                                EnumChatFormatting.DARK_GREEN))
                        .send(player.mcServer, player);
            }
        }
    }

    public static void backupNotification(ResourceLocation id, String key, Object... args) {
        if (!ServerUtilitiesConfig.backups.silent_backup) {
            Notification
                    .of(id, StringUtils.color(ServerUtilities.lang(null, key, args), EnumChatFormatting.LIGHT_PURPLE))
                    .send(ServerUtils.getServer(), null);
        }
    }
}
