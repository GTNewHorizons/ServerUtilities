package serverutils.lib.lib.tile;

@FunctionalInterface
public interface IChangeCallback {

    void onContentsChanged(boolean majorChange);
}
