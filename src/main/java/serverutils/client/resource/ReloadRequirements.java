package serverutils.client.resource;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Sets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Holds methods to create standard predicates to select {@link IResourceType}s that should be reloaded.
 */
@SideOnly(Side.CLIENT)
public final class ReloadRequirements {

    /**
     * Creates a reload predicate accepting all resource types.
     *
     * @return a predicate accepting all types
     */
    public static Predicate<IResourceType> all() {
        return type -> true;
    }

    /**
     * Creates an inclusive reload predicate. Only given resource types will be loaded along with this.
     *
     * @param inclusion the set of resource types to be included in the reload
     * @return an inclusion predicate based on the given types
     */
    public static Predicate<IResourceType> include(IResourceType... inclusion) {
        Set<IResourceType> inclusionSet = Sets.newHashSet(inclusion);
        return inclusionSet::contains;
    }
}
