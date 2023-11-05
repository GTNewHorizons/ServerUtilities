package serverutils.aurora.page;

import io.netty.handler.codec.http.HttpResponseStatus;
import serverutils.aurora.tag.Tag;

public class WebPageNotFound extends HTTPWebPage {

    private final String uri;

    public WebPageNotFound(String u) {
        uri = u;
    }

    @Override
    public String getDescription() {
        return "Page not found";
    }

    @Override
    public void body(Tag body) {
        Tag text = body.h1("");
        text.text("Error! Page ");
        text.span(uri, "error");
        text.text(" not found!");
    }

    @Override
    public HttpResponseStatus getStatus() {
        return HttpResponseStatus.NOT_FOUND;
    }
}
