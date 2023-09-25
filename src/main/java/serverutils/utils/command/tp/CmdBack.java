package serverutils.utils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.util.permission.PermissionAPI;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.FTBUtilitiesPlayerData;
import serverutils.utils.data.TeleportLog;
import serverutils.utils.data.TeleportType;

public class CmdBack extends CmdBase {

    public CmdBack() {
        super("back", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ForgePlayer p = CommandUtils.getForgePlayer(player);

        FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(p);

        TeleportLog lastTeleportLog = data.getLastTeleportLog();

        if (lastTeleportLog == null) {
            throw ServerUtilities.error(sender, "ftbutilities.lang.warps.no_dp");
        }

        data.checkTeleportCooldown(sender, FTBUtilitiesPlayerData.Timer.BACK);

        FTBUtilitiesPlayerData.Timer.BACK.teleport(player, playerMP -> lastTeleportLog.teleporter(), universe -> {
            if (!PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.INFINITE_BACK_USAGE)) {
                for (TeleportType t : TeleportType.values()) {
                    data.clearLastTeleport(t);
                }
            }
        });
    }
}
