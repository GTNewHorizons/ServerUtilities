package serverutils.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesCommon;
import serverutils.data.NodeEntry;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigDouble;
import serverutils.lib.config.ConfigInt;
import serverutils.lib.config.ConfigTimer;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.config.RankConfigValueInfo;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.permission.DefaultPermissionHandler;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.ranks.ICommandWithPermission;

public class CmdDumpPermissions extends CmdBase {

    public CmdDumpPermissions() {
        super("dump_permissions", Level.OP_OR_SP);
    }

    private static final String[] EMPTY_ROW = { "", "", "", "", "", "" };

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        File permFile = MinecraftServer.getServer().getFile(ServerUtilities.SERVER_FOLDER + "all_permissions.txt");
        List<NodeEntry> permNodes = new ArrayList<>(ServerUtilitiesCommon.CUSTOM_PERM_PREFIX_REGISTRY);
        FileUtils.delete(permFile);

        int nodeCount = 0;

        for (String s : PermissionAPI.getPermissionHandler().getRegisteredNodes()) {
            DefaultPermissionLevel level = DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(s);
            String desc = PermissionAPI.getPermissionHandler().getNodeDescription(s);

            boolean printNode = true;

            for (NodeEntry cprefix : ServerUtilitiesCommon.CUSTOM_PERM_PREFIX_REGISTRY) {
                if (s.startsWith(cprefix.getNode())) {
                    if (cprefix.level != null && level == cprefix.level && desc.isEmpty()) {
                        printNode = false;
                    }
                    break;
                }
            }
            if (printNode) {
                permNodes.add(new NodeEntry(s, level, desc));
            }
        }

        for (RankConfigValueInfo info : RankConfigAPI.getHandler().getRegisteredConfigs()) {
            String desc = new ChatComponentTranslation("permission." + info.node).getUnformattedText();
            permNodes.add(
                    new NodeEntry(
                            info.node,
                            info.defaultValue,
                            info.defaultOPValue,
                            desc.equals(info.node) ? "" : desc,
                            null));
        }

        List<List<String>> export = new ArrayList<>();
        export.add(Arrays.asList("Node", "Description", "Type", "Player default", "OP default", "Variants"));
        export.add(Arrays.asList(EMPTY_ROW));

        for (NodeEntry entry : permNodes) {
            List<String> variants = new ArrayList<>();
            if (entry.player instanceof ConfigBoolean) {
                variants.add("true");
                variants.add("false");
            } else if (entry.player instanceof ConfigInt configInt) {
                int min = configInt.getMin();
                int max = configInt.getMax();
                variants.add(
                        String.format(
                                "%s to %s",
                                min == Integer.MIN_VALUE ? "∞" : String.valueOf(min),
                                max == Integer.MAX_VALUE ? "∞" : String.valueOf(max)));
            } else if (entry.player instanceof ConfigDouble configDouble) {
                double min = configDouble.getMin();
                double max = configDouble.getMax();
                variants.add(
                        String.format(
                                "%s to %s",
                                min == Double.NEGATIVE_INFINITY ? "∞" : StringUtils.formatDouble(min),
                                max == Double.POSITIVE_INFINITY ? '∞' : StringUtils.formatDouble(max)));
            } else if (entry.player instanceof ConfigTimer configTimer) {
                Ticks max = configTimer.getMax();
                variants.add(String.format("0s to %s", !max.hasTicks() ? "∞" : max.toString()));
            } else {
                variants = new ArrayList<>(entry.player.getVariants());
                variants.sort(StringUtils.IGNORE_CASE_COMPARATOR);
            }

            export.add(
                    Arrays.asList(
                            entry.getNode(),
                            entry.desc,
                            entry.player.getId(),
                            entry.player.getStringForGUI().getUnformattedText(),
                            entry.op.getStringForGUI().getUnformattedText(),
                            variants.toString()));
        }

        List<String> permFileOut = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        int[] maxLength = new int[6];
        for (List<String> l : export) {
            for (int i = 0; i < maxLength.length; i++) {
                maxLength[i] = Math.max(maxLength[i], l.get(i).length());
            }
        }

        for (List<String> l : export) {
            builder.setLength(0);
            nodeCount++;
            for (int i = 0; i < maxLength.length; i++) {
                if (i > 0) {
                    builder.append(" | ");
                }
                builder.append(StringUtils.fillString(l.get(i), ' ', maxLength[i]));
            }
            permFileOut.add(builder.toString());
        }

        permFileOut.subList(2, permFileOut.size()).sort(StringUtils.IGNORE_CASE_COMPARATOR);
        permFileOut.add('\n' + "Command Permissions");
        int preSize = permFileOut.size();

        List<List<String>> commandList = new ArrayList<>();
        commandList.add(Arrays.asList("Node", "Command", "Default Permission", "Usage"));
        commandList.add(Arrays.asList(EMPTY_ROW));

        for (ICommand command : CommandUtils.getAllCommands(sender)) {
            if (command instanceof ICommandWithPermission cmd) {
                String node = cmd.serverutilities$getPermissionNode();
                DefaultPermissionLevel defaultPermissionLevel = DefaultPermissionHandler.INSTANCE
                        .getDefaultPermissionLevel(node);
                IChatComponent usage = CommandUtils.getTranslatedUsage(command, sender);

                commandList.add(
                        Arrays.asList(
                                node,
                                "/" + command.getCommandName(),
                                defaultPermissionLevel.name(),
                                usage.getUnformattedText()));
            }
        }

        int[] cmdMaxLength = new int[4];
        for (List<String> l : commandList) {
            for (int i = 0; i < cmdMaxLength.length; i++) {
                cmdMaxLength[i] = Math.max(cmdMaxLength[i], l.get(i).length());
            }
        }

        for (List<String> l : commandList) {
            builder.setLength(0);
            nodeCount++;
            for (int i = 0; i < cmdMaxLength.length; i++) {
                if (i > 0) {
                    builder.append(" | ");
                }
                builder.append(StringUtils.fillString(l.get(i), ' ', cmdMaxLength[i]));
            }
            // Filters out one wrong permission node
            if (!l.get(0).equals("command")) {
                permFileOut.add(builder.toString());
            }
        }

        permFileOut.subList(preSize + 2, permFileOut.size()).sort(StringUtils.IGNORE_CASE_COMPARATOR);
        FileUtils.saveSafe(permFile, permFileOut);
        sender.addChatMessage(
                ServerUtilities.lang(sender, "serverutilities.lang.permissions_dumped", nodeCount, permFile.getPath()));
    }
}
