package serverutils.lib.icon;

import java.awt.Image;

import javax.annotation.Nullable;

@FunctionalInterface
public interface ImageCallback {

    void imageLoaded(boolean queued, @Nullable Image image);
}
