package serverutils.command.tp;

import static serverutils.data.TeleportType.BACK;

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
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.task.Task;

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

        BlockDimPos noPosFound = new BlockDimPos(0, 0, 0, 0);
        if (lastTeleportLog.getBlockDimPos().equalsPos(noPosFound)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.no_pos_found");
        }

        data.checkTeleportCooldown(sender, BACK);

        Task task = new Task() {

            @Override
            public void execute(Universe universe) {
                if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.INFINITE_BACK_USAGE)) {
                    for (TeleportType t : TeleportType.values()) {
                        data.clearLastTeleport(t);
                    }
                }
            }
        };

        data.teleport(lastTeleportLog.teleporter(), BACK, task);
    }
}
