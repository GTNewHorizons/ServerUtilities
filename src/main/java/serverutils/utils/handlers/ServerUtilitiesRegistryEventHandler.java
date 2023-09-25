package serverutils.utils.handlers;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameRules;

import serverutils.lib.events.ServerUtilitiesLibPreInitRegistryEvent;
import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.config.IConfigCallback;
import serverutils.lib.lib.data.AdminPanelAction;
import serverutils.lib.lib.data.ServerUtilitiesLibAPI;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.gui.GuiIcons;
import serverutils.lib.lib.icon.ItemIcon;
import serverutils.lib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ServerUtilitiesUniverseData;
import serverutils.utils.net.MessageViewCrashList;
import serverutils.utils.ranks.Ranks;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ServerUtilitiesRegistryEventHandler {

    public static final ServerUtilitiesRegistryEventHandler INST = new ServerUtilitiesRegistryEventHandler();

    @SubscribeEvent
    public void onServerUtilitiesLibPreInitRegistry(ServerUtilitiesLibPreInitRegistryEvent event) {
        ServerUtilitiesLibPreInitRegistryEvent.Registry registry = event.getRegistry();
        registry.registerServerReloadHandler(
                new ResourceLocation(ServerUtilities.MOD_ID, "ranks"),
                reloadEvent -> Ranks.INSTANCE.reload());
        registry.registerServerReloadHandler(
                new ResourceLocation(ServerUtilities.MOD_ID, "badges"),
                reloadEvent -> ServerUtilitiesUniverseData.clearBadgeCache());

        registry.registerSyncData(ServerUtilities.MOD_ID, new ServerUtilitiesSyncData());

        registry.registerAdminPanelAction(
                new AdminPanelAction(
                        ServerUtilities.MOD_ID,
                        "crash_reports",
                        ItemIcon.getItemIcon(Item.getItemById(339)),
                        0) {

                    @Override
                    public Type getType(ForgePlayer player, NBTTagCompound data) {
                        return Type.fromBoolean(player.hasPermission(ServerUtilitiesPermissions.CRASH_REPORTS_VIEW));
                    }

                    @Override
                    public void onAction(ForgePlayer player, NBTTagCompound data) {
                        new MessageViewCrashList(player.team.universe.server.getFile("crash-reports"))
                                .sendTo(player.getPlayer());
                    }
                });

        registry.registerAdminPanelAction(new AdminPanelAction(ServerUtilities.MOD_ID, "edit_world", GuiIcons.GLOBE, 0) {

            @Override
            public Type getType(ForgePlayer player, NBTTagCompound data) {
                return Type.fromBoolean(player.hasPermission(ServerUtilitiesPermissions.EDIT_WORLD_GAMERULES));
            }

            @Override
            public void onAction(ForgePlayer player, NBTTagCompound data) {
                ConfigGroup main = ConfigGroup.newGroup("edit_world");
                main.setDisplayName(new ChatComponentTranslation("admin_panel.serverutilities.edit_world"));

                if (player.hasPermission(ServerUtilitiesPermissions.EDIT_WORLD_GAMERULES)) {
                    ConfigGroup gamerules = main.getGroup("gamerules");
                    gamerules.setDisplayName(new ChatComponentTranslation("gamerules"));

                    GameRules rules = player.team.universe.world.getGameRules();

                    for (String key : rules.getRules()) {
                        String value = rules.getGameRuleStringValue(key);

                        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                            gamerules
                                    .addBool(
                                            key,
                                            () -> rules.getGameRuleBooleanValue(key),
                                            v -> rules.setOrCreateGameRule(key, Boolean.toString(v)),
                                            false)
                                    .setDisplayName(new ChatComponentText(StringUtils.camelCaseToWords(key)));
                        } else {
                            try {
                                Integer.parseInt(value);
                                gamerules
                                        .addInt(
                                                key,
                                                () -> Integer.parseInt(rules.getGameRuleStringValue(key)),
                                                v -> rules.setOrCreateGameRule(key, Integer.toString(v)),
                                                1,
                                                1,
                                                Integer.MAX_VALUE)
                                        .setDisplayName(new ChatComponentText(StringUtils.camelCaseToWords(key)));
                            } catch (NumberFormatException ignored) {
                                gamerules
                                        .addString(
                                                key,
                                                () -> rules.getGameRuleStringValue(key),
                                                v -> rules.setOrCreateGameRule(key, v),
                                                "")
                                        .setDisplayName(new ChatComponentText(StringUtils.camelCaseToWords(key)));
                            }
                        }
                    }
                }

                ServerUtilitiesLibAPI.editServerConfig(player.getPlayer(), main, IConfigCallback.DEFAULT);
            }

        });
    }
}
