package serverutils.command.pregen;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.misc.PregeneratorCommandInfo;
import serverutils.pregenerator.ChunkLoaderManager;

import java.io.IOException;
import java.util.Objects;

public class CmdStart extends CmdBase {
    public CmdStart() {
        super("start", Level.OP);
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        checkArgs(sender, args, 3);
        if (args.length > 2)
        {
            double xLoc, zLoc;
            if (args[0].equals("~"))
            {
                xLoc = sender.getPlayerCoordinates().posX;
            }
            else
            {
                xLoc = parseDoubleBounded(sender, args[0], -30000000.0D, 30000000.0D);
            }

            if (args[1].equals("~"))
            {
                zLoc = sender.getPlayerCoordinates().posZ;
            }
            else
            {
                zLoc = parseDoubleBounded(sender, args[1], -30000000.0D, 30000000.0D);
            }

            int radius = parseInt(sender, args[2]);
            if (radius > 2000)
            {
                sender.addChatMessage(new ChatComponentText("Radii larger than 2000 are not permitted. World sizes will be 100's of gbs"));
                return;
            }

            int dimensionID = sender.getEntityWorld().provider.dimensionId;
            PregeneratorCommandInfo commandInfo = new PregeneratorCommandInfo(xLoc, zLoc, radius, dimensionID);
            if (!ChunkLoaderManager.instance.isGenerating())
            {
                try
                {
                    ChunkLoaderManager.instance.initializePregenerator(commandInfo, MinecraftServer.getServer());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    sender.addChatMessage(new ChatComponentText("Cannot start a pregenerator! File exception when starting pregenerator!"));
                }
            }
            else
            {
                sender.addChatMessage(new ChatComponentText("Cannot start a pregenerator! There's already generation in progress!"));
            }
        }
    }
    
    @Override
    public void checkArgs(ICommandSender sender, String[] args, int i)
    {
        if (args.length < i) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if (sender instanceof MinecraftServer)
        {
            for (int j = 0; i < args.length; i++)
            {
                if (Objects.equals(args[i], "~"))
                {
                    throw new WrongUsageException(getCommandUsage(sender));
                }
            }
        }
    }
}
