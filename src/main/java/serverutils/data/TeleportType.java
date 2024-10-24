package serverutils.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import serverutils.ServerUtilitiesPermissions;

public enum TeleportType {

    HOME(ServerUtilitiesPermissions.HOMES_BACK, ServerUtilitiesPermissions.HOMES_WARMUP,
            ServerUtilitiesPermissions.HOMES_COOLDOWN),
    WARP(ServerUtilitiesPermissions.WARPS_BACK, ServerUtilitiesPermissions.WARPS_WARMUP,
            ServerUtilitiesPermissions.WARPS_COOLDOWN),
    BACK(ServerUtilitiesPermissions.BACK_BACK, ServerUtilitiesPermissions.BACK_WARMUP,
            ServerUtilitiesPermissions.BACK_COOLDOWN),
    SPAWN(ServerUtilitiesPermissions.SPAWN_BACK, ServerUtilitiesPermissions.SPAWN_WARMUP,
            ServerUtilitiesPermissions.SPAWN_COOLDOWN),
    TPA(ServerUtilitiesPermissions.TPA_BACK, ServerUtilitiesPermissions.TPA_WARMUP,
            ServerUtilitiesPermissions.TPA_COOLDOWN),
    RTP(ServerUtilitiesPermissions.RTP_BACK, ServerUtilitiesPermissions.RTP_WARMUP,
            ServerUtilitiesPermissions.RTP_COOLDOWN),
    RESPAWN(ServerUtilitiesPermissions.RESPAWN_BACK, null, null),
    VANILLA_TP(ServerUtilitiesPermissions.VANILLA_TP_BACK, null, null);

    private final String permission;
    private final String warmup;
    private final String cooldown;

    TeleportType(@NotNull String node, @Nullable String warmup, @Nullable String cooldown) {
        this.permission = node;
        this.warmup = warmup;
        this.cooldown = cooldown;
    }

    public @NotNull String getPermission() {
        return this.permission;
    }

    public @Nullable String getWarmupPermission() {
        return this.warmup;
    }

    public @Nullable String getCooldownPermission() {
        return this.cooldown;
    }
}
