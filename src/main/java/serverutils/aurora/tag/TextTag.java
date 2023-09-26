package serverutils.aurora.tag;

public class TextTag extends TagBase {

    private String text;

    public TextTag(String s) {
        text = s;
    }

    @Override
    public boolean isEmpty() {
        return text.isEmpty();
    }

    @Override
    public void build(StringBuilder builder) {
        if (!text.isEmpty()) {
            builder.append(text.replace("<", "&lt;").replace(">", "&gt;"));
        }
    }
}
