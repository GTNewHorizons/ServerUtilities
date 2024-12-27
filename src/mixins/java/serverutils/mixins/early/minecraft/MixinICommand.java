package serverutils.mixins.early.minecraft;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.ranks.ICommandWithPermission;
import serverutils.ranks.Ranks;

@Mixin(ICommand.class)
public interface MixinICommand extends ICommandWithPermission {

    @Shadow
    String getCommandName();

    @Shadow
    boolean canCommandSenderUseCommand(ICommandSender sender);

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

    @Override
    default boolean serverutilities$hasPermission(@NotNull EntityPlayerMP player) {
        Event.Result result = Ranks.INSTANCE.getPermissionResult(player, serverutilities$getPermissionNode(), true);
        if (result == Event.Result.DEFAULT) {
            return canCommandSenderUseCommand(player);
        }

        return result == Event.Result.ALLOW;
    }
}
