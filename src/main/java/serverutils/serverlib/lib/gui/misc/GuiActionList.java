package serverutils.serverlib.lib.gui.misc;

import serverutils.serverlib.lib.data.Action;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.SimpleTextButton;
import serverutils.serverlib.lib.gui.WidgetType;
import serverutils.serverlib.lib.util.misc.MouseButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class GuiActionList extends GuiButtonListBase
{
	private class ActionButton extends SimpleTextButton
	{
		private final Action.Inst action;

		private ActionButton(Panel panel, Action.Inst a)
		{
			super(panel, a.title.getFormattedText(), a.icon);
			action = a;
		}

		@Override
		public void onClicked(MouseButton button)
		{
			GuiHelper.playClickSound();

			if (action.requiresConfirm)
			{
				String key = "team_action." + action.id.getNamespace() + "." + action.id.getPath() + ".confirmation";
				openYesNo(action.title.getFormattedText() + "?", I18n.hasKey(key) ? (TextFormatting.RED + I18n.format(key)) : "", () -> callback.accept(action.id));
			}
			else
			{
				callback.accept(action.id);
			}
		}

		@Override
		public boolean renderTitleInCenter()
		{
			return false;
		}

		@Override
		public WidgetType getWidgetType()
		{
			return action.enabled ? WidgetType.mouseOver(isMouseOver()) : WidgetType.DISABLED;
		}
	}

	private final ArrayList<Action.Inst> actions;
	private final Consumer<ResourceLocation> callback;

	public GuiActionList(String title, Collection<Action.Inst> a, Consumer<ResourceLocation> c)
	{
		setTitle(title);
		actions = new ArrayList<>(a);
		actions.sort(null);
		callback = c;
	}

	@Override
	public void addButtons(Panel panel)
	{
		for (Action.Inst a : actions)
		{
			panel.add(new ActionButton(panel, a));
		}
	}
}
