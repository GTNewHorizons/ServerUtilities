package serverutils.utils.handlers;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ServerChatEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.lib.events.universe.UniverseClearCacheEvent;
import serverutils.lib.lib.EnumMessageLocation;
import serverutils.lib.lib.config.ConfigEnum;
import serverutils.lib.lib.config.RankConfigAPI;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.util.NBTUtils;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.permission.PermissionAPI;
import serverutils.lib.lib.util.text_components.Notification;
import serverutils.lib.lib.util.text_components.TextComponentParser;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesCommon;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.command.CmdShutdown;
import serverutils.utils.data.ClaimedChunks;
import serverutils.utils.data.ServerUtilitiesPlayerData;
import serverutils.utils.data.ServerUtilitiesUniverseData;
import serverutils.utils.net.MessageUpdatePlayTime;
import serverutils.utils.ranks.Ranks;

public class ServerUtilitiesServerEventHandler {

    public static final ServerUtilitiesServerEventHandler INST = new ServerUtilitiesServerEventHandler();
    private static final ResourceLocation AFK_ID = new ResourceLocation(ServerUtilities.MOD_ID, "afk");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("\\~\\~(.*?)\\~\\~");
    private static final String STRIKETHROUGH_REPLACE = "&m$1&m";
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*|__(.*?)__");
    private static final String BOLD_REPLACE = "&l$1$2&l";
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*|_(.*?)_");
    private static final String ITALIC_REPLACE = "&o$1$2&o";

    @SubscribeEvent
    public void onCacheCleared(UniverseClearCacheEvent event) {
        if (Ranks.INSTANCE != null) {
            Ranks.INSTANCE.clearCache();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerChatEvent(ServerChatEvent event) {
        if (!ServerUtilitiesConfig.ranks.override_chat || !Ranks.isActive()) {
            return;
        }

        EntityPlayerMP player = event.player;

        if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CHAT_SPEAK)
                || NBTUtils.getPersistedData(player, false).getBoolean(ServerUtilitiesPlayerData.TAG_MUTED)) {
            player.addChatComponentMessage(
                    StringUtils.color(ServerUtilities.lang(player, "commands.mute.muted"), EnumChatFormatting.RED));
            event.setCanceled(true);
            return;
        }

        IChatComponent main = new ChatComponentText("");
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(Universe.get().getPlayer(player));
        main.appendSibling(data.getNameForChat(player));

        String message = event.message.trim();

        boolean b = false;

        if (!message.contains("https://") && !message.contains("http://")
                && PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CHAT_FORMATTING)) {
            for (Map.Entry<String, String> entry : ServerUtilitiesCommon.KAOMOJIS.entrySet()) {
                message = message.replace(entry.getValue(), "<emoji:" + entry.getKey() + ">");
            }

            b = !message.equals(message = STRIKETHROUGH_PATTERN.matcher(message).replaceAll(STRIKETHROUGH_REPLACE)) | b;
            b = !message.equals(message = BOLD_PATTERN.matcher(message).replaceAll(BOLD_REPLACE)) | b;
            b = !message.equals(message = ITALIC_PATTERN.matcher(message).replaceAll(ITALIC_REPLACE)) | b;

            for (Map.Entry<String, String> entry : ServerUtilitiesCommon.KAOMOJIS.entrySet()) {
                message = message.replace("<emoji:" + entry.getKey() + ">", entry.getValue());
            }
        }

        IChatComponent text;

        if (b) {
            text = TextComponentParser.parse(message, null);
        } else {
            text = ForgeHooks.newChatWithLinks(message);
        }

        EnumChatFormatting colortf = (EnumChatFormatting) ((ConfigEnum) RankConfigAPI
                .get(player, ServerUtilitiesPermissions.CHAT_TEXT_COLOR)).getValue();

        if (colortf != EnumChatFormatting.WHITE) {
            text.getChatStyle().setColor(colortf);
        }

        if (Ranks.INSTANCE.getPermissionResult(player, ServerUtilitiesPermissions.CHAT_TEXT_BOLD, false)
                == Event.Result.ALLOW) {
            text.getChatStyle().setBold(true);
        }

        if (Ranks.INSTANCE.getPermissionResult(player, ServerUtilitiesPermissions.CHAT_TEXT_ITALIC, false)
                == Event.Result.ALLOW) {
            text.getChatStyle().setItalic(true);
        }

        if (Ranks.INSTANCE.getPermissionResult(player, ServerUtilitiesPermissions.CHAT_TEXT_UNDERLINED, false)
                == Event.Result.ALLOW) {
            text.getChatStyle().setUnderlined(true);
        }

        if (Ranks.INSTANCE.getPermissionResult(player, ServerUtilitiesPermissions.CHAT_TEXT_STRIKETHROUGH, false)
                == Event.Result.ALLOW) {
            text.getChatStyle().setStrikethrough(true);
        }

        if (Ranks.INSTANCE.getPermissionResult(player, ServerUtilitiesPermissions.CHAT_TEXT_OBFUSCATED, false)
                == Event.Result.ALLOW) {
            text.getChatStyle().setObfuscated(true);
        }

        main.appendSibling(text);
        // event.component.appendSibling(main);
        event.component = new ChatComponentTranslation("translation.test.args", data.getNameForChat(player), text);
        // event.setComponent(main);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!Universe.loaded()) {
            return;
        }

        Universe universe = Universe.get();

        long now = System.currentTimeMillis();

        if (event.phase == TickEvent.Phase.START) {
            if (ClaimedChunks.isActive()) {
                ClaimedChunks.instance.update(universe, now);
            }
        } else {
            EntityPlayerMP playerToKickForAfk = null; // Do one at time, easier
            boolean afkEnabled = ServerUtilitiesConfig.afk.isEnabled(universe.server);

            for (EntityPlayerMP player : (List<EntityPlayerMP>) universe.server
                    .getConfigurationManager().playerEntityList) {
                if (ServerUtils.isFake(player)) {
                    continue;
                }

                boolean fly = player.capabilities.allowFlying;

                if (!player.capabilities.isCreativeMode
                        && NBTUtils.getPersistedData(player, false).getBoolean(ServerUtilitiesPlayerData.TAG_FLY)) {
                    player.capabilities.allowFlying = true;
                }

                if (fly != player.capabilities.allowFlying) {
                    player.sendPlayerAbilities();
                }

                if (afkEnabled) {
                    ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(universe.getPlayer(player));
                    boolean prevIsAfk = data.afkTime >= ServerUtilitiesConfig.afk.getNotificationTimer();
                    data.afkTime = System.currentTimeMillis() - player.func_154331_x();
                    boolean isAFK = data.afkTime >= ServerUtilitiesConfig.afk.getNotificationTimer();

                    if (prevIsAfk != isAFK) {
                        for (EntityPlayerMP player1 : (List<EntityPlayerMP>) universe.server
                                .getConfigurationManager().playerEntityList) {
                            EnumMessageLocation location = ServerUtilitiesPlayerData.get(universe.getPlayer(player1))
                                    .getAFKMessageLocation();

                            if (location != EnumMessageLocation.OFF) {
                                IChatComponent component = ServerUtilities.lang(
                                        player1,
                                        isAFK ? "permission.serverutilities.afk.timer.is_afk"
                                                : "permission.serverutilities.afk.timer.isnt_afk",
                                        player.getDisplayName());
                                component.getChatStyle().setColor(EnumChatFormatting.GRAY);

                                if (location == EnumMessageLocation.CHAT) {
                                    player1.addChatMessage(component);
                                } else {
                                    Notification.of(AFK_ID, component).send(universe.server, player1);
                                }
                            }
                        }

                        ServerUtilities.LOGGER
                                .info(player.getDisplayName() + (isAFK ? " is now AFK" : " is no longer AFK"));

                        // if (ServerUtilitiesConfig.chat.replace_tab_names) {
                        // new MessageUpdateTabName(player).sendToAll();
                        // }
                    }

                    if (playerToKickForAfk == null) {
                        long maxTime = RankConfigAPI
                                .get(player.mcServer, player.getGameProfile(), ServerUtilitiesPermissions.AFK_TIMER)
                                .getTimer().millis();

                        if (maxTime > 0L && data.afkTime >= maxTime) {
                            playerToKickForAfk = player;
                        }
                    }
                }
            }

            if (playerToKickForAfk != null && playerToKickForAfk.playerNetServerHandler != null) {
                playerToKickForAfk.playerNetServerHandler
                        .onDisconnect(new ChatComponentTranslation("multiplayer.disconnect.idling"));
            }

            if (ServerUtilitiesUniverseData.shutdownTime > 0L && ServerUtilitiesUniverseData.shutdownTime - now <= 0) {
                CmdShutdown.shutdown(universe.server);
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isRemote && event.phase == TickEvent.Phase.START
                && event.world.provider.dimensionId == ServerUtilitiesConfig.world.spawn_dimension) {
            if (ServerUtilitiesConfig.world.forced_spawn_dimension_time != -1) {
                event.world.setWorldTime(ServerUtilitiesConfig.world.forced_spawn_dimension_time);
            }

            if (ServerUtilitiesConfig.world.forced_spawn_dimension_weather != -1) {
                event.world.getWorldInfo().setRaining(ServerUtilitiesConfig.world.forced_spawn_dimension_weather >= 1);
                event.world.getWorldInfo()
                        .setThundering(ServerUtilitiesConfig.world.forced_spawn_dimension_weather >= 2);
            }

            if (ServerUtilitiesConfig.world.show_playtime && event.world.getTotalWorldTime() % 20L == 7L) {
                for (EntityPlayerMP player : (List<EntityPlayerMP>) event.world.playerEntities) {
                    new MessageUpdatePlayTime(player.func_147099_x().writeStat(StatList.minutesPlayedStat))
                            .sendTo(player);
                }
            }
        }
    }
}
