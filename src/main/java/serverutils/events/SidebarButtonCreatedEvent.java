package serverutils.events;

import serverutils.client.gui.SidebarButton;

public class SidebarButtonCreatedEvent extends ServerUtilitiesEvent {

    private final SidebarButton button;

    public SidebarButtonCreatedEvent(SidebarButton b) {
        button = b;
    }

    public SidebarButton getButton() {
        return button;
    }
}
