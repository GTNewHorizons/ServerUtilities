package serverutils.mixins.early.minecraft.vanish;

import static serverutils.ServerUtilitiesPermissions.SEE_VANISH;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandListPlayers;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.EnumChatFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import serverutils.lib.data.Universe;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringJoiner;
import serverutils.lib.util.permission.PermissionAPI;

@Mixin(CommandListPlayers.class)
public abstract class MixinCommandListPlayers {

    @Redirect(
            method = "processCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;func_152609_b(Z)Ljava/lang/String;"))
    private String serverutilities$removeVanishedPlayers(ServerConfigurationManager instance, boolean includeUuid,
            @Local(argsOnly = true) ICommandSender sender) {
        List<EntityPlayerMP> players = new ArrayList<>(instance.playerEntityList);

        boolean canSeeVanish = !(sender instanceof EntityPlayerMP playerMP)
                || PermissionAPI.hasPermission(playerMP, SEE_VANISH);
        if (!canSeeVanish) {
            players.removeIf(ServerUtils::isVanished);
        }

        return StringJoiner.with(", ").join(players, player -> {
            String playerName = player.getCommandSenderName();

            if (canSeeVanish && ServerUtils.isVanished(player)) {
                playerName = "[V] " + EnumChatFormatting.STRIKETHROUGH + playerName + EnumChatFormatting.RESET;
            }

            if (includeUuid) {
                playerName += " (" + player.getUniqueID().toString() + ")";
            }

            return playerName;
        });
    }

    @ModifyExpressionValue(
            method = "processCommand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCurrentPlayerCount()I"))
    private int serverutilities$removeVanishedPlayers(int original, @Local(argsOnly = true) ICommandSender sender) {
        if (!(sender instanceof EntityPlayerMP playerMP) || PermissionAPI.hasPermission(playerMP, SEE_VANISH)) {
            return original + Universe.get().getVanishedPlayers().size();
        }
        return original;
    }
}
