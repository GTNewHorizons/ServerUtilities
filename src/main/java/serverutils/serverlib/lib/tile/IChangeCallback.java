package serverutils.serverlib.lib.tile;

@FunctionalInterface
public interface IChangeCallback {

	void onContentsChanged(boolean majorChange);
}