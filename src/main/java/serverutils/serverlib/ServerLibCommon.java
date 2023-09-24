package serverutils.serverlib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;										 
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import serverutils.serverlib.events.ServerLibPreInitRegistryEvent;
import serverutils.serverlib.events.IReloadHandler;
import serverutils.serverlib.events.ServerReloadEvent;
import serverutils.serverlib.lib.EnumReloadType;
import serverutils.serverlib.lib.config.ConfigBlockState;
import serverutils.serverlib.lib.config.ConfigBoolean;
import serverutils.serverlib.lib.config.ConfigColor;
import serverutils.serverlib.lib.config.ConfigDouble;
import serverutils.serverlib.lib.config.ConfigEnum;
import serverutils.serverlib.lib.config.ConfigFluid;
import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.config.ConfigInt;
import serverutils.serverlib.lib.config.ConfigItemStack;
import serverutils.serverlib.lib.config.ConfigList;
import serverutils.serverlib.lib.config.ConfigLong;
import serverutils.serverlib.lib.config.ConfigNBT;
import serverutils.serverlib.lib.config.ConfigNull;
import serverutils.serverlib.lib.config.ConfigString;
import serverutils.serverlib.lib.config.ConfigStringEnum;
import serverutils.serverlib.lib.config.ConfigTeam;
import serverutils.serverlib.lib.config.ConfigTeamClient;
import serverutils.serverlib.lib.config.ConfigTextComponent;
import serverutils.serverlib.lib.config.ConfigTimer;
import serverutils.serverlib.lib.config.ConfigValueProvider;
import serverutils.serverlib.lib.config.IConfigCallback;
import serverutils.serverlib.lib.config.RankConfigAPI;
import serverutils.serverlib.lib.data.AdminPanelAction;
import serverutils.serverlib.lib.data.ServerLibAPI;
import serverutils.serverlib.lib.data.ServerLibTeamGuiActions;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ISyncData;
import serverutils.serverlib.lib.data.TeamAction;
import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.math.Ticks;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.util.BlockUtils;
import serverutils.serverlib.net.ServerLibNetHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ServerLibCommon {
	public static final Map<String, ConfigValueProvider> CONFIG_VALUE_PROVIDERS = new HashMap<>();
	public static final Map<UUID, EditingConfig> TEMP_SERVER_CONFIG = new HashMap<>();
	public static final Map<String, ISyncData> SYNCED_DATA = new HashMap<>();
	public static final HashMap<ResourceLocation, IReloadHandler> RELOAD_IDS = new HashMap<>();
	public static final Map<ResourceLocation, TeamAction> TEAM_GUI_ACTIONS = new HashMap<>();
	public static final Map<ResourceLocation, AdminPanelAction> ADMIN_PANEL_ACTIONS = new HashMap<>();
	private static final Map<String, Function<ForgePlayer, IChatComponent>> CHAT_FORMATTING_SUBSTITUTES = new HashMap<>();

	public static Function<String, IChatComponent> chatFormattingSubstituteFunction(ForgePlayer player) {
		return s -> {
			Function<ForgePlayer, IChatComponent> sub = CHAT_FORMATTING_SUBSTITUTES.get(s);
			return sub == null ? null : sub.apply(player);
		};
	}

	public static class EditingConfig {
		public final ConfigGroup group;
		public final IConfigCallback callback;

		public EditingConfig(ConfigGroup g, IConfigCallback c) {
			group = g;
			callback = c;
		}
	}

	public void preInit(FMLPreInitializationEvent event) {
		if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
			ServerLib.LOGGER.info("Loading ServerLib in development environment");
		}
	}

	public void init(FMLInitializationEvent event) {
		ServerLibNetHandler.init();

		ServerLibPreInitRegistryEvent.Registry registry = new ServerLibPreInitRegistryEvent.Registry() {
			@Override
			public void registerConfigValueProvider(String id, ConfigValueProvider provider) {
				CONFIG_VALUE_PROVIDERS.put(id, provider);
			}

			@Override
			public void registerSyncData(String mod, ISyncData data)
			{
				SYNCED_DATA.put(mod, data);
			}

			@Override
			public void registerServerReloadHandler(ResourceLocation id, IReloadHandler handler)
			{
				RELOAD_IDS.put(id, handler);
			}

			@Override
			public void registerAdminPanelAction(AdminPanelAction action)
			{
				ADMIN_PANEL_ACTIONS.put(action.getId(), action);
			}

			@Override
			public void registerTeamAction(TeamAction action)
			{
				TEAM_GUI_ACTIONS.put(action.getId(), action);
			}
		};

		registry.registerConfigValueProvider(ConfigNull.ID, () -> ConfigNull.INSTANCE);
		registry.registerConfigValueProvider(ConfigList.ID, () -> new ConfigList<>(ConfigNull.INSTANCE));
		registry.registerConfigValueProvider(ConfigBoolean.ID, () -> new ConfigBoolean(false));
		registry.registerConfigValueProvider(ConfigInt.ID, () -> new ConfigInt(0));
		registry.registerConfigValueProvider(ConfigDouble.ID, () -> new ConfigDouble(0D));
		registry.registerConfigValueProvider(ConfigLong.ID, () -> new ConfigLong(0L));
		registry.registerConfigValueProvider(ConfigString.ID, () -> new ConfigString(""));
		registry.registerConfigValueProvider(ConfigColor.ID, () -> new ConfigColor(Color4I.WHITE));
		registry.registerConfigValueProvider(ConfigEnum.ID, () -> new ConfigStringEnum(Collections.emptyList(), ""));
		//registry.registerConfigValueProvider(ConfigBlockState.ID, () -> new ConfigBlockState(BlockUtils.AIR_STATE));
		registry.registerConfigValueProvider(ConfigItemStack.ID, () -> new ConfigItemStack(InvUtils.EMPTY_STACK));
		registry.registerConfigValueProvider(ConfigTextComponent.ID, () -> new ConfigTextComponent(new ChatComponentText("")));
		registry.registerConfigValueProvider(ConfigTimer.ID, () -> new ConfigTimer(Ticks.NO_TICKS));
		registry.registerConfigValueProvider(ConfigNBT.ID, () -> new ConfigNBT(null));
		registry.registerConfigValueProvider(ConfigFluid.ID, () -> new ConfigFluid(null, null));
		registry.registerConfigValueProvider(ConfigTeam.TEAM_ID, () -> new ConfigTeamClient(""));

		registry.registerAdminPanelAction(new AdminPanelAction(ServerLib.MOD_ID, "reload", GuiIcons.REFRESH, -1000) {
			@Override
			public Type getType(ForgePlayer player, NBTTagCompound data) {
				return Type.fromBoolean(player.isOP());
			}

			@Override
			public void onAction(ForgePlayer player, NBTTagCompound data) {
				ServerLibAPI.reloadServer(player.team.universe, player.getPlayer(), EnumReloadType.RELOAD_COMMAND, ServerReloadEvent.ALL);
			}
		}.setTitle(new ChatComponentTranslation("serverlib.lang.reload_server_button")));

		registry.registerTeamAction(ServerLibTeamGuiActions.CONFIG);
		registry.registerTeamAction(ServerLibTeamGuiActions.INFO);
		registry.registerTeamAction(ServerLibTeamGuiActions.MEMBERS);
		registry.registerTeamAction(ServerLibTeamGuiActions.ALLIES);
		registry.registerTeamAction(ServerLibTeamGuiActions.MODERATORS);
		registry.registerTeamAction(ServerLibTeamGuiActions.ENEMIES);
		registry.registerTeamAction(ServerLibTeamGuiActions.LEAVE);
		registry.registerTeamAction(ServerLibTeamGuiActions.TRANSFER_OWNERSHIP);

		new ServerLibPreInitRegistryEvent(registry).post();

		RankConfigAPI.getHandler();

		CHAT_FORMATTING_SUBSTITUTES.put("name", ForgePlayer::getDisplayName);
		CHAT_FORMATTING_SUBSTITUTES.put("team", player -> player.team.getTitle());
		//CHAT_FORMATTING_SUBSTITUTES.put("tag", player -> new ChatComponentText(player.getTag())); //TODO
	}

	public void postInit()
	{
	}

	/*
	public void reloadConfig(LoaderState.ModState state)
	{
		JsonElement overridesE = JsonUtils.fromJson(new File(CommonUtils.folderConfig, "config_overrides.json"));

		if (overridesE.isJsonObject())
		{
		}
	}*/

	public void handleClientMessage(MessageToClient message)
	{
	}

	public void spawnDust(World world, double x, double y, double z, float r, float g, float b, float a)
	{
	}

	public void spawnDust(World world, double x, double y, double z, Color4I col)
	{
		spawnDust(world, x, y, z, col.redf(), col.greenf(), col.bluef(), col.alphaf());
	}

	public long getWorldTime()
	{
		return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getTotalWorldTime(); //getWorld(0).getTotalWorldTime();
	}
}