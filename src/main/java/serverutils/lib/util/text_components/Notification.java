package serverutils.lib.util.text_components;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

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

    private final ResourceLocation id;
    private Ticks timer;
    private boolean important;

    private Notification(ResourceLocation i, String text) {
        super(text);
        id = i;
        timer = Ticks.SECOND.x(3);
        important = false;
    }

    public Notification(Notification n) {
        this(n.getId(), n.getUnformattedTextForChat());

        setChatStyle(n.getChatStyle().createShallowCopy());

        for (IChatComponent line : n.getSiblings()) {
            getSiblings().add(line.createCopy());
        }

        setTimer(n.getTimer());
        setImportant(n.isImportant());
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

    public int hashCode() {
        return id.hashCode();
    }

    public boolean equals(Object o) {
        return o == this || (o instanceof Notification notification && notification.getId().equals(getId()));
    }

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

    public Notification setImportant(boolean v) {
        important = v;
        return this;
    }

    @Override
    public Notification createCopy() {
        return new Notification(this);
    }

    public void send(MinecraftServer server, @Nullable EntityPlayer player) {
        ServerUtils.notify(server, player, this);
    }

    public void send(MinecraftServer server, @Nullable ICommandSender sender) {
        if (sender == null) {
            ServerUtils.notify(server, null, this);
        } else if (sender instanceof EntityPlayer) {
            ServerUtils.notify(server, (EntityPlayer) sender, this);
        } else {
            sender.addChatMessage(this);
        }
    }
}
