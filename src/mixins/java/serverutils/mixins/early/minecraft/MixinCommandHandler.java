package serverutils.mixins.early.minecraft;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.Event;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.NodeEntry;
import serverutils.lib.command.CommandTreeBase;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.ranks.ICommandWithPermission;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

@Mixin(CommandHandler.class)
public abstract class MixinCommandHandler {

    @ModifyExpressionValue(
            method = { "getPossibleCommands(Lnet/minecraft/command/ICommandSender;)Ljava/util/List;",
                    "executeCommand" },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/command/ICommand;canCommandSenderUseCommand(Lnet/minecraft/command/ICommandSender;)Z"))
    private boolean serverutilities$checkPermission(boolean original, @Local(argsOnly = true) ICommandSender sender,
            @Local ICommand command) {
        if (sender instanceof EntityPlayerMP player) {
            Event.Result result = Ranks.INSTANCE.getPermissionResult(
                    player,
                    ((ICommandWithPermission) command).serverutilities$getPermissionNode(),
                    true);

            if (result != Event.Result.DEFAULT) {
                return result == Event.Result.ALLOW;
            }
        }
        return original;
    }

    @Inject(method = "registerCommand", at = @At(value = "HEAD"))
    private void serverutilities$setPermissionNode(ICommand command, CallbackInfoReturnable<ICommand> cir) {
        ModContainer container = Loader.instance().activeModContainer();
        String node = (container == null ? Rank.NODE_COMMAND : (Rank.NODE_COMMAND + '.' + container.getModId())) + "."
                + command.getCommandName();
        ICommandWithPermission cmd = (ICommandWithPermission) command;
        cmd.serverutilities$setPermissionNode(node);
        cmd.serverutilities$setModName(container == null ? "Minecraft" : container.getName());
        serverUtilities$registerPermissions(cmd);
    }

    @Unique
    private DefaultPermissionLevel serverUtilities$getDefaultLevel(ICommand command) {
        if (command instanceof CommandBase cmdBase) {
            return cmdBase.getRequiredPermissionLevel() > 0 ? DefaultPermissionLevel.OP : DefaultPermissionLevel.ALL;
        }
        return DefaultPermissionLevel.OP;
    }

    @Unique
    private void serverUtilities$registerPermissions(ICommandWithPermission command) {
        String node = command.serverutilities$getPermissionNode();
        DefaultPermissionLevel level = serverUtilities$getDefaultLevel(command);

        if (command instanceof CommandTreeBase tree) {
            for (ICommand c : tree.getSubCommands()) {
                ICommandWithPermission child = (ICommandWithPermission) c;
                child.serverutilities$setPermissionNode(node + '.' + child.getCommandName());
                child.serverutilities$setModName(command.serverutilities$getModName());
                serverUtilities$registerPermissions(child);
            }
        }

        if (Loader.instance().getLoaderState().ordinal() > LoaderState.PREINITIALIZATION.ordinal()) {
            PermissionAPI.registerNode(node, level, "");
        } else {
            ServerUtilitiesPermissions.earlyPermissions.add(new NodeEntry(node, level, ""));
        }
    }
}
