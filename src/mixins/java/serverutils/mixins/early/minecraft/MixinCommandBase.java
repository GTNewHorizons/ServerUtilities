package serverutils.mixins.early.minecraft;

import net.minecraft.command.CommandBase;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import serverutils.ranks.ICommandWithPermission;

@Mixin(CommandBase.class)
public abstract class MixinCommandBase implements ICommandWithPermission {

    @Unique
    private String serverUtilities$permissionNode;

    @Unique
    private String serverUtilities$modName;

    @Override
    public String serverutilities$getPermissionNode() {
        return serverUtilities$permissionNode;
    }

    @Override
    public void serverutilities$setPermissionNode(@NotNull String node) {
        this.serverUtilities$permissionNode = node;
    }

    @Override
    public String serverutilities$getModName() {
        return serverUtilities$modName;
    }

    @Override
    public void serverutilities$setModName(@NotNull String modName) {
        this.serverUtilities$modName = modName;
    }
}
