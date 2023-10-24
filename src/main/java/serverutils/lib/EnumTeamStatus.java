package serverutils.lib;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.BiFunction;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.util.IStringSerializable;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.NameMap;

public enum EnumTeamStatus implements IStringSerializable {

    ENEMY(-10, "enemy", EnumChatFormatting.RED, true),
    NONE(0, "none", EnumChatFormatting.WHITE, true),
    INVITED(10, "invited", EnumChatFormatting.DARK_AQUA, true),
    ALLY(30, "ally", EnumChatFormatting.DARK_GREEN, true),
    MEMBER(50, "member", EnumChatFormatting.BLUE, false),
    MOD(80, "mod", EnumChatFormatting.BLUE, true),
    OWNER(100, "owner", EnumChatFormatting.GOLD, false);

    public static final EnumTeamStatus[] VALUES = values();
    public static final BiFunction<ICommandSender, EnumTeamStatus, IChatComponent> NAME_GETTER = (sender,
            value) -> StringUtils.color(ServerUtilities.lang(sender, value.getLangKey()), value.getColor());
    public static final NameMap<EnumTeamStatus> NAME_MAP = NameMap.createWithName(NONE, NAME_GETTER, VALUES);
    public static final NameMap<EnumTeamStatus> NAME_MAP_PERMS = NameMap
            .createWithName(ALLY, NAME_GETTER, NONE, ALLY, MEMBER);
    public static final Collection<EnumTeamStatus> VALID_VALUES = new LinkedHashSet<>();

    static {
        for (EnumTeamStatus s : VALUES) {
            if (s.canBeSet) {
                VALID_VALUES.add(s);
            }
        }
    }

    private final String name;
    private final int status;
    private final EnumChatFormatting color;
    private final String langKey;
    private final boolean canBeSet;

    EnumTeamStatus(int s, String n, EnumChatFormatting c, boolean cs) {
        name = n;
        status = s;
        color = c;
        langKey = "serverutilities.lang.team_status." + name;
        canBeSet = cs;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public EnumChatFormatting getColor() {
        return color;
    }

    public String getLangKey() {
        return langKey;
    }

    public boolean canBeSet() {
        return canBeSet;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isEqualOrGreaterThan(EnumTeamStatus s) {
        return status >= s.status;
    }

    public String toString() {
        return getName();
    }
}
