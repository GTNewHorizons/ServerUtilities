package serverutils.integration.vp.journeymap;

import com.sinthoras.visualprospecting.integration.journeymap.buttons.LayerButton;

import serverutils.integration.vp.VPButtonManager;

public class VPLayerButton extends LayerButton {

    public static final VPLayerButton INSTANCE = new VPLayerButton();

    public VPLayerButton() {
        super(VPButtonManager.INSTANCE);
    }
}
