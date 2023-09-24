package serverutils.serverlib.net;

import serverutils.serverlib.ServerLibCommon;
import serverutils.serverlib.lib.data.Action;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.gui.misc.GuiActionList;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author LatvianModder
 */
public class MessageMyTeamGuiResponse extends MessageToClient
{
	private ITextComponent title;
	private Collection<Action.Inst> actions;

	public MessageMyTeamGuiResponse()
	{
	}

	public MessageMyTeamGuiResponse(ForgePlayer player)
	{
		title = player.team.getTitle();
		actions = new ArrayList<>();
		NBTTagCompound emptyData = new NBTTagCompound();

		for (Action action : ServerLibCommon.TEAM_GUI_ACTIONS.values())
		{
			Action.Type type = action.getType(player, emptyData);

			if (type.isVisible())
			{
				actions.add(new Action.Inst(action, type));
			}
		}
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.MY_TEAM;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeTextComponent(title);
		data.writeCollection(actions, Action.Inst.SERIALIZER);
	}

	@Override
	public void readData(DataIn data)
	{
		title = data.readTextComponent();
		actions = data.readCollection(Action.Inst.DESERIALIZER);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		new GuiActionList(title.getFormattedText(), actions, id -> new MessageMyTeamAction(id, new NBTTagCompound()).sendToServer()).openGui();
	}
}