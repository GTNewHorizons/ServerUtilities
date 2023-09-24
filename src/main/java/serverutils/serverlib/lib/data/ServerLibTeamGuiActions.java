package serverutils.serverlib.lib.data;

import serverutils.serverlib.FTBLib;
import serverutils.serverlib.lib.EnumTeamStatus;
import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.net.MessageMyTeamPlayerList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;

import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class ServerLibTeamGuiActions
{
	private static final Predicate<EnumTeamStatus> NO_ENEMIES_PREDICATE = status -> status != EnumTeamStatus.ENEMY;
	private static final Predicate<EnumTeamStatus> MEMBERS_PREDICATE = status -> status.isEqualOrGreaterThan(EnumTeamStatus.MEMBER);
	private static final Predicate<EnumTeamStatus> ALLIES_PREDICATE = MEMBERS_PREDICATE.negate().and(NO_ENEMIES_PREDICATE);
	private static final Predicate<EnumTeamStatus> ENEMIES_PREDICATE = status -> status == EnumTeamStatus.ENEMY || status == EnumTeamStatus.NONE;

	public static final TeamAction CONFIG = new TeamAction(FTBLib.MOD_ID, "config", GuiIcons.SETTINGS, -100)
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
	}.setTitle(new TextComponentTranslation("gui.settings"));

	public static final TeamAction INFO = new TeamAction(FTBLib.MOD_ID, "info", GuiIcons.INFO, 0)
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
	}.setTitle(new TextComponentTranslation("gui.info"));

	public static final TeamAction MEMBERS = new TeamAction(FTBLib.MOD_ID, "members", GuiIcons.FRIENDS, 30)
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
						ITextComponent component = new TextComponentTranslation("ftblib.lang.team.invited_you", player.team, player.getDisplayName());
						component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team join " + player.team.getId()));
						p.getPlayer().sendMessage(component);
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

	public static final TeamAction ALLIES = new TeamAction(FTBLib.MOD_ID, "allies", GuiIcons.STAR, 40)
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

	public static final TeamAction MODERATORS = new TeamAction(FTBLib.MOD_ID, "moderators", GuiIcons.SHIELD, 50)
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

	public static final TeamAction ENEMIES = new TeamAction(FTBLib.MOD_ID, "enemies", GuiIcons.CLOSE, 60)
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

	public static final TeamAction LEAVE = new TeamAction(FTBLib.MOD_ID, "leave", GuiIcons.REMOVE, 10000)
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

	public static final TeamAction TRANSFER_OWNERSHIP = new TeamAction(FTBLib.MOD_ID, "transfer_ownership", GuiIcons.RIGHT, 10000)
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