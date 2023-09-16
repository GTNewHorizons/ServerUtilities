package ftb.lib.api.cmd;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

/**
 * Created by LatvianModder on 23.02.2016.
 */
public interface ICustomCommandInfo {

    void addInfo(List<IChatComponent> list, ICommandSender sender);
}
