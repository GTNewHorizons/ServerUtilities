package serverutils.ranks;

import net.minecraft.command.ICommand;

import org.jetbrains.annotations.NotNull;

public interface ICommandWithPermission extends ICommand {

    String serverutilities$getPermissionNode();

    void serverutilities$setPermissionNode(@NotNull String node);

    String serverutilities$getModName();

    void serverutilities$setModName(@NotNull String modName);
}
