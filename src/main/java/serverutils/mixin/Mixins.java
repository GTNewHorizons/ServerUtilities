package serverutils.mixin;

import static serverutils.ServerUtilitiesConfig.commands;
import static serverutils.ServerUtilitiesConfig.general;
import static serverutils.ServerUtilitiesConfig.mixins;
import static serverutils.ServerUtilitiesConfig.ranks;
import static serverutils.ServerUtilitiesConfig.world;
import static serverutils.mixin.TargetedMod.RANDOMTHINGS;
import static serverutils.mixin.TargetedMod.VANILLA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import serverutils.core.ServerUtilitiesCore;

public enum Mixins {

    COMMAND_PERMISSIONS(new Builder("Command Permissions").addTargetedMod(VANILLA).setSide(Side.BOTH)
            .setPhase(Phase.EARLY).setApplyIf(() -> ranks.enabled && ranks.command_permissions)
            .addMixinClasses("minecraft.MixinCommandBase", "minecraft.MixinCommandHandler", "minecraft.MixinICommand")),
    REPLACE_TAB_NAMES(new Builder("Replace tab menu names").addTargetedMod(VANILLA).setSide(Side.CLIENT)
            .setPhase(Phase.EARLY).addMixinClasses("forge.MixinGuiIngameForge")),
    VANILLA_TP_BACK_COMPAT(new Builder("/back compat for the vanilla /tp").addTargetedMod(VANILLA).setSide(Side.BOTH)
            .setPhase(Phase.EARLY).setApplyIf(() -> commands.back).addMixinClasses("minecraft.MixinCommandTeleport")),
    VANISH(new Builder("/vanish command").addTargetedMod(VANILLA).setSide(Side.SERVER).setPhase(Phase.EARLY)
            .setApplyIf(() -> commands.vanish).addMixinClasses(
                    "minecraft.vanish.MixinServerConfigurationManager",
                    "minecraft.vanish.MixinMinecraftServer",
                    "minecraft.vanish.MixinEntityTrackerEntry",
                    "minecraft.vanish.MixinNetHandlerPlayServer",
                    "minecraft.vanish.MixinCommandListPlayers",
                    "minecraft.vanish.MixinEntityPlayer",
                    "minecraft.vanish.MixinEntityPlayerMP",
                    "minecraft.vanish.MixinWorld",
                    "minecraft.vanish.MixinItemInWorldManager")),
    HIDE_VANISHED_FROM_DETECTOR(new Builder("Hide vanished players from the RandomThings online detector")
            .addTargetedMod(RANDOMTHINGS).setSide(Side.SERVER).setPhase(Phase.LATE).setApplyIf(() -> commands.vanish)
            .addMixinClasses("randomthings.MixinWorldUtils")),
    PAUSE_WHEN_EMPTY(new Builder("Pauses the server when empty after X seconds; Servers Only").setPhase(Phase.EARLY)
            .setSide(Side.SERVER).addTargetedMod(TargetedMod.VANILLA)
            .addMixinClasses(
                    "minecraft.MixinMinecraftServer_PauseWhenEmpty",
                    "minecraft.MixinDedicatedServer_PauseWhenEmpty")
            .setApplyIf(() -> general.enable_pause_when_empty_property)),
    PLAYERS_SLEEPING_PERCENTAGE(new Builder("Player Sleeping Percentage").addTargetedMod(VANILLA).setSide(Side.BOTH)
            .setPhase(Phase.EARLY).setApplyIf(() -> world.enable_player_sleeping_percentage)
            .addMixinClasses("minecraft.MixinWorldServer_SleepPercentage")),
    ENDERMEN_GRIEFING(new Builder("Disable Endermen Griefing in Claimed Chunks").addTargetedMod(VANILLA)
            .setSide(Side.BOTH).setPhase(Phase.EARLY).setApplyIf(() -> mixins.endermen)
            .addMixinClasses("minecraft.MixinEndermanGriefing"));

    private final List<String> mixinClasses;
    private final Supplier<Boolean> applyIf;
    private final Phase phase;
    private final Side side;
    private final List<TargetedMod> targetedMods;
    private final List<TargetedMod> excludedMods;

    Mixins(Builder builder) {
        this.mixinClasses = builder.mixinClasses;
        this.applyIf = builder.applyIf;
        this.side = builder.side;
        this.targetedMods = builder.targetedMods;
        this.excludedMods = builder.excludedMods;
        this.phase = builder.phase;
        if (this.targetedMods.isEmpty()) {
            throw new RuntimeException("No targeted mods specified for " + this.name());
        }
        if (this.applyIf == null) {
            throw new RuntimeException("No ApplyIf function specified for " + this.name());
        }
    }

    public static List<String> getEarlyMixins(Set<String> loadedCoreMods) {
        // This may be possible to handle differently or fix.
        final List<String> mixins = new ArrayList<>();
        final List<String> notLoading = new ArrayList<>();
        for (Mixins mixin : Mixins.values()) {
            if (mixin.phase == Phase.EARLY) {
                if (mixin.shouldLoad(loadedCoreMods, Collections.emptySet())) {
                    mixins.addAll(mixin.mixinClasses);
                } else {
                    notLoading.addAll(mixin.mixinClasses);
                }
            }
        }
        ServerUtilitiesCore.LOGGER.info("Not loading the following EARLY mixins: {}", notLoading);
        return mixins;
    }

    public static List<String> getLateMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        final List<String> notLoading = new ArrayList<>();
        for (Mixins mixin : Mixins.values()) {
            if (mixin.phase == Phase.LATE) {
                if (mixin.shouldLoad(Collections.emptySet(), loadedMods)) {
                    mixins.addAll(mixin.mixinClasses);
                } else {
                    notLoading.addAll(mixin.mixinClasses);
                }
            }
        }
        ServerUtilitiesCore.LOGGER.info("Not loading the following LATE mixins: {}", notLoading.toString());
        return mixins;
    }

    private boolean shouldLoadSide() {
        return side == Side.BOTH || (side == Side.SERVER && FMLLaunchHandler.side().isServer())
                || (side == Side.CLIENT && FMLLaunchHandler.side().isClient());
    }

    private boolean allModsLoaded(List<TargetedMod> targetedMods, Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (targetedMods.isEmpty()) return false;

        for (TargetedMod target : targetedMods) {
            if (target == VANILLA) continue;

            // Check coremod first
            if (!loadedCoreMods.isEmpty() && target.coreModClass != null
                    && !loadedCoreMods.contains(target.coreModClass))
                return false;
            else if (!loadedMods.isEmpty() && target.modId != null && !loadedMods.contains(target.modId)) return false;
        }

        return true;
    }

    private boolean noModsLoaded(List<TargetedMod> targetedMods, Set<String> loadedCoreMods, Set<String> loadedMods) {
        if (targetedMods.isEmpty()) return true;

        for (TargetedMod target : targetedMods) {
            if (target == VANILLA) continue;

            // Check coremod first
            if (!loadedCoreMods.isEmpty() && target.coreModClass != null
                    && loadedCoreMods.contains(target.coreModClass))
                return false;
            else if (!loadedMods.isEmpty() && target.modId != null && loadedMods.contains(target.modId)) return false;
        }

        return true;
    }

    private boolean shouldLoad(Set<String> loadedCoreMods, Set<String> loadedMods) {
        return (shouldLoadSide() && applyIf.get()
                && allModsLoaded(targetedMods, loadedCoreMods, loadedMods)
                && noModsLoaded(excludedMods, loadedCoreMods, loadedMods));
    }

    private static class Builder {

        private final List<String> mixinClasses = new ArrayList<>();
        private Supplier<Boolean> applyIf = () -> true;
        private Side side = Side.BOTH;
        private Phase phase = Phase.LATE;
        private final List<TargetedMod> targetedMods = new ArrayList<>();
        private final List<TargetedMod> excludedMods = new ArrayList<>();

        public Builder(@SuppressWarnings("unused") String description) {}

        public Builder addMixinClasses(String... mixinClasses) {
            this.mixinClasses.addAll(Arrays.asList(mixinClasses));
            return this;
        }

        public Builder setPhase(Phase phase) {
            this.phase = phase;
            return this;
        }

        public Builder setSide(Side side) {
            this.side = side;
            return this;
        }

        public Builder setApplyIf(Supplier<Boolean> applyIf) {
            this.applyIf = applyIf;
            return this;
        }

        public Builder addTargetedMod(TargetedMod mod) {
            this.targetedMods.add(mod);
            return this;
        }

        public Builder addExcludedMod(TargetedMod mod) {
            this.excludedMods.add(mod);
            return this;
        }
    }

    @SuppressWarnings("SimplifyStreamApiCallChains")
    private static String[] addPrefix(String prefix, String... values) {
        return Arrays.stream(values).map(s -> prefix + s).collect(Collectors.toList())
                .toArray(new String[values.length]);
    }

    private enum Side {
        BOTH,
        CLIENT,
        SERVER
    }

    private enum Phase {
        EARLY,
        LATE,
    }
}
