package serverutils.lib.events;

import java.util.function.Consumer;

import serverutils.lib.lib.config.IRankConfigHandler;

public class RegisterRankConfigHandlerEvent extends ServerUtilitiesLibEvent {

	private final Consumer<IRankConfigHandler> callback;

	public RegisterRankConfigHandlerEvent(Consumer<IRankConfigHandler> c) {
		callback = c;
	}

	public void setHandler(IRankConfigHandler handler) {
		callback.accept(handler);
	}
}
