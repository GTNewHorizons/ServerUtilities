package serverutils.lib.command;

import java.util.function.Consumer;

import net.minecraft.command.ICommandSender;

public class CmdSimple extends CmdBase {

    private final Consumer<ICommandSender> consumer;

    public CmdSimple(String name, Level level, Consumer<ICommandSender> consumer) {
        super(name, level);
        this.consumer = consumer;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        consumer.accept(sender);
    }
}
