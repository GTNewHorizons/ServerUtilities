package serverutils.lib.lib.config;

@FunctionalInterface
public interface IIteratingConfig {

    ConfigValue getIteration(boolean next);
}
