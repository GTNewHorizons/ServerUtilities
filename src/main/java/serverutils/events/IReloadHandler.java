package serverutils.events;

@FunctionalInterface
public interface IReloadHandler {

    boolean onReload(ServerReloadEvent reloadEvent) throws Exception;
}
