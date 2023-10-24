package serverutils.client.resource;

import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Handles reload parameters for selective loaders.
 */
public enum SelectiveReloadStateHandler {

    INSTANCE;

    @Nullable
    private Predicate<IResourceType> currentPredicate = null;

    /***
     * Pushes a resource type predicate for the current reload. Should only be called when initiating a resource reload.
     * If a reload is already in progress when this is called, an exception will be thrown.
     *
     * @param resourcePredicate the resource requirement predicate for the current reload
     */
    public void beginReload(Predicate<IResourceType> resourcePredicate) {
        if (this.currentPredicate != null) {
            throw new IllegalStateException("Recursive resource reloading detected");
        }

        this.currentPredicate = resourcePredicate;
    }

    /**
     * Gets the current reload resource predicate for the initiated reload.
     *
     * @return the active reload resource predicate, or an accepting one if none in progress
     */
    public Predicate<IResourceType> get() {
        // if (this.currentPredicate == null || !ForgeModContainer.selectiveResourceReloadEnabled) {
        // return ReloadRequirements.all();
        // }

        // return this.currentPredicate;

        return ReloadRequirements.all();
    }

    /**
     * Finishes the current reload and deletes the previously added reload predicate.
     */
    public void endReload() {
        this.currentPredicate = null;
    }
}
