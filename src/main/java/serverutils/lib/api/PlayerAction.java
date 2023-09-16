package serverutils.lib.api;

import java.util.List;

import latmod.lib.util.FinalIDObject;
import serverutils.lib.TextureCoords;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.friends.ILMPlayer;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;

public abstract class PlayerAction extends FinalIDObject {

    public static enum Type {

        SELF,
        OTHER,
        BOTH;

        public boolean self() {
            return this == SELF || this == BOTH;
        }

        public boolean other() {
            return this == OTHER || this == BOTH;
        }

        public boolean equalsType(Type t) {
            if (t == null || t == BOTH) return true;
            else if (t == SELF) return self();
            else return other();
        }
    }

    public final Type type;
    public final int priority;
    public final TextureCoords icon;

    public PlayerAction(Type t, String id, int p, TextureCoords c) {
        super(id);
        type = (t == null) ? Type.SELF : t;
        priority = p;
        icon = c;
    }

    public abstract void onClicked(ILMPlayer self, ILMPlayer other);

    public String getDisplayName() {
        return ServerUtilitiesLibraryMod.proxy.translate("player_action." + getID());
    }

    public void addMouseOverText(List<String> l) {}

    public void render(int ax, int ay, double z) {
        ServerUtilitiesLibraryClient.setTexture(icon);
        GuiLM.drawTexturedRectD(ax, ay, z, 16, 16, icon.minU, icon.minV, icon.maxU, icon.maxV);
    }

    public void postRender(int ax, int ay, double z) {}

    public int compareTo(Object o) {
        int i = Integer.compare(((PlayerAction) o).priority, priority);
        return (i == 0) ? super.compareTo(o) : i;
    }

    public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
        return true;
    }

    public Boolean configDefault() {
        return null;
    }
}
