package serverutils.serverlib.events;

import serverutils.serverlib.lib.config.IRankConfigHandler;

import java.util.function.Consumer;

public class RegisterRankConfigHandlerEvent extends ServerLibEvent
{
	private final Consumer<IRankConfigHandler> callback;

	public RegisterRankConfigHandlerEvent(Consumer<IRankConfigHandler> c)
	{
		callback = c;
	}

	public void setHandler(IRankConfigHandler handler)
	{
		callback.accept(handler);
	}
}