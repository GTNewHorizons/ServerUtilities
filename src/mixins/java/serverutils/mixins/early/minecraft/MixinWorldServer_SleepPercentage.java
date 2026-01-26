package serverutils.mixins.early.minecraft;

import static serverutils.ServerUtilitiesConfig.afk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.ISaveHandler;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import serverutils.data.ServerUtilitiesPlayerData;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer_SleepPercentage extends World {

    @Shadow
    private boolean allPlayersSleeping;

    @Shadow
    @Final
    private MinecraftServer mcServer;
    @Unique
    private int percent;

    @Unique
    private List<EntityPlayer> sleepingPlayers;

    @Unique
    private Set<UUID> previousSleepingPlayers;

    /**
     * We need to access this.playerEntities from the superclass, so we're extending World, and need this fake
     * constructor to make Java happy
     **/
    public MixinWorldServer_SleepPercentage(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
            WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
        throw new RuntimeException(
                "Server Utilities player sleeping percentage broke in a huge way. This error should never happen");
    }

    @Inject(method = "updateAllPlayersSleepingFlag", at = @At("HEAD"), cancellable = true)
    public void serverutilities$handlePlayersSleepingPercentage(CallbackInfo ci) {
        percent = Integer.parseInt(this.getGameRules().getGameRuleStringValue("playersSleepingPercentage"));
        if (percent > 100) {
            this.allPlayersSleeping = false;
            ci.cancel(/* /r/nosleep, vanilla behaviour */);
        } else {
            EntityPlayer theSleeper = null;
            if (sleepingPlayers == null) {
                sleepingPlayers = new ArrayList<>();
            }
            sleepingPlayers.clear();
            int playerCountWithoutAFK = serverutilities$getListWithoutAFK(this.playerEntities).size();
            int cap = (int) Math.ceil(playerCountWithoutAFK * percent * 0.01f);
            for (EntityPlayer player : this.playerEntities) {
                if (player.isPlayerSleeping()) {
                    sleepingPlayers.add(player);
                    // to find the player who was the last to sleep
                    if (!previousSleepingPlayers.contains(player.getUniqueID())) {
                        theSleeper = player;
                    }
                    if (sleepingPlayers.size() >= cap) {
                        this.allPlayersSleeping = true;
                        break;
                    }
                }
            }
            previousSleepingPlayers = sleepingPlayers.stream().map(EntityPlayer::getUniqueID)
                    .collect(Collectors.toSet());
            // if server is dedicated, or open to lan
            if (!sleepingPlayers.isEmpty() && cap > 0 && theSleeper != null && (!mcServer.isSinglePlayer())) {
                for (EntityPlayer player : this.playerEntities) {
                    String percentString = String.format("%d", (sleepingPlayers.size() * 100) / playerCountWithoutAFK);
                    player.addChatMessage(
                            new ChatComponentTranslation(
                                    "serverutilities.world.players_sleeping",
                                    theSleeper.getDisplayName(),
                                    sleepingPlayers.size(),
                                    playerCountWithoutAFK,
                                    percentString));
                }
            }
            ci.cancel();
        }
    }

    @Redirect(
            method = "areAllPlayersAsleep",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/WorldServer;playerEntities:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD))
    public List<EntityPlayer> serverutilities$speedup1(WorldServer instance) {
        return sleepingPlayers.isEmpty() ? this.playerEntities : sleepingPlayers;
    }

    @Inject(
            method = "areAllPlayersAsleep",
            at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"),
            cancellable = true)
    public void serverutilities$speedup2(CallbackInfoReturnable<Boolean> ctx) {
        if (percent < 1) ctx.setReturnValue(true);
    }

    @Inject(
            method = "wakeAllPlayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;wakeUpPlayer(ZZZ)V"))
    public void serverutilities$broadcast(CallbackInfo ctx, @Local EntityPlayer player) {
        if (percent > 0 && percent < 100 && (!mcServer.isSinglePlayer())) {
            player.addChatMessage(new ChatComponentTranslation("serverutilities.world.skip_night"));
        }
    }

    @Unique
    public List<EntityPlayer> serverutilities$getListWithoutAFK(List<EntityPlayer> list) {
        return list.stream().filter(player -> {
            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.getNullable(player);
            return data == null || data.afkTime <= afk.getNotificationTimer();
        }).collect(Collectors.toList());
    }
}
