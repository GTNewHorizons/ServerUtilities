package serverutils.data;

import javax.annotation.Nullable;

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
    RESPAWN(ServerUtilitiesPermissions.RESPAWN_BACK, null, null);

    private String permission;
    private String warmup;
    private String cooldown;

    TeleportType(String node, @Nullable String warmup, @Nullable String cooldown) {
        this.permission = node;
        this.warmup = warmup;
        this.cooldown = cooldown;
    }

    public String getPermission() {
        return this.permission;
    }

    public String getWarmupPermission() {
        return this.warmup;
    }

    public String getCooldownPermission() {
        return this.cooldown;
    }
}
