package serverutils.lib.tile;

@FunctionalInterface
public interface IChangeCallback {

    void onContentsChanged(boolean majorChange);
}
