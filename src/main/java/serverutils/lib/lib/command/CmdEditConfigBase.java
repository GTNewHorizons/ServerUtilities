package serverutils.lib.lib.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibConfig;
import serverutils.lib.lib.config.ConfigGroup;
import serverutils.lib.lib.config.ConfigValue;
import serverutils.lib.lib.config.ConfigValueInstance;
import serverutils.lib.lib.config.IConfigCallback;
import serverutils.lib.lib.data.ServerUtilitiesLibAPI;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.text_components.Notification;

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
            ServerUtilitiesLibAPI
                    .editServerConfig(getCommandSenderAsPlayer(sender), getGroup(sender), getCallback(sender));
            return;
        }

        checkArgs(sender, args, 1);
        ConfigGroup group = getGroup(sender);
        ConfigValueInstance instance = group.getValueInstance(args[0]);

        if (instance == null) {
            throw ServerUtilitiesLib.error(sender, "serverlib.lang.config_command.invalid_key", args[0]);
        }

        if (args.length >= 2) {
            String valueString = String.valueOf(StringUtils.joinSpaceUntilEnd(1, args));

            if (!instance.getValue().setValueFromString(sender, valueString, true)) {
                return;
            }

            if (ServerUtilitiesLibConfig.debugging.log_config_editing) {
                ServerUtilitiesLib.LOGGER.info("Setting " + instance.getPath() + " to " + valueString);
            }

            instance.getValue().setValueFromString(sender, valueString, false);
            getCallback(sender).onConfigSaved(group, sender);
            Notification
                    .of(
                            Notification.VANILLA_STATUS,
                            ServerUtilitiesLib.lang(
                                    sender,
                                    "serverlib.lang.config_command.set",
                                    instance.getDisplayName(),
                                    group.getValue(args[0]).toString()))
                    .send(getCommandSenderAsPlayer(sender).mcServer, getCommandSenderAsPlayer(sender));
        }

        sender.addChatMessage(instance.getValue().getStringForGUI());
    }
}