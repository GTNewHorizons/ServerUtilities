package serverutils.serverlib.lib.tile;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface IChangeCallback
{
	void onContentsChanged(boolean majorChange);
}