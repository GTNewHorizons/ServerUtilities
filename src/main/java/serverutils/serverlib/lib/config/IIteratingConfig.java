package serverutils.serverlib.lib.config;

@FunctionalInterface
public interface IIteratingConfig
{
	ConfigValue getIteration(boolean next);
}