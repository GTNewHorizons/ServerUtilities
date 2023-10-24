package serverutils.lib.data;

import serverutils.lib.util.IStringSerializable;
import serverutils.lib.util.misc.NameMap;

public enum TeamType implements IStringSerializable {

    PLAYER("player", true, false, true, false),
    SERVER("server", false, true, true, false),
    SERVER_NO_SAVE("server_no_save", false, true, false, false),
    NONE("none", false, false, false, true);

    private final String name;
    public final boolean isPlayer, isServer, save, isNone;

    public static final NameMap<TeamType> NAME_MAP = NameMap.create(PLAYER, values());

    TeamType(String n, boolean p, boolean s, boolean sv, boolean nn) {
        name = n;
        isPlayer = p;
        isServer = s;
        save = sv;
        isNone = nn;
    }

    @Override
    public String getName() {
        return name;
    }
}
