package serverutils.serverlib.lib.gui;

import serverutils.serverlib.lib.client.ClientUtils;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public interface IOpenableGui extends Runnable
{
	void openGui();

	default void openGuiLater()
	{
		ClientUtils.runLater(this);
	}

	default void closeGui()
	{
		closeGui(true);
	}

	default void closeGui(boolean openPrevScreen)
	{
	}

	default void openContextMenu(@Nullable Panel panel)
	{
		if (this instanceof Widget)
		{
			((Widget) this).getGui().openContextMenu(panel);
		}
	}

	default void closeContextMenu()
	{
		if (this instanceof Widget)
		{
			((Widget) this).getGui().closeContextMenu();
		}
		else
		{
			openContextMenu(null);
		}
	}

	@Override
	default void run()
	{
		openGui();
	}
}