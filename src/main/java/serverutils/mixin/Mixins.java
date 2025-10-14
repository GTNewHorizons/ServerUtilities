package serverutils.mixin;

import static serverutils.ServerUtilitiesConfig.*;
import static serverutils.mixin.TargetedMod.RANDOMTHINGS;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // spotless:off
    COMMAND_PERMISSIONS(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> ranks.enabled && ranks.command_permissions)
            .addCommonMixins(
                    "minecraft.MixinCommandBase",
                    "minecraft.MixinCommandHandler",
                    "minecraft.MixinICommand")),
    REPLACE_TAB_NAMES(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .addClientMixins("forge.MixinGuiIngameForge")),
    VANILLA_TP_BACK_COMPAT(new MixinBuilder("/back compat for the vanilla /tp")
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> commands.back)
            .addCommonMixins("minecraft.MixinCommandTeleport")),
    VANISH_COMMAND(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> commands.vanish)
            .addServerMixins(
                    "minecraft.vanish.MixinServerConfigurationManager",
                    "minecraft.vanish.MixinMinecraftServer",
                    "minecraft.vanish.MixinEntityTrackerEntry",
                    "minecraft.vanish.MixinNetHandlerPlayServer",
                    "minecraft.vanish.MixinCommandListPlayers",
                    "minecraft.vanish.MixinEntityPlayer",
                    "minecraft.vanish.MixinEntityPlayerMP",
                    "minecraft.vanish.MixinWorld",
                    "minecraft.vanish.MixinItemInWorldManager")),
    HIDE_VANISHED_FROM_DETECTOR(new MixinBuilder("Hide vanished players from the RandomThings online detector")
            .addRequiredMod(RANDOMTHINGS)
            .setPhase(Phase.LATE)
            .setApplyIf(() -> commands.vanish)
            .addServerMixins("randomthings.MixinWorldUtils")),
    PAUSE_WHEN_EMPTY(new MixinBuilder("Pauses the server when empty after X seconds; Servers Only")
            .setPhase(Phase.EARLY)
            .addServerMixins(
                    "minecraft.MixinMinecraftServer_PauseWhenEmpty",
                    "minecraft.MixinDedicatedServer_PauseWhenEmpty")
            .setApplyIf(() -> general.enable_pause_when_empty_property)),
    PLAYERS_SLEEPING_PERCENTAGE(new MixinBuilder()
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> world.enable_player_sleeping_percentage)
            .addCommonMixins("minecraft.MixinWorldServer_SleepPercentage")),
    DISABLE_ENDERMEN_GRIEFING(new MixinBuilder("Disable Endermen Griefing in Claimed Chunks")
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> mixins.endermen)
            .addCommonMixins("minecraft.MixinEndermanGriefing")),
    BYPASS_PLAYER_LIMIT(new MixinBuilder("Adds permission for bypassing player limit")
            .setPhase(Phase.EARLY)
            .setApplyIf(() -> mixins.bypassPlayerLimit)
            .addCommonMixins("minecraft.MixinNetHandlerLoginServer"));
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @NotNull
    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
