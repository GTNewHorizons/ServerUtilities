package serverutils.lib.util.text_components;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentStyle;
import net.minecraft.util.IChatComponent;

public class ChatComponentScore extends ChatComponentStyle {

    public final String name;
    public final String objective;

    public String value = "";

    @Override
    public String getUnformattedTextForChat() {
        return this.value;
    }

    @Override
    public ChatComponentScore createCopy() {
        ChatComponentScore score = new ChatComponentScore(this.name, this.objective);
        score.setValue(this.value);
        score.setChatStyle(this.getChatStyle().createShallowCopy());

        for (IChatComponent component : this.getSiblings()) {
            score.appendSibling(component.createCopy());
        }
        return score;
    }

    public ChatComponentScore(String name, String objective) {
        this.name = name;
        this.objective = objective;
    }

    public String getName() {
        return this.name;
    }

    public String getObjective() {
        return this.objective;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void resolve(ICommandSender sender) {

    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof ChatComponentScore score)) {
            return false;
        } else {
            return this.name.equals(score.name) && this.objective.equals(score.objective) && super.equals(object);
        }
    }

    @Override
    public String toString() {
        return "ScoreComponent{name='" + this.name
                + '\''
                + "objective='"
                + this.objective
                + '\''
                + ", siblings="
                + this.siblings
                + ", style="
                + this.getChatStyle()
                + '}';
    }
}
