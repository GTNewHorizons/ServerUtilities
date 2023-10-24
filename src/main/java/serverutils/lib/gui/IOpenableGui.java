package serverutils.lib.gui;

import javax.annotation.Nullable;

import serverutils.lib.client.ClientUtils;

public interface IOpenableGui extends Runnable {

    void openGui();

    default void openGuiLater() {
        ClientUtils.runLater(this);
    }

    default void closeGui() {
        closeGui(true);
    }

    default void closeGui(boolean openPrevScreen) {}

    default void openContextMenu(@Nullable Panel panel) {
        if (this instanceof Widget) {
            ((Widget) this).getGui().openContextMenu(panel);
        }
    }

    default void closeContextMenu() {
        if (this instanceof Widget) {
            ((Widget) this).getGui().closeContextMenu();
        } else {
            openContextMenu(null);
        }
    }

    @Override
    default void run() {
        openGui();
    }
}
