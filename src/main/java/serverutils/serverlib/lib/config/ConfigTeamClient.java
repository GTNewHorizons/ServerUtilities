package serverutils.serverlib.lib.config;

import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.gui.IOpenableGui;
import serverutils.serverlib.lib.gui.misc.GuiSelectTeamValue;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.icon.Icon;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.util.FinalIDObject;
import serverutils.serverlib.lib.util.misc.MouseButton;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ConfigTeamClient extends ConfigString
{
	public static class TeamInst extends FinalIDObject
	{
		public final short uid;
		public ITextComponent title;
		public Icon icon;

		public TeamInst(short u, String id)
		{
			super(id);
			uid = u;
		}
	}

	public final Map<String, TeamInst> map;

	public ConfigTeamClient(String v)
	{
		super(v, ForgeTeam.TEAM_ID_PATTERN);
		map = new HashMap<>();
	}

	@Override
	public String getId()
	{
		return ConfigTeam.TEAM_ID;
	}

	@Override
	public ITextComponent getStringForGUI()
	{
		TeamInst inst = map.get(getString());

		if (inst != null)
		{
			return inst.title.createCopy();
		}

		return super.getStringForGUI();
	}

	@Override
	public ConfigTeamClient copy()
	{
		ConfigTeamClient config = new ConfigTeamClient(getString());

		for (TeamInst inst : map.values())
		{
			TeamInst inst1 = new TeamInst(inst.uid, inst.getId());
			inst1.title = inst.title.createCopy();
			inst1.icon = inst.icon.copy();
			config.map.put(inst1.getId(), inst1);
		}

		return config;
	}

	@Override
	public Color4I getColor()
	{
		return Color4I.LIGHT_GREEN;
	}

	@Override
	public void addInfo(ConfigValueInstance inst, List<String> list)
	{
	}

	@Override
	public List<String> getVariants()
	{
		return new ArrayList<>(map.keySet());
	}

	@Override
	public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback)
	{
		if (inst.getCanEdit())
		{
			new GuiSelectTeamValue(this, gui, callback).openGui();
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String key)
	{
		nbt.setString(key, getString());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String key)
	{
		NBTBase id = nbt.getTag(key);

		if (id instanceof NBTTagString)
		{
			setString(((NBTTagString) id).getString());
		}
		else if (id instanceof NBTPrimitive)
		{
			ForgeTeam team = null;
			short ids = ((NBTPrimitive) id).getShort();

			for (TeamInst inst : map.values())
			{
				if (inst.uid == ids)
				{
					setString(inst.getId());
					return;
				}
			}
		}
	}

	@Override
	public void writeData(DataOut data)
	{
		throw new IllegalStateException("Can't write Team client property!");
	}

	@Override
	public void readData(DataIn data)
	{
		int s = data.readVarInt();
		map.clear();

		for (int i = 0; i < s; i++)
		{
			short uid = data.readShort();
			String id = data.readString();
			TeamInst inst = new TeamInst(uid, id);
			inst.title = data.readTextComponent();
			inst.icon = data.readIcon();
			map.put(inst.getId(), inst);
		}

		setString(data.readString());
	}
}