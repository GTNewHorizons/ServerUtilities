package serverutils.aurora.page;

import io.netty.handler.codec.http.HttpResponseStatus;
import serverutils.aurora.PageType;

public interface WebPage {

    String getContent();

    default String getContentType() {
        return "text/html";
    }

    default HttpResponseStatus getStatus() {
        return HttpResponseStatus.OK;
    }

    default PageType getPageType() {
        return PageType.ENABLED;
    }
}
