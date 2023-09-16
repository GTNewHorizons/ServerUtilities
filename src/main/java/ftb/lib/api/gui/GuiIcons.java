package ftb.lib.api.gui;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import ftb.lib.TextureCoords;

public class GuiIcons {

    public static final Map<String, TextureCoords> iconMap = new HashMap<>();

    private static final TextureCoords getIcon(String s) {
        TextureCoords t = new TextureCoords(
                new ResourceLocation("ftbl", "textures/icons/" + s + ".png"),
                0,
                0,
                16,
                16,
                16,
                16);
        iconMap.put(s, t);
        return t;
    }

    public static final TextureCoords up = getIcon("up");
    public static final TextureCoords down = getIcon("down");
    public static final TextureCoords left = getIcon("left");
    public static final TextureCoords right = getIcon("right");

    public static final TextureCoords accept = getIcon("accept");
    public static final TextureCoords add = getIcon("add");
    public static final TextureCoords remove = getIcon("remove");
    public static final TextureCoords info = getIcon("info");
    public static final TextureCoords accept_gray = getIcon("accept_gray");
    public static final TextureCoords add_gray = getIcon("add_gray");
    public static final TextureCoords remove_gray = getIcon("remove_gray");
    public static final TextureCoords info_gray = getIcon("info_gray");
    public static final TextureCoords settings = getIcon("settings");
    public static final TextureCoords cancel = getIcon("cancel");
    public static final TextureCoords back = getIcon("back");
    public static final TextureCoords close = getIcon("close");
    public static final TextureCoords refresh = getIcon("refresh");

    public static final TextureCoords player = getIcon("player");
    public static final TextureCoords player_gray = getIcon("player_gray");
    public static final TextureCoords online = getIcon("online");

    public static final TextureCoords sort_az = getIcon("sort_az");
    public static final TextureCoords friends = getIcon("friends");
    public static final TextureCoords bug = getIcon("bug");
    public static final TextureCoords jacket = getIcon("jacket");
    public static final TextureCoords bed = getIcon("bed");
    public static final TextureCoords bell = getIcon("bell");
    public static final TextureCoords compass = getIcon("compass");
    public static final TextureCoords map = getIcon("map");
    public static final TextureCoords shield = getIcon("shield");
    public static final TextureCoords art = getIcon("art");
    public static final TextureCoords money_bag = getIcon("money_bag");
    public static final TextureCoords controller = getIcon("controller");
    public static final TextureCoords feather = getIcon("feather");
    public static final TextureCoords camera = getIcon("camera");

    public static final TextureCoords[] inv = { getIcon("inv_io"), getIcon("inv_in"), getIcon("inv_out"),
            getIcon("inv_none"), };

    public static final TextureCoords[] redstone = { getIcon("rs_none"), getIcon("rs_high"), getIcon("rs_low"),
            getIcon("rs_pulse"), };

    public static final TextureCoords[] security = { getIcon("security_public"), getIcon("security_private"),
            getIcon("security_friends"), getIcon("security_group"), };

    public static final TextureCoords color_blank = getIcon("color_blank");
    public static final TextureCoords color_hsb = getIcon("color_hsb");
    public static final TextureCoords color_rgb = getIcon("color_rgb");

    public static final TextureCoords online_red = getIcon("online_red");
    public static final TextureCoords notes = getIcon("notes");
    public static final TextureCoords chat = getIcon("chat");
    public static final TextureCoords bin = getIcon("bin");
    public static final TextureCoords marker = getIcon("marker");
    public static final TextureCoords beacon = getIcon("beacon");

    public static final TextureCoords dice = getIcon("dice");
    public static final TextureCoords diamond = getIcon("diamond");
    public static final TextureCoords time = getIcon("time");
    public static final TextureCoords globe = getIcon("globe");
    public static final TextureCoords money = getIcon("money");
    public static final TextureCoords check = getIcon("check");
    public static final TextureCoords star = getIcon("star");

    public static final TextureCoords book = getIcon("book");
    public static final TextureCoords book_red = getIcon("book_red");
    public static final TextureCoords toggle_gamemode = getIcon("toggle_gamemode");
    public static final TextureCoords toggle_rain = getIcon("toggle_rain");
    public static final TextureCoords toggle_day = getIcon("toggle_day");
    public static final TextureCoords toggle_night = getIcon("toggle_night");
}
