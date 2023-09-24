package serverutils.serverlib.integration;

import serverutils.serverlib.lib.gui.GuiContainerWrapper;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.Widget;
import mezz.jei.api.gui.IGhostIngredientHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class NEIGhostItemHandler implements IGhostIngredientHandler<GuiContainerWrapper>
{
	@Override
	public <I> List<Target<I>> getTargets(GuiContainerWrapper gui, I ingredient, boolean doStart)
	{
		List<Target<I>> list = new ArrayList<>();
		getTargets(list, ingredient, gui.getGui());
		Collections.reverse(list);
		return list;
	}

	private <I> void getTargets(List<Target<I>> list, Object ingredient, Panel panel)
	{
		for (Widget widget : panel.widgets)
		{
			if (widget.isGhostIngredientTarget(ingredient))
			{
				list.add((Target<I>) new WidgetTarget(widget));
			}

			if (widget instanceof Panel)
			{
				getTargets(list, ingredient, (Panel) widget);
			}
		}
	}

	@Override
	public void onComplete()
	{
	}
}