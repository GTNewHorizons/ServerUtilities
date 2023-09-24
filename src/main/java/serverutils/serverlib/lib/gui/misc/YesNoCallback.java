package serverutils.serverlib.lib.gui.misc;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface YesNoCallback
{
	void onButtonClicked(boolean result);
}