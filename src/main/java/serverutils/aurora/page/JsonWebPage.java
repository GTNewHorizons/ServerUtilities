package serverutils.aurora.page;

import com.google.gson.JsonElement;

public abstract class JsonWebPage implements WebPage {

    public abstract JsonElement getJson();

    @Override
    public String getContent() {
        return getJson().toString();
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
