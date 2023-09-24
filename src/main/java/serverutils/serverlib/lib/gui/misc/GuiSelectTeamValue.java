package serverutils.serverlib.lib.gui.misc;

import serverutils.serverlib.lib.config.ConfigTeamClient;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.IOpenableGui;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.SimpleTextButton;
import serverutils.serverlib.lib.util.misc.MouseButton;
import net.minecraft.client.resources.I18n;

/**
 * @author LatvianModder
 */
public class GuiSelectTeamValue extends GuiButtonListBase
{
	private final ConfigTeamClient value;
	private final IOpenableGui callbackGui;
	private final Runnable callback;

	public GuiSelectTeamValue(ConfigTeamClient v, IOpenableGui c, Runnable cb)
	{
		setTitle(I18n.format("ftblib.select_team.gui"));
		setHasSearchBox(true);
		value = v;
		callbackGui = c;
		callback = cb;
	}

	@Override
	public void addButtons(Panel panel)
	{
		for (ConfigTeamClient.TeamInst inst : value.map.values())
		{
			panel.add(new SimpleTextButton(panel, inst.title.getFormattedText(), inst.icon)
			{
				@Override
				public void onClicked(MouseButton button)
				{
					GuiHelper.playClickSound();
					callbackGui.openGui();
					value.setString(inst.getId());
					callback.run();
				}
			});
		}
	}
}