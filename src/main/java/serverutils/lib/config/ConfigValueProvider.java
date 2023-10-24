package serverutils.lib.config;

import java.util.function.Supplier;

@FunctionalInterface
public interface ConfigValueProvider extends Supplier<ConfigValue> {
}
