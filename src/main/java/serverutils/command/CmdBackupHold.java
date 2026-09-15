package serverutils.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.task.backup.ExternalBackupHold;
import serverutils.task.backup.HoldResponse;
import serverutils.task.backup.HoldResult;

/**
 * {@code /backup hold ...}, the external backup hold protocol. Replies are plain single-line ASCII rather than
 * localised chat, because the caller is a script reading an RCON response.
 */
public class CmdBackupHold extends CmdTreeBase {

    public CmdBackupHold() {
        super("hold");
        addSubcommand(new CmdHoldBegin());
        addSubcommand(new CmdHoldRenew());
        addSubcommand(new CmdHoldEnd());
        addSubcommand(new CmdHoldStatus());
    }

    private abstract static class HoldCommand extends CmdBase {

        HoldCommand(String name) {
            // Level.SERVER would reject RCON, and Level.OP would admit any opped player, so the sender is checked
            // explicitly instead. See docs/external-backup-plan.md.
            super(name, Level.OP);
        }

        abstract HoldResponse run(String[] args);

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            HoldResponse response = ExternalBackupHold.isConsoleOrRcon(sender) ? run(args)
                    : HoldResponse.of(HoldResult.DENIED);
            sender.addChatMessage(new ChatComponentText(response.toString()));
        }

        static int parseSeconds(String[] args, int index) {
            if (args.length <= index) return 0;
            try {
                return Integer.parseInt(args[index]);
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
    }

    private static class CmdHoldBegin extends HoldCommand {

        CmdHoldBegin() {
            super("begin");
        }

        @Override
        HoldResponse run(String[] args) {
            int seconds = parseSeconds(args, 0);
            if (seconds < 0) return HoldResponse.of(HoldResult.INVALID_ARGUMENT);
            return ExternalBackupHold.INSTANCE.begin(seconds);
        }
    }

    private static class CmdHoldRenew extends HoldCommand {

        CmdHoldRenew() {
            super("renew");
        }

        @Override
        HoldResponse run(String[] args) {
            if (args.length < 1) return HoldResponse.of(HoldResult.INVALID_ARGUMENT);
            int seconds = parseSeconds(args, 1);
            if (seconds < 0) return HoldResponse.of(HoldResult.INVALID_ARGUMENT);
            return ExternalBackupHold.INSTANCE.renew(args[0], seconds);
        }
    }

    private static class CmdHoldEnd extends HoldCommand {

        CmdHoldEnd() {
            super("end");
        }

        @Override
        HoldResponse run(String[] args) {
            if (args.length < 1) return HoldResponse.of(HoldResult.INVALID_ARGUMENT);
            return ExternalBackupHold.INSTANCE.end(args[0]);
        }
    }

    private static class CmdHoldStatus extends HoldCommand {

        CmdHoldStatus() {
            super("status");
        }

        @Override
        HoldResponse run(String[] args) {
            return ExternalBackupHold.INSTANCE.status();
        }
    }
}
