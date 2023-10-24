package serverutils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportLog;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.util.permission.PermissionAPI;

public class CmdBack extends CmdBase {

    public CmdBack() {
        super("back", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);

        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(p);

        TeleportLog lastTeleportLog = data.getLastTeleportLog();

        if (lastTeleportLog == null) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.no_dp");
        }

        data.checkTeleportCooldown(sender, ServerUtilitiesPlayerData.Timer.BACK);

        ServerUtilitiesPlayerData.Timer.BACK.teleport(player, playerMP -> lastTeleportLog.teleporter(), universe -> {
            if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.INFINITE_BACK_USAGE)) {
                for (TeleportType t : TeleportType.values()) {
                    data.clearLastTeleport(t);
                }
            }
        });
    }
}
