package serverutils.ranks;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public interface ICommandWithPermission {

    Map<String, String> commandOwners = new HashMap<>();

    Map<String, String> commandPermissions = new HashMap<>();

    String serverutilities$getPermissionNode();

    void serverutilities$setPermissionNode(@NotNull String node);

    String serverutilities$getModName();

    void serverutilities$setModName(@NotNull String modName);
}
