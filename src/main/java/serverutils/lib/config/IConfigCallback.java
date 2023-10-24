package serverutils.lib.config;

import net.minecraft.command.ICommandSender;

@FunctionalInterface
public interface IConfigCallback {

    IConfigCallback DEFAULT = (group, sender) -> {};

    void onConfigSaved(ConfigGroup group, ICommandSender sender);
}
