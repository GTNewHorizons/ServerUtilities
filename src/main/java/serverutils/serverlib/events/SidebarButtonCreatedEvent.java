package serverutils.serverlib.events;

import serverutils.serverlib.client.SidebarButton;
public class SidebarButtonCreatedEvent extends ServerLibEvent
{
	private final SidebarButton button;

	public SidebarButtonCreatedEvent(SidebarButton b)
	{
		button = b;
	}

	public SidebarButton getButton()
	{
		return button;
	}
}