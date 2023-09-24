package serverutils.serverlib.command.client;

import serverutils.serverlib.lib.command.CmdBase;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.SimpleTextButton;
import serverutils.serverlib.lib.gui.misc.GuiButtonListBase;
import serverutils.serverlib.lib.icon.ItemIcon;
import serverutils.serverlib.lib.util.misc.MouseButton;
import net.minecraft.stats.Achievement;
import net.minecraft.advancements.Criterion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandListAchievements extends CmdBase
{
	public CommandListAchievements()
	{
		super("list_achievements", Level.ALL);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, final String[] args) throws CommandException
	{
		if (!(sender instanceof EntityPlayer))
		{
			return;
		}

		List<Achievement> list = new ArrayList<>();

		for (Achievement a : Minecraft.getMinecraft().thePlayer.connection.getAdvancementManager().getAdvancementList().getAdvancements())
		{
			if (a.getDisplay() != null)
			{
				list.add(a);
			}
		}

		list.sort((o1, o2) -> o1.getDisplay().getTitle().getUnformattedText().compareToIgnoreCase(o2.getDisplay().getTitle().getUnformattedText()));

		GuiButtonListBase gui = new GuiButtonListBase()
		{
			@Override
			public void addButtons(Panel panel)
			{
				for (Achievement achievement : list)
				{
					panel.add(new SimpleTextButton(panel, achievement.getDisplay().getTitle().getFormattedText(), ItemIcon.getItemIcon(achievement.getDisplay().getIcon()))
					{
						@Override
						public void onClicked(MouseButton button)
						{
							GuiHelper.playClickSound();
							GuiScreen.setClipboardString(achievement.statId); //getId().toString());
							closeGui();
						}

						@Override
						public void addMouseOverText(List<String> list)
						{
							super.addMouseOverText(list);
							list.add(EnumChatFormatting.GRAY + Achievement.getId().toString());

							for (Map.Entry<String, Criterion> entry : achievement.getCriteria().entrySet())
							{
								list.add(EnumChatFormatting.DARK_GRAY + "- " + entry.getKey());
							}
						}
					});
				}
			}
		};

		gui.setTitle(I18n.format("gui.achievements"));
		gui.setHasSearchBox(true);
		gui.openGuiLater();
	}
}