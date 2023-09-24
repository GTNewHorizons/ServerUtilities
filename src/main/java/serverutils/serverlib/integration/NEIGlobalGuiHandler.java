package serverutils.serverlib.integration;

import serverutils.serverlib.client.ServerLibClientEventHandler;
import serverutils.serverlib.lib.gui.IGuiWrapper;
import serverutils.serverlib.lib.gui.WrappedIngredient;
import mezz.jei.api.gui.IGlobalGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;

/**
 * @author LatvianModder
 */
public class NEIGlobalGuiHandler implements IGlobalGuiHandler
{
	@Override
	public Collection<Rectangle> getGuiExtraAreas()
	{
		GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;

		if (ServerLibClientEventHandler.areButtonsVisible(currentScreen))
		{
			return Collections.singleton(ServerLibClientEventHandler.lastDrawnArea);
		}

		return Collections.emptySet();
	}

	@Override
	@Nullable
	public Object getIngredientUnderMouse(int mouseX, int mouseY)
	{
		GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;

		if (currentScreen instanceof IGuiWrapper)
		{
			return WrappedIngredient.unwrap(((IGuiWrapper) currentScreen).getGui().getIngredientUnderMouse());
		}

		return null;
	}
}