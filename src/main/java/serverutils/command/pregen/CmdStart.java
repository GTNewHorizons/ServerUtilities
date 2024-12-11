package serverutils.command.pregen;

import java.io.IOException;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import serverutils.lib.command.CmdBase;
import serverutils.lib.command.ICommandWithParent;
import serverutils.lib.util.misc.PregeneratorCommandInfo;
import serverutils.pregenerator.ChunkLoaderManager;

public class CmdStart extends CmdBase {

    public CmdStart() {
        super("start", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        checkArgs(sender, args, 1);

        int radius;
        double xLoc, zLoc;

        if (args.length == 3) {
            xLoc = parseDoubleBounded(sender, args[0], -30000000.0D, 30000000.0D);
            zLoc = parseDoubleBounded(sender, args[1], -30000000.0D, 30000000.0D);

            radius = parseInt(sender, args[2]);
            if (radius > 2000) {
                sender.addChatMessage(
                        new ChatComponentText(
                                "Radii larger than 2000 are not permitted. World sizes will be 100's of gbs"));
                return;
            }
        } else {
            xLoc = sender.getPlayerCoordinates().posX;
            zLoc = sender.getPlayerCoordinates().posZ;

            radius = parseInt(sender, args[0]);
            if (radius > 2000) {
                sender.addChatMessage(
                        new ChatComponentText(
                                "Radii larger than 2000 are not permitted. World sizes will be 100's of gbs"));
                return;
            }
        }

        int dimensionID = sender.getEntityWorld().provider.dimensionId;
        PregeneratorCommandInfo commandInfo = new PregeneratorCommandInfo(xLoc, zLoc, radius, dimensionID);

        if (!ChunkLoaderManager.instance.isGenerating()) {
            try {
                sender.addChatMessage(
                        new ChatComponentText("Initializing pregenerator. Check progress with '/pregen progress'."));
                ChunkLoaderManager.instance.initializePregenerator(commandInfo, MinecraftServer.getServer());
            } catch (IOException e) {
                e.printStackTrace();
                sender.addChatMessage(
                        new ChatComponentText(
                                "Cannot start a pregenerator! File exception when starting pregenerator!"));
            }
        } else {
            sender.addChatMessage(
                    new ChatComponentText("Cannot start a pregenerator! There's already generation in progress!"));
        }
    }

    @Override
    public void checkArgs(ICommandSender sender, String[] args, int i) {

        if (args.length < i) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        if (sender instanceof MinecraftServer) {
            if (args.length != 3) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
        } else {
            if (!(args.length == 1 || args.length == 3)) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
        }
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        if (sender instanceof MinecraftServer) {
            return "commands." + ICommandWithParent.getCommandPath(this) + ".usage_server";
        }

        return "commands." + ICommandWithParent.getCommandPath(this) + ".usage_client";
    }
}
