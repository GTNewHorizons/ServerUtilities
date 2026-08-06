package serverutils.mixins.late.brigadier;

import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import serverutils.ranks.ICommandWithPermission;

@Mixin(value = LiteralCommandNode.class, remap = false)
public class MixinLiteralCommandNode<S> implements ICommandWithPermission {

    @Unique
    private String serverUtilities$permissionNode;

    @Unique
    private String serverUtilities$modName;

    @Unique
    private String serverUtilities$modId;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void serverutilities$init(String literal, Command<S> command, Predicate<S> requirement,
            CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks, CallbackInfo ci) {
        ModContainer container = Loader.instance().activeModContainer();
        serverutilities$setModName(container == null ? "" : container.getName());
        serverutilities$setModId(container == null ? "" : container.getModId());
    }

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

    @Override
    public String serverutilities$getModId() {
        return serverUtilities$modId;
    }

    public void serverutilities$setModId(@NotNull String modId) {
        this.serverUtilities$modId = modId;
    }
}
