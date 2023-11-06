package serverutils.integration.vp;

import com.sinthoras.visualprospecting.integration.model.buttons.ButtonManager;

public class VPButtonManager extends ButtonManager {

    public static final VPButtonManager INSTANCE = new VPButtonManager();

    public VPButtonManager() {
        super("serverutilities.vp.button", "team");
    }
}
