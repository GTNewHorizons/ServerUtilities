package serverutils.aurora.page;

import java.io.InputStream;

import serverutils.aurora.tag.PairedTag;
import serverutils.aurora.tag.Tag;
import serverutils.lib.util.StringUtils;

public abstract class HTTPWebPage implements WebPage {

    private static String css = "";

    @Override
    public String getContent() {
        Tag http = new PairedTag("html");
        Tag head = http.paired("head");
        head.unpaired("meta").attr("charset", "UTF-8");
        head(head);
        Tag body = http.paired("body");
        body(body);

        if (addBackButton()) {
            body.h3("").a("< Back to Aurora index page", "/");
        }

        return http.getContent();
    }

    public String getTitle() {
        return "Aurora";
    }

    public String getDescription() {
        return "";
    }

    public String getStylesheet() {
        return "/assets/serverutilities/style.css";
    }

    public String getIcon() { // TODO: Change this to the local one in resources
        return "https://latvian.dev/logo.svg"; // Taken from Latvian.dev since the aurora part is permanently down
    }

    public boolean addBackButton() {
        return true;
    }

    public void head(Tag head) {
        String d = getDescription();

        if (!d.isEmpty()) {
            head.paired("title", getTitle() + " - " + d);
            head.meta("description", d);
        } else {
            head.paired("title", getTitle());
        }

        head.unpaired("link").attr("rel", "icon").attr("href", getIcon());

        try {
            if (css.isEmpty()) {
                InputStream is = HTTPWebPage.class.getResourceAsStream(getStylesheet());
                css = StringUtils.readString(is);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!css.isEmpty()) {
            head.unpaired("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", css);
        }

        head.meta("viewport", "width=device-width, initial-scale=1.0");
    }

    public abstract void body(Tag body);
}
