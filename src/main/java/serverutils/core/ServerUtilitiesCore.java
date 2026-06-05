package serverutils.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import serverutils.ServerUtilitiesConfig;
import serverutils.aurora.AuroraConfig;
import serverutils.mixin.Mixins;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class ServerUtilitiesCore implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static {
        try {
            var cleF = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            cleF.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cle = (Set<String>) cleF.get(Launch.classLoader);
            // for Brigadier
            cle.remove("com.mojang.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        ConfigurationManager.registerConfig(ServerUtilitiesConfig.class);
        ConfigurationManager.registerConfig(AuroraConfig.class);
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.serverutilities.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
