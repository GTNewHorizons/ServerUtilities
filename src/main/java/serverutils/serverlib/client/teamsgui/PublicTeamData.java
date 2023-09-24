package serverutils.serverlib.client.teamsgui;

import serverutils.serverlib.lib.EnumTeamColor;
import serverutils.serverlib.lib.data.ForgeTeam;
import serverutils.serverlib.lib.icon.Icon;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.util.FinalIDObject;
import net.minecraft.util.IChatComponent;

public class PublicTeamData extends FinalIDObject implements Comparable<PublicTeamData> {
	public enum Type {
		CAN_JOIN,
		REQUESTING_INVITE,
		NEEDS_INVITE,
		ENEMY
	}

	public static final DataOut.Serializer<PublicTeamData> SERIALIZER = (data, d) -> {
		data.writeString(d.getId());
		data.writeTextComponent(d.displayName);
		data.writeString(d.description);
		EnumTeamColor.NAME_MAP.write(data, d.color);
		data.writeIcon(d.icon);
		data.writeByte(d.type.ordinal());
	};

	public static final DataIn.Deserializer<PublicTeamData> DESERIALIZER = PublicTeamData::new;

	public final IChatComponent displayName;
	public final String description;
	public final EnumTeamColor color;
	public final Icon icon;
	public Type type;

	public PublicTeamData(DataIn data) {
		super(data.readString());
		displayName = data.readTextComponent();
		description = data.readString();
		color = EnumTeamColor.NAME_MAP.read(data);
		icon = data.readIcon();
		type = Type.values()[data.readUnsignedByte()];
	}

	public PublicTeamData(ForgeTeam team, Type c) {
		super(team.getId());
		displayName = team.getTitle();
		description = team.getDesc();
		color = team.getColor();
		icon = team.getIcon();
		type = c;
	}

	@Override
	public int compareTo(PublicTeamData o) {
		int i = type.compareTo(o.type);

		if (i == 0) {
			i = displayName.getUnformattedText().compareToIgnoreCase(o.displayName.getUnformattedText());
		}
		return i;
	}
}