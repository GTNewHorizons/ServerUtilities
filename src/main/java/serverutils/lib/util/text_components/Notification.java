package serverutils.lib.util.text_components;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.ServerUtilities;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringJoiner;

public class Notification extends ChatComponentText {

    public static final ResourceLocation VANILLA_STATUS = new ResourceLocation("minecraft", "status");

    public static Notification of(ResourceLocation id, String text, IChatComponent... lines) {
        Notification n = new Notification(id, text);

        for (IChatComponent line : lines) {
            n.addLine(line);
        }

        return n;
    }

    public static Notification of(ResourceLocation id, IChatComponent... lines) {
        return of(id, "", lines);
    }

    public static Notification of(String id, IChatComponent... lines) {
        return of(new ResourceLocation(ServerUtilities.MOD_ID, id), "", lines);
    }

    private final ResourceLocation id;
    private Ticks timer;
    private boolean important;
    private boolean vanilla;

    private Notification(ResourceLocation i, String text) {
        super(text);
        id = i;
        timer = Ticks.SECOND.x(3);
        important = false;
        vanilla = false;
    }

    public Notification(Notification n) {
        this(n.getId(), n.getUnformattedTextForChat());

        setChatStyle(n.getChatStyle().createShallowCopy());

        for (IChatComponent line : n.getSiblings()) {
            getSiblings().add(line.createCopy());
        }

        setTimer(n.getTimer());
        setImportant(n.isImportant());
        setVanilla(n.isVanilla());
    }

    public Notification addLine(IChatComponent line) {
        if (!getSiblings().isEmpty()) {
            appendText("\n");
        }

        appendSibling(line);
        return this;
    }

    public Notification setError() {
        getChatStyle().setColor(EnumChatFormatting.DARK_RED);
        important = true;
        return this;
    }

    @Override
    public Notification appendText(String text) {
        return appendSibling(new ChatComponentText(text));
    }

    @Override
    public Notification appendSibling(IChatComponent component) {
        super.appendSibling(component);
        return this;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof Notification notification
                && notification.getId().toString().equals(getId().toString()));
    }

    @Override
    public String toString() {
        return "Notification{" + StringJoiner.with(", ").joinObjects(
                "id=" + id,
                "siblings=" + siblings,
                "style=" + getChatStyle(),
                "timer=" + timer,
                "important=" + important) + '}';
    }

    public ResourceLocation getId() {
        return id;
    }

    public Ticks getTimer() {
        return timer;
    }

    public Notification setTimer(Ticks t) {
        timer = t;
        return this;
    }

    public boolean isImportant() {
        return important;
    }

    public boolean isVanilla() {
        return vanilla;
    }

    public Notification setImportant(boolean v) {
        important = v;
        return this;
    }

    public Notification setVanilla(boolean v) {
        vanilla = v;
        return this;
    }

    @Override
    public Notification createCopy() {
        return new Notification(this);
    }

    public void sendToAll() {
        send(null);
    }

    public void send(@Nullable EntityPlayer player) {
        ServerUtils.notify(player, this);
    }
}
