package serverutils.utils.mod.cmd.admin;

import java.io.File;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;

import serverutils.lib.LMNBTUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.utils.world.LMPlayerServer;

// FIXME: UNFINISHED
public class CmdResetPos extends CommandLM {

    public CmdResetPos() {
        super("reset_pos", CommandLevel.OP);
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <player>";
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);
        LMPlayerServer p = LMPlayerServer.get(args[0]);
        if (p.isOnline()) {
            return error(new ChatComponentText("Player can't be online!"));
        }

        double x, y, z;

        if (args.length >= 4) {
            EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
            x = func_110665_a(ics, ep.posX, args[1], -30000000, 30000000);
            y = func_110665_a(ics, ep.posY, args[2], -30000000, 30000000);
            z = func_110665_a(ics, ep.posZ, args[3], -30000000, 30000000);
        } else {
            ChunkCoordinates c = ServerUtilitiesLib.getServerWorld().getSpawnPoint();
            x = c.posX + 0.5D;
            y = c.posY + 0.5D;
            z = c.posZ + 0.5D;
        }

        File file = new File(
                ServerUtilitiesLib.getServerWorld().getSaveHandler().getWorldDirectory(),
                "playerdata/" + p.getProfile().getId() + ".dat");

        if (!file.exists()) {
            return error(new ChatComponentText("Cannot load the file!"));
        }

        NBTTagCompound data = LMNBTUtils.readMap(file);
        LMNBTUtils.writeMap(file, data);

        return new ChatComponentText("Reset position of " + p.getProfile().getName());
    }
}
