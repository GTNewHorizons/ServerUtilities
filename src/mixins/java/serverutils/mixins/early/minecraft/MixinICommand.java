package serverutils.mixins.early.minecraft;

import net.minecraft.command.ICommand;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import serverutils.ranks.ICommandWithPermission;

@Mixin(ICommand.class)
public interface MixinICommand extends ICommandWithPermission {

    @Shadow
    String getCommandName();

    @Override
    default String serverutilities$getPermissionNode() {
        return commandPermissions.get(getCommandName());
    }

    @Override
    default void serverutilities$setPermissionNode(@NotNull String node) {
        commandPermissions.put(getCommandName(), node);
    }

    @Override
    default String serverutilities$getModName() {
        return commandOwners.get(getCommandName());
    }

    @Override
    default void serverutilities$setModName(@NotNull String modName) {
        commandOwners.put(getCommandName(), modName);
    }
}
