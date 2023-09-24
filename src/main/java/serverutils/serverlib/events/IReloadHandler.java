package serverutils.serverlib.events;

@FunctionalInterface
public interface IReloadHandler
{
	boolean onReload(ServerReloadEvent reloadEvent) throws Exception;
}