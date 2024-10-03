package serverutils.command.pregen;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import serverutils.lib.command.CmdBase;
import serverutils.pregenerator.ChunkLoaderManager;

public class CmdProgress extends CmdBase {

    public CmdProgress() {
        super("progress", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (ChunkLoaderManager.instance.isGenerating()) {
            sender.addChatMessage(new ChatComponentText(ChunkLoaderManager.instance.progressString()));
        } else {
            sender.addChatMessage(new ChatComponentText("No generator running."));
        }
    }
}
