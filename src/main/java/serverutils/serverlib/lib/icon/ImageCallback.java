package serverutils.serverlib.lib.icon;

import javafx.scene.image.Image;

import javax.annotation.Nullable;

@FunctionalInterface
public interface ImageCallback
{
	void imageLoaded(boolean queued, @Nullable Image image);
}