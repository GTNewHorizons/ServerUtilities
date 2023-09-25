package serverutils.lib.events;

import serverutils.lib.client.SidebarButton;

public class SidebarButtonCreatedEvent extends ServerUtilitiesLibEvent {

    private final SidebarButton button;

    public SidebarButtonCreatedEvent(SidebarButton b) {
        button = b;
    }

    public SidebarButton getButton() {
        return button;
    }
}
