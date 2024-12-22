package serverutils;

import static serverutils.lib.EnumMessageLocation.ACTION_BAR;
import static serverutils.lib.EnumMessageLocation.CHAT;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.EnumMessageLocation;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;

public enum ServerUtilitiesNotifications {

    CHUNK_MODIFIED("chunk_modified", ACTION_BAR),
    CHUNK_CHANGED("chunk_changed", ACTION_BAR),
    CANT_MODIFY_CHUNK("cant_modify_chunk", ACTION_BAR),
    TELEPORT("teleport", ACTION_BAR),
    TELEPORT_WARMUP("teleport_warmup", ACTION_BAR),
    BACKUP("backup", ACTION_BAR),
    CONFIG_CHANGED("config_changed", ACTION_BAR),
    RESTART_TIMER("restart_timer", ACTION_BAR),
    CLEANUP("cleanup", ACTION_BAR),
    PLAYER_AFK("player_afk", CHAT);

    public static final ServerUtilitiesNotifications[] VALUES = values();

    private final String id;
    private final String desc;
    private EnumMessageLocation location;

    ServerUtilitiesNotifications(String id, EnumMessageLocation defaultLocation) {
        this.id = id;
        this.desc = StatCollector.translateToLocal(ServerUtilities.MOD_ID + ".notifications." + id + ".desc");
        this.location = defaultLocation;
    }

    public Notification createNotification(IChatComponent component) {
        return Notification.of(id, component);
    }

    public Notification createNotification(String key, Object... args) {
        return createNotification(ServerUtilities.lang(key, args));
    }

    public void send(EntityPlayer player, String key, Object... args) {
        createNotification(key, args).send(player);
    }

    public void send(EntityPlayer player, IChatComponent component) {
        createNotification(component).send(player);
    }

    public void sendAll(String key, Object... args) {
        createNotification(key, args).sendToAll();
    }

    public void sendAll(IChatComponent component) {
        createNotification(component).sendToAll();
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
                Notification notification = CHUNK_CHANGED.createNotification(team.getTitle());

                if (!team.getDesc().isEmpty()) {
                    notification.addLine(StringUtils.italic(new ChatComponentText(team.getDesc()), true));
                }

                notification.send(player);
            } else {
                CHUNK_CHANGED.send(
                        player,
                        StringUtils.color("serverutilities.lang.chunks.wilderness", EnumChatFormatting.DARK_GREEN));
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public void setLocation(EnumMessageLocation enabled) {
        this.location = enabled;
    }

    public EnumMessageLocation getLocation() {
        return location;
    }

    public static @Nullable ServerUtilitiesNotifications getFromId(ResourceLocation id) {
        for (ServerUtilitiesNotifications n : VALUES) {
            if (n.id.equalsIgnoreCase(id.getResourcePath())) {
                return n;
            }
        }
        return null;
    }
}
