package serverutils.command.pregen;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import serverutils.lib.command.CmdBase;
import serverutils.pregenerator.ChunkLoaderManager;

public class CmdStop extends CmdBase {

    public CmdStop() {
        super("stop", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (ChunkLoaderManager.instance.isGenerating()) {
            ChunkLoaderManager.instance.reset(true);
            sender.addChatMessage(new ChatComponentText("Cancelling pregeneration."));
        } else {
            sender.addChatMessage(new ChatComponentText("No generator running."));
        }
    }
}
