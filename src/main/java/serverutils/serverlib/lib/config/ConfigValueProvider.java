package serverutils.serverlib.lib.config;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface ConfigValueProvider extends Supplier<ConfigValue>
{
}