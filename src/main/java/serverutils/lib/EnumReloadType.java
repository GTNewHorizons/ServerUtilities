package serverutils.lib;

public enum EnumReloadType {

    /**
     * On client side - logged in, on server side - world created
     */
    CREATED(false),

    /**
     * On client side - /reload_client, on server side - /reload
     */
    RELOAD_COMMAND(true);

    private final boolean command;

    EnumReloadType(boolean b) {
        command = b;
    }

    public boolean command() {
        return command;
    }
}
