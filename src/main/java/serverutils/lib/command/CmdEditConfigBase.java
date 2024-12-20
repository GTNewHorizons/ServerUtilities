package serverutils.lib.command;

import static serverutils.ServerUtilitiesNotifications.CONFIG_CHANGED;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.util.StringUtils;

public abstract class CmdEditConfigBase extends CmdBase {

    public CmdEditConfigBase(String n, Level l) {
        super(n, l);
    }

    public abstract ConfigGroup getGroup(ICommandSender sender) throws CommandException;

    public IConfigCallback getCallback(ICommandSender sender) throws CommandException {
        return IConfigCallback.DEFAULT;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        try {
            ConfigGroup group = getGroup(sender);

            if (args.length == 1) {
                List<String> keys = getListOfStringsFromIterableMatchingLastWord(args, group.getValueKeyTree());

                if (keys.size() > 1) {
                    keys.sort(StringUtils.ID_COMPARATOR);
                }

                return keys;
            } else if (args.length == 2) {
                ConfigValue value = group.getValue(args[0]);

                if (!value.isNull()) {
                    List<String> variants = value.getVariants();

                    if (!variants.isEmpty()) {
                        return getListOfStringsFromIterableMatchingLastWord(args, variants);
                    }
                }

                return Collections.emptyList();
            }
        } catch (CommandException ex) {
            // IChatComponent c = new ChatComponentTranslation(ex.getMessage(), ex.getErrorObjects());
            // c.getChatStyle().setColor(EnumChatFormatting.DARK_RED);
            // sender.addChatMessage(c);
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0 && sender instanceof EntityPlayerMP) {
            ServerUtilitiesAPI
                    .editServerConfig(getCommandSenderAsPlayer(sender), getGroup(sender), getCallback(sender));
            return;
        }

        checkArgs(sender, args, 1);
        ConfigGroup group = getGroup(sender);
        ConfigValueInstance instance = group.getValueInstance(args[0]);

        if (instance == null) {
            throw ServerUtilities.error(sender, "serverutilities.lang.config_command.invalid_key", args[0]);
        }

        if (args.length >= 2) {
            String valueString = StringUtils.joinSpaceUntilEnd(1, args);

            if (!instance.getValue().setValueFromString(sender, valueString, true)) {
                return;
            }

            if (ServerUtilitiesConfig.debugging.log_config_editing) {
                ServerUtilities.LOGGER.info("Setting {} to {}", instance.getPath(), valueString);
            }

            instance.getValue().setValueFromString(sender, valueString, false);
            getCallback(sender).onConfigSaved(group, sender);
            CONFIG_CHANGED.send(
                    getCommandSenderAsPlayer(sender),
                    "serverutilities.lang.config_command.set",
                    instance.getDisplayName(),
                    group.getValue(args[0]).toString());
        }

        sender.addChatMessage(instance.getValue().getStringForGUI());
    }
}
