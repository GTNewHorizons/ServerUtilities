package serverutils.integration.navigator;

import com.gtnewhorizons.navigator.api.model.buttons.ButtonManager;

public class ClaimsButtonManager extends ButtonManager {

    public static final ClaimsButtonManager INSTANCE = new ClaimsButtonManager();

    public ClaimsButtonManager() {
        super("serverutilities.vp.button", "team");
    }
}
