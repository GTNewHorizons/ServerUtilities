package ftb.lib;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class BroadcastSender implements ICommandSender {

    public static final BroadcastSender inst = new BroadcastSender();

    public String getCommandSenderName() {
        return "[Server]";
    }

    public IChatComponent func_145748_c_() {
        return null;
    }

    public void addChatMessage(IChatComponent component) {
        FTBLib.getServer().getConfigurationManager().sendChatMsgImpl(component, true);
    }

    public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
        return true;
    }

    public ChunkCoordinates getPlayerCoordinates() {
        return FTBLib.getServerWorld().getSpawnPoint();
    }

    public World getEntityWorld() {
        return FTBLib.getServerWorld();
    }
}
