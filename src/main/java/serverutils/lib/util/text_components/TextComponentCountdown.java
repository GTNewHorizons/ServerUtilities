package serverutils.lib.util.text_components;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringJoiner;

public class TextComponentCountdown extends ChatComponentText {

    public final long countdown;

    public TextComponentCountdown(String s, long t) {
        super(s);
        countdown = t;
    }

    public TextComponentCountdown(long t) {
        this("", t);
    }

    @Override
    public String getChatComponentText_TextValue() {
        return Ticks.get(countdown - ServerUtilities.PROXY.getWorldTime() - (countdown % 20L)).toTimeString();
    }

    @Override
    public String getUnformattedTextForChat() {
        return getChatComponentText_TextValue();
    }

    @Override
    public TextComponentCountdown createCopy() {
        TextComponentCountdown component = new TextComponentCountdown(countdown);
        component.setChatStyle(getChatStyle().createShallowCopy());

        for (IChatComponent IChatComponent : getSiblings()) {
            component.appendSibling(IChatComponent.createCopy());
        }

        return component;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof TextComponentCountdown) {
            TextComponentCountdown t = (TextComponentCountdown) o;
            return countdown == t.countdown && getChatStyle().equals(t.getChatStyle())
                    && getSiblings().equals(t.getSiblings());
        } else if (o instanceof ChatComponentText) {
            ChatComponentText t = (ChatComponentText) o;
            return getChatComponentText_TextValue().equals(t.getChatComponentText_TextValue())
                    && getChatStyle().equals(t.getChatStyle())
                    && getSiblings().equals(t.getSiblings());
        }

        return false;
    }

    public String toString() {
        return "CountdownComponent{" + StringJoiner.with(", ").joinObjects(
                "countdown=" + countdown,
                "text=" + getChatComponentText_TextValue(),
                "siblings=" + siblings,
                "style=" + getChatStyle()) + '}';
    }
}
