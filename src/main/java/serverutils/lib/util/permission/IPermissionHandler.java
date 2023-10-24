package serverutils.lib.util.permission;

import java.util.Collection;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import serverutils.lib.util.permission.context.IContext;

public interface IPermissionHandler {

    /**
     * Use {@link PermissionAPI#registerNode(String, DefaultPermissionLevel, String)}
     */
    void registerNode(String node, DefaultPermissionLevel level, String desc);

    /**
     * @return Immutable collection of all registered nodes
     */
    Collection<String> getRegisteredNodes();

    /**
     * Use {@link PermissionAPI#hasPermission(GameProfile, String, IContext)}
     */
    boolean hasPermission(GameProfile profile, String node, @Nullable IContext context);

    /**
     * @param node Permission node
     * @return Description of the node. "" in case this node doesn't have a decription
     * @see #registerNode(String, DefaultPermissionLevel, String)
     */
    String getNodeDescription(String node);
}
