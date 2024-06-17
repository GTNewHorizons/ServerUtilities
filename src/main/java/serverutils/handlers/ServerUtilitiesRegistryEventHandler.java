package serverutils.handlers;

import static serverutils.ServerUtilitiesPermissions.RANK_EDIT;

import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameRules;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.events.ServerUtilitiesPreInitRegistryEvent;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.data.AdminPanelAction;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.ItemIcon;
import serverutils.lib.util.StringUtils;
import serverutils.net.MessageRanks;
import serverutils.net.MessageViewCrashList;
import serverutils.ranks.Ranks;

public class ServerUtilitiesRegistryEventHandler {

    public static final ServerUtilitiesRegistryEventHandler INST = new ServerUtilitiesRegistryEventHandler();

    @SubscribeEvent
    public void onServerUtilitiesPreInitRegistry(ServerUtilitiesPreInitRegistryEvent event) {
        ServerUtilitiesPreInitRegistryEvent.Registry registry = event.getRegistry();
        registry.registerServerReloadHandler(
                new ResourceLocation(ServerUtilities.MOD_ID, "ranks"),
                reloadEvent -> Ranks.INSTANCE.reload());

        registry.registerSyncData(ServerUtilities.MOD_ID, new ServerUtilitiesSyncData());

        registry.registerAdminPanelAction(
                new AdminPanelAction(ServerUtilities.MOD_ID, "crash_reports", ItemIcon.getItemIcon(Items.paper), 0) {

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

        registry.registerAdminPanelAction(
                new AdminPanelAction(ServerUtilities.MOD_ID, "edit_world", GuiIcons.GLOBE, 0) {

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
                                                .setDisplayName(
                                                        new ChatComponentText(StringUtils.camelCaseToWords(key)));
                                    } catch (NumberFormatException ignored) {
                                        gamerules
                                                .addString(
                                                        key,
                                                        () -> rules.getGameRuleStringValue(key),
                                                        v -> rules.setOrCreateGameRule(key, v),
                                                        "")
                                                .setDisplayName(
                                                        new ChatComponentText(StringUtils.camelCaseToWords(key)));
                                    }
                                }
                            }
                        }

                        ServerUtilitiesAPI.editServerConfig(player.getPlayer(), main, IConfigCallback.DEFAULT);
                    }

                });
        registry.registerAdminPanelAction(
                new AdminPanelAction(ServerUtilities.MOD_ID, "edit_rank", ItemIcon.getItemIcon(Items.book), 0) {

                    @Override
                    public Type getType(ForgePlayer player, NBTTagCompound data) {
                        return Type.fromBoolean(ServerUtilitiesConfig.ranks.enabled && player.hasPermission(RANK_EDIT));
                    }

                    @Override
                    public void onAction(ForgePlayer player, NBTTagCompound data) {
                        new MessageRanks(Ranks.INSTANCE, player).sendTo(player.getPlayer());
                    }
                });
    }
}
