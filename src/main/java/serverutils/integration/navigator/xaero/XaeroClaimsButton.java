package serverutils.integration.navigator.xaero;

import com.gtnewhorizons.navigator.api.xaero.buttons.XaeroLayerButton;

import serverutils.integration.navigator.ClaimsButtonManager;

public class XaeroClaimsButton extends XaeroLayerButton {

    public static final XaeroClaimsButton INSTANCE = new XaeroClaimsButton();

    public XaeroClaimsButton() {
        super(ClaimsButtonManager.INSTANCE);
    }
}
