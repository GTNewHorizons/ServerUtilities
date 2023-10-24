package serverutils.lib.gui.misc;

import net.minecraft.client.resources.I18n;

import serverutils.lib.config.ConfigTeamClient;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.util.misc.MouseButton;

public class GuiSelectTeamValue extends GuiButtonListBase {

    private final ConfigTeamClient value;
    private final IOpenableGui callbackGui;
    private final Runnable callback;

    public GuiSelectTeamValue(ConfigTeamClient v, IOpenableGui c, Runnable cb) {
        setTitle(I18n.format("serverutilities.select_team.gui"));
        setHasSearchBox(true);
        value = v;
        callbackGui = c;
        callback = cb;
    }

    @Override
    public void addButtons(Panel panel) {
        for (ConfigTeamClient.TeamInst inst : value.map.values()) {
            panel.add(new SimpleTextButton(panel, inst.title.getFormattedText(), inst.icon) {

                @Override
                public void onClicked(MouseButton button) {
                    GuiHelper.playClickSound();
                    callbackGui.openGui();
                    value.setString(inst.getId());
                    callback.run();
                }
            });
        }
    }
}
