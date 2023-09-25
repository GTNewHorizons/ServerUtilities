package serverutils.old.mod.cmd;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMStringUtils;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;

public class CmdMath extends CommandLM {

    private static Boolean hasEngine = null;
    private static ScriptEngine engine = null;

    public CmdMath() {
        super("math", CommandLevel.ALL);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);

        if (hasEngine == null) {
            hasEngine = Boolean.FALSE;

            try {
                engine = new ScriptEngineManager().getEngineByName("JavaScript");
                if (engine != null) hasEngine = Boolean.TRUE;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (hasEngine == Boolean.TRUE) {
            try {
                String s = LMStringUtils.unsplit(args, " ").trim();
                Object o = engine.eval(s);
                return new ChatComponentText(String.valueOf(o));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            return error(new ChatComponentText("JavaScript Engine not found!"));
        }

        return error(new ChatComponentText("Error"));
    }
}
