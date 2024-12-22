package serverutils.lib.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesCommon;
import serverutils.ServerUtilitiesConfig;
import serverutils.events.IReloadHandler;
import serverutils.events.ServerReloadEvent;
import serverutils.lib.EnumReloadType;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigNull;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.ConfigValueProvider;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;
import serverutils.net.MessageCloseGui;
import serverutils.net.MessageEditConfig;
import serverutils.net.MessageSyncData;

public class ServerUtilitiesAPI {

    public static void reloadServer(Universe universe, ICommandSender sender, EnumReloadType type,
            ResourceLocation id) {
        long ms = System.currentTimeMillis();
        universe.clearCache();

        HashSet<ResourceLocation> failed = new HashSet<>();
        ServerReloadEvent event = new ServerReloadEvent(universe, sender, type, id, failed);

        for (Map.Entry<ResourceLocation, IReloadHandler> entry : ServerUtilitiesCommon.RELOAD_IDS.entrySet()) {
            try {
                if (event.reload(entry.getKey()) && !entry.getValue().onReload(event)) {
                    event.failedToReload(entry.getKey());
                }
            } catch (Exception ex) {
                event.failedToReload(entry.getKey());

                if (ServerUtilitiesConfig.debugging.print_more_errors) {
                    ex.printStackTrace();
                }
            }
        }

        event.post();

        for (EntityPlayerMP player : universe.server.getConfigurationManager().playerEntityList) {
            ForgePlayer p = universe.getPlayer(player);
            new MessageSyncData(false, player, p).sendTo(player);
        }

        String millis = (System.currentTimeMillis() - ms) + "ms";

        if (type == EnumReloadType.RELOAD_COMMAND) {
            for (EntityPlayerMP player : universe.server.getConfigurationManager().playerEntityList) {
                Notification notification = Notification.of("reload_server");
                notification.addLine(ServerUtilities.lang(player, "serverutilities.lang.reload_server", millis));

                if (event.isClientReloadRequired()) {
                    notification.addLine(
                            ServerUtilities.lang(
                                    player,
                                    "serverutilities.lang.reload_client",
                                    StringUtils.color(new ChatComponentText("F3 + T"), EnumChatFormatting.GOLD)));
                }

                if (!failed.isEmpty()) {
                    notification.addLine(
                            StringUtils.color(
                                    ServerUtilities.lang(player, "serverutilities.lang.reload_failed"),
                                    EnumChatFormatting.RED));
                    ServerUtilities.LOGGER.warn("These IDs failed to reload:");

                    for (ResourceLocation f : failed) {
                        notification.addLine(
                                StringUtils.color(new ChatComponentText(f.toString()), EnumChatFormatting.RED));
                        ServerUtilities.LOGGER.warn("- " + f);
                    }
                }

                notification.setImportant(true);
                notification.setTimer(Ticks.SECOND.x(7));
                notification.send(player);
            }
        }
        reload(universe.server);
        // universe.server.reload();
        ServerUtilities.LOGGER.info("Reloaded server in " + millis);
    }

    public static void editServerConfig(EntityPlayerMP player, ConfigGroup group, IConfigCallback callback) {
        ServerUtilitiesCommon.TEMP_SERVER_CONFIG
                .put(player.getGameProfile().getId(), new ServerUtilitiesCommon.EditingConfig(group, callback));
        new MessageEditConfig(group).sendTo(player);
    }

    public static ConfigValue createConfigValueFromId(String id) {
        if (id.isEmpty()) {
            return ConfigNull.INSTANCE;
        }

        ConfigValueProvider provider = ServerUtilitiesCommon.CONFIG_VALUE_PROVIDERS.get(id);
        Objects.requireNonNull(provider, "Unknown Config ID: " + id);
        ConfigValue value = provider.get();
        return value == null || value.isNull() ? ConfigNull.INSTANCE : value;
    }

    public static void sendCloseGuiPacket(EntityPlayerMP player) {
        new MessageCloseGui().sendTo(player);
    }

    /**
     * Helper method for other mods so they don't have to deal with other classes than this
     */
    public static boolean arePlayersInSameTeam(UUID player1, UUID player2) {
        if (!Universe.loaded()) {
            return false;
        } else if (player1 == player2 || player1.equals(player2)) {
            return true;
        }

        ForgePlayer p1 = Universe.get().getPlayer(player1);

        if (p1 == null || !p1.hasTeam()) {
            return false;
        }

        ForgePlayer p2 = Universe.get().getPlayer(player2);
        return p2 != null && p2.hasTeam() && p1.team.equalsTeam(p2.team);
    }

    public static boolean isPlayerInTeam(UUID player, String team) {
        if (!Universe.loaded()) {
            return false;
        }

        ForgePlayer p = Universe.get().getPlayer(player);

        if (p == null) {
            return false;
        }

        return p.hasTeam() ? p.team.getId().equals(team) : team.isEmpty();
    }

    public static boolean isPlayerInTeam(UUID player, int team) {
        if (!Universe.loaded()) {
            return false;
        }

        ForgePlayer p = Universe.get().getPlayer(player);

        if (p == null) {
            return false;
        }

        return p.hasTeam() ? p.team.getUID() == team : team == 0;
    }

    public static String getTeam(UUID player) {
        if (!Universe.loaded()) {
            return "";
        }

        ForgePlayer p = Universe.get().getPlayer(player);
        return p == null ? "" : p.team.getId();
    }

    public static short getTeamID(UUID player) {
        if (!Universe.loaded()) {
            return 0;
        }

        ForgePlayer p = Universe.get().getPlayer(player);
        return p == null ? 0 : p.team.getUID();
    }

    public static void reload(MinecraftServer server) {
        // TODO: check if this is important
        if (Thread.currentThread().getName().equals("Server thread")) {
            server.getConfigurationManager().saveAllPlayerData();
            // server.worldServers[0].getLootTableManager().reloadLootTables();
            // server.getAdvancementManager().reload();
            // server.getFunctionManager().reload();
            // server.getConfigurationManager().reloadResources();
        } else {
            // server.addScheduledTask((s) -> reload(s));
        }

    }

}
