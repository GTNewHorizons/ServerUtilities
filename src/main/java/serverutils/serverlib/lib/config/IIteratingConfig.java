package serverutils.serverlib.lib.config;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface IIteratingConfig
{
	ConfigValue getIteration(boolean next);
}