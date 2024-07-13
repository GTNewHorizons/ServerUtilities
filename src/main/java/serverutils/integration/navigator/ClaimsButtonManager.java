package serverutils.integration.navigator;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

import serverutils.ServerUtilities;

public class ClaimsButtonManager extends ButtonManager {

    public static final ClaimsButtonManager INSTANCE = new ClaimsButtonManager();

    @Override
    public ResourceLocation getIcon(SupportedMods mod, String theme) {
        return new ResourceLocation(ServerUtilities.MOD_ID, "textures/icons/team_blank.png");
    }

    @Override
    public String getButtonText() {
        return StatCollector.translateToLocal("serverutilities.navigator.button");
    }
}
