package serverutils.mixins.early.minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.ISaveHandler;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import serverutils.ServerUtilitiesConfig;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.data.Universe;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World {

    @Shadow
    private boolean allPlayersSleeping;

    @Unique
    private int percent;

    @Unique
    private List<EntityPlayer> sleepingPlayers;

    /**
     * We need to access this.playerEntities from the superclass, so we're extending World, and need this fake
     * constructor to make Java happy
     **/
    public MixinWorldServer(ISaveHandler p_i45368_1_, String p_i45368_2_, WorldProvider p_i45368_3_,
            WorldSettings p_i45368_4_, Profiler p_i45368_5_) {
        super(p_i45368_1_, p_i45368_2_, p_i45368_3_, p_i45368_4_, p_i45368_5_);
        throw new ArithmeticException("2 + 2 = 5 ???");
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void serverutilities$playersSleepingConstructor(CallbackInfo ci) {
        sleepingPlayers = new ArrayList<>();
    }

    @Inject(method = "updateAllPlayersSleepingFlag", at = @At("HEAD"), cancellable = true)
    public void hhheheheheeh(CallbackInfo ci) {
        percent = Integer.parseInt(this.getGameRules().getGameRuleStringValue("playersSleepingPercentage"));
        if (percent > 100) {
            this.allPlayersSleeping = false;
            ci.cancel(/* /r/nosleep, vanilla behaviour */);
        } else {
            sleepingPlayers.clear();
            int cap = (int) Math.ceil(getListWithoutAFK(this.playerEntities).size() * percent * 0.01f);
            for (EntityPlayer player : this.playerEntities) {
                if (player.isPlayerSleeping()) {
                    sleepingPlayers.add(player);
                    if (sleepingPlayers.size() >= cap) {
                        this.allPlayersSleeping = true;
                        break;
                    }
                }
            }

            if (!sleepingPlayers.isEmpty() && cap > 0) {
                for (EntityPlayer player : this.playerEntities) {
                    String percentString = String.format("%d", (sleepingPlayers.size() * 100) / cap);
                    player.addChatMessage(
                            new ChatComponentTranslation(
                                    "serverutilities.world.players_sleeping",
                                    player.getDisplayName(),
                                    sleepingPlayers.size(),
                                    cap,
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
    public List<EntityPlayer> baited(WorldServer instance) {
        return sleepingPlayers.isEmpty() ? this.playerEntities : sleepingPlayers;
    }

    @Inject(
            method = "areAllPlayersAsleep",
            at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"),
            cancellable = true)
    public void turbofast(CallbackInfoReturnable<Boolean> ctx) {
        if (percent < 1) ctx.setReturnValue(true);
    }

    @Inject(
            method = "wakeAllPlayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;wakeUpPlayer(ZZZ)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void broadcast(CallbackInfo ctx, Iterator iterator, EntityPlayer player) {
        if (percent > 0 && percent < 100) {
            player.addChatMessage(new ChatComponentTranslation("serverutiltiies.world.skip_night"));
        }
    }

    public List<EntityPlayer> getListWithoutAFK(List<EntityPlayer> list) {
        long notificationTimer = ServerUtilitiesConfig.afk.getNotificationTimer();
        return list.stream()
                .filter(
                        (EntityPlayer entity) -> ServerUtilitiesPlayerData
                                .get(Universe.get().getPlayer((EntityPlayerMP) entity)).afkTime >= notificationTimer)
                .collect(Collectors.toList());
    }
}
