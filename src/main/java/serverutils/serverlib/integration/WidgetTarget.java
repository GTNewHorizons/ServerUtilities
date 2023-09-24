package serverutils.serverlib.integration;

import serverutils.serverlib.lib.gui.Widget;
import mezz.jei.api.gui.IGhostIngredientHandler;

import java.awt.*;

/**
 * @author LatvianModder
 */
public class WidgetTarget implements IGhostIngredientHandler.Target<Object>
{
	private final Widget widget;

	public WidgetTarget(Widget w)
	{
		widget = w;
	}

	@Override
	public Rectangle getArea()
	{
		return new Rectangle(widget.getX(), widget.getY(), widget.width, widget.height);
	}

	@Override
	public void accept(Object ingredient)
	{
		widget.acceptGhostIngredient(ingredient);
	}
}