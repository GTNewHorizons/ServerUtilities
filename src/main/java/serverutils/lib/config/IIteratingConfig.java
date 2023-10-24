package serverutils.lib.config;

@FunctionalInterface
public interface IIteratingConfig {

    ConfigValue getIteration(boolean next);
}
