package serverutils.mixins.early.minecraft;

import static serverutils.ServerUtilitiesConfig.afk;

import java.util.ArrayList;
import java.util.HashSet;
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

import serverutils.ServerUtilitiesConfig;
import serverutils.compat.WitcheryCompat;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.OtherMods;

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

    @Unique
    private boolean serverUtilities$isVampireSleep;

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
            return;
        }
        EntityPlayer theSleeper = null;
        if (sleepingPlayers == null) {
            sleepingPlayers = new ArrayList<>();
        }
        if (previousSleepingPlayers == null) {
            previousSleepingPlayers = new HashSet<>();
        }
        sleepingPlayers.clear();
        int playerCountWithoutAFK = 0;

        int vampireSleepCount = 0;

        for (EntityPlayer player : this.playerEntities) {
            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.getNullable(player);
            if (data == null || data.afkTime <= afk.getNotificationTimer()) {
                ++playerCountWithoutAFK;
            }
            if (player.isPlayerSleeping()) {
                sleepingPlayers.add(player);
                // to find the player who was the last to sleep
                if (!previousSleepingPlayers.contains(player.getUniqueID())) {
                    theSleeper = player;
                }

                if (OtherMods.isWitcheryLoaded() && WitcheryCompat.isVampire(player)
                        && WitcheryCompat.isSleepInCoffin(this, player)) {
                    ++vampireSleepCount;
                }
            }
        }
        int cap = (int) Math.ceil(playerCountWithoutAFK * percent * 0.01f);
        this.allPlayersSleeping = !sleepingPlayers.isEmpty() && sleepingPlayers.size() >= cap;

        if (allPlayersSleeping && vampireSleepCount > 0) {
            int vampirePercent = vampireSleepCount * 100 / sleepingPlayers.size();
            if (vampirePercent >= ServerUtilitiesConfig.world.vampire_sleep_percent) {
                serverUtilities$isVampireSleep = true;
            }
        }

        previousSleepingPlayers.clear();
        for (EntityPlayer p : sleepingPlayers) {
            previousSleepingPlayers.add(p.getUniqueID());
        }
        // if server is dedicated, or open to lan
        if (!sleepingPlayers.isEmpty() && cap > 0 && theSleeper != null && (!mcServer.isSinglePlayer())) {
            String key = "serverutilities.world.players_sleeping";

            if (OtherMods.isWitcheryLoaded() && WitcheryCompat.isVampire(theSleeper)
                    && WitcheryCompat.isSleepInCoffin(this, theSleeper)) {
                key = "serverutilities.world.vampires_sleeping";
            }
            String percentString = String.format("%d", (sleepingPlayers.size() * 100) / playerCountWithoutAFK);
            for (EntityPlayer player : this.playerEntities) {
                player.addChatMessage(
                        new ChatComponentTranslation(
                                key,
                                theSleeper.getDisplayName(),
                                sleepingPlayers.size(),
                                playerCountWithoutAFK,
                                percentString));
            }
        }
        ci.cancel();
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
            String key = serverUtilities$isVampireSleep ? "serverutilities.world.skip_day"
                    : "serverutilities.world.skip_night";
            player.addChatMessage(new ChatComponentTranslation(key));
        }
    }

    @Inject(method = "wakeAllPlayers", at = @At(value = "TAIL"))
    public void serverutilities$vampireSetTime(CallbackInfo ctx) {
        if (serverUtilities$isVampireSleep) {
            // Logic synced from Witchery
            long currentTime = worldInfo.getWorldTime() - 11000L;
            worldInfo.setWorldTime(currentTime);
            serverUtilities$isVampireSleep = false;
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
