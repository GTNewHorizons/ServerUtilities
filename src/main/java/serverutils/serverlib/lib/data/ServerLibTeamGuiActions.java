package serverutils.serverlib.lib.data;

import net.minecraft.event.ClickEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.EnumTeamStatus;
import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.net.MessageMyTeamPlayerList;

import java.util.function.Predicate;

public class ServerLibTeamGuiActions
{
	private static final Predicate<EnumTeamStatus> NO_ENEMIES_PREDICATE = status -> status != EnumTeamStatus.ENEMY;
	private static final Predicate<EnumTeamStatus> MEMBERS_PREDICATE = status -> status.isEqualOrGreaterThan(EnumTeamStatus.MEMBER);
	private static final Predicate<EnumTeamStatus> ALLIES_PREDICATE = MEMBERS_PREDICATE.negate().and(NO_ENEMIES_PREDICATE);
	private static final Predicate<EnumTeamStatus> ENEMIES_PREDICATE = status -> status == EnumTeamStatus.ENEMY || status == EnumTeamStatus.NONE;

	public static final TeamAction CONFIG = new TeamAction(ServerLib.MOD_ID, "config", GuiIcons.SETTINGS, -100)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return player.team.isModerator(player) ? Type.ENABLED : Type.DISABLED;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			ServerLibAPI.editServerConfig(player.getPlayer(), player.team.getSettings(), player.team);
		}
	}.setTitle(new ChatComponentTranslation("gui.settings"));

	public static final TeamAction INFO = new TeamAction(ServerLib.MOD_ID, "info", GuiIcons.INFO, 0)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return Type.INVISIBLE;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			//TODO: Open info gui
		}
	}.setTitle(new ChatComponentTranslation("gui.info"));

	public static final TeamAction MEMBERS = new TeamAction(ServerLib.MOD_ID, "members", GuiIcons.FRIENDS, 30)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return (player.team.isModerator(player) && player.team.universe.getPlayers().size() > 1) ? Type.ENABLED : Type.DISABLED;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			if (data.isEmpty())
			{
				new MessageMyTeamPlayerList(getId(), player, NO_ENEMIES_PREDICATE).sendTo(player.getPlayer());
				return;
			}

			ForgePlayer p = player.team.universe.getPlayer(data.getString("player"));

			if (p == null || p == player)
			{
				return;
			}

			switch (data.getString("action"))
			{
				case "kick":
				{
					if (player.team.isMember(p))
					{
						player.team.removeMember(p);
						player.team.setRequestingInvite(p, true);
					}

					break;
				}
				case "invite":
				{
					player.team.setStatus(p, EnumTeamStatus.INVITED);

					if (player.team.isRequestingInvite(p))
					{
						if (p.hasTeam())
						{
							player.team.setRequestingInvite(p, false);
						}
						else
						{
							player.team.addMember(p, false);
						}
					}
					else if (p.isOnline())
					{
						IChatComponent component = new ChatComponentTranslation("serverlib.lang.team.invited_you", player.team, player.getDisplayName());
						component.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team join " + player.team.getId()));
						p.getPlayer().addChatMessage(component);
					}

					break;
				}
				case "cancel_invite":
				{
					if (player.team.getHighestStatus(p) == EnumTeamStatus.INVITED)
					{
						player.team.setStatus(p, EnumTeamStatus.NONE);
					}

					break;
				}
				case "deny_request":
				{
					player.team.setRequestingInvite(p, false);
					break;
				}
			}
		}
	};

	public static final TeamAction ALLIES = new TeamAction(ServerLib.MOD_ID, "allies", GuiIcons.STAR, 40)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return (player.team.isModerator(player) && player.team.universe.getPlayers().size() > 1) ? Type.ENABLED : Type.DISABLED;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			if (data.isEmpty())
			{
				new MessageMyTeamPlayerList(getId(), player, ALLIES_PREDICATE).sendTo(player.getPlayer());
			}

			ForgePlayer p = player.team.universe.getPlayer(data.getString("player"));

			if (p != null && p != player)
			{
				player.team.setStatus(p, data.getBoolean("add") ? EnumTeamStatus.ALLY : EnumTeamStatus.NONE);
			}
		}
	};

	public static final TeamAction MODERATORS = new TeamAction(ServerLib.MOD_ID, "moderators", GuiIcons.SHIELD, 50)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return (player.team.isOwner(player) && player.team.getMembers().size() > 1) ? Type.ENABLED : Type.DISABLED;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			if (data.isEmpty())
			{
				new MessageMyTeamPlayerList(getId(), player, MEMBERS_PREDICATE).sendTo(player.getPlayer());
				return;
			}

			ForgePlayer p = player.team.universe.getPlayer(data.getString("player"));

			if (p != null && p != player)
			{
				player.team.setStatus(p, data.getBoolean("add") ? EnumTeamStatus.MOD : EnumTeamStatus.NONE);
			}
		}
	};

	public static final TeamAction ENEMIES = new TeamAction(ServerLib.MOD_ID, "enemies", GuiIcons.CLOSE, 60)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return (player.team.isModerator(player) && player.team.universe.getPlayers().size() > 1) ? Type.ENABLED : Type.DISABLED;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			if (data.isEmpty())
			{
				new MessageMyTeamPlayerList(getId(), player, ENEMIES_PREDICATE).sendTo(player.getPlayer());
			}

			ForgePlayer p = player.team.universe.getPlayer(data.getString("player"));

			if (p != null && p != player)
			{
				player.team.setStatus(p, data.getBoolean("add") ? EnumTeamStatus.ENEMY : EnumTeamStatus.NONE);
			}
		}
	};

	public static final TeamAction LEAVE = new TeamAction(ServerLib.MOD_ID, "leave", GuiIcons.REMOVE, 10000)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return (!player.team.isOwner(player) || player.team.getMembers().size() <= 1) ? Type.ENABLED : Type.INVISIBLE;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			player.team.removeMember(player);
			ServerLibAPI.sendCloseGuiPacket(player.getPlayer());
		}
	}.setRequiresConfirm();

	public static final TeamAction TRANSFER_OWNERSHIP = new TeamAction(ServerLib.MOD_ID, "transfer_ownership", GuiIcons.RIGHT, 10000)
	{
		@Override
		public Type getType(ForgePlayer player, NBTTagCompound data)
		{
			return (!player.team.isOwner(player) || player.team.getMembers().size() <= 1) ? Type.INVISIBLE : Type.ENABLED;
		}

		@Override
		public void onAction(ForgePlayer player, NBTTagCompound data)
		{
			if (data.isEmpty())
			{
				new MessageMyTeamPlayerList(getId(), player, MEMBERS_PREDICATE).sendTo(player.getPlayer());
			}

			ForgePlayer p = player.team.universe.getPlayer(data.getString("player"));

			if (p != null && p != player)
			{
				player.team.setStatus(p, EnumTeamStatus.OWNER);
			}
		}
	};
}