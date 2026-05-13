package serverutils.ranks;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.NodeEntry;
import serverutils.lib.command.CommandTreeBase;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;

public interface ICommandWithPermission {

    Map<String, String> commandOwners = new HashMap<>();

    Map<String, String> commandPermissions = new HashMap<>();

    String serverutilities$getPermissionNode();

    void serverutilities$setPermissionNode(String node);

    String serverutilities$getModName();

    void serverutilities$setModName(String modName);

    default String serverutilities$getModId() {
        return "";
    }

    default void serverutilities$setModId(String modId) {}

    default boolean serverutilities$hasPermission(EntityPlayerMP player) {
        return false;
    }

    default void serverUtilities$registerPermissions() {
        String node = this.serverutilities$getPermissionNode();
        DefaultPermissionLevel level = serverUtilities$getDefaultLevel();

        if (this instanceof CommandTreeBase tree) {
            for (ICommand c : tree.getSubCommands()) {
                ICommandWithPermission child = (ICommandWithPermission) c;
                child.serverutilities$setPermissionNode(node.toLowerCase() + '.' + c.getCommandName());
                child.serverutilities$setModName(this.serverutilities$getModName());
                child.serverUtilities$registerPermissions();
            }
        }

        if (Loader.instance().getLoaderState().ordinal() > LoaderState.PREINITIALIZATION.ordinal()) {
            PermissionAPI.registerNode(node, level, "");
        } else {
            ServerUtilitiesPermissions.earlyPermissions.add(new NodeEntry(node, level, ""));
        }
    }

    default DefaultPermissionLevel serverUtilities$getDefaultLevel() {
        if (this instanceof CommandBase cmdBase) {
            return cmdBase.getRequiredPermissionLevel() > 0 ? DefaultPermissionLevel.OP : DefaultPermissionLevel.ALL;
        }

        // Command permissions function as overrides, so it's fine to return OP for non-CommandBase commands
        // Only used for display purposes in the rank edit GUI
        return DefaultPermissionLevel.OP;
    }
}
