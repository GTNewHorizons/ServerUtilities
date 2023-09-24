package serverutils.serverlib.events;

import java.util.function.Consumer;

import serverutils.serverlib.lib.config.IRankConfigHandler;

public class RegisterRankConfigHandlerEvent extends ServerLibEvent {

	private final Consumer<IRankConfigHandler> callback;

	public RegisterRankConfigHandlerEvent(Consumer<IRankConfigHandler> c) {
		callback = c;
	}

	public void setHandler(IRankConfigHandler handler) {
		callback.accept(handler);
	}
}
