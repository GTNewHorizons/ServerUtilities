package serverutils.integration.navigator.journeymap;

import com.gtnewhorizons.navigator.api.journeymap.buttons.JMLayerButton;

import serverutils.integration.navigator.ClaimsButtonManager;

public class JMClaimsButton extends JMLayerButton {

    public static final JMClaimsButton INSTANCE = new JMClaimsButton();

    public JMClaimsButton() {
        super(ClaimsButtonManager.INSTANCE);
    }
}
