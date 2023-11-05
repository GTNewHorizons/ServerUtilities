package serverutils.lib.gui.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.data.Action;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.WidgetType;
import serverutils.lib.util.misc.MouseButton;

public class GuiActionList extends GuiButtonListBase {

    private class ActionButton extends SimpleTextButton {

        private final Action.Inst action;

        private ActionButton(Panel panel, Action.Inst a) {
            super(panel, a.title.getFormattedText(), a.icon);
            action = a;
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            if (action.requiresConfirm) {
                String key = "team_action." + action.id.getResourceDomain()
                        + "."
                        + action.id.getResourcePath()
                        + ".confirmation";
                openYesNo(
                        action.title.getFormattedText() + "?",
                        (EnumChatFormatting.RED + I18n.format(key)),
                        () -> callback.accept(action.id));
            } else {
                callback.accept(action.id);
            }
        }

        @Override
        public boolean renderTitleInCenter() {
            return false;
        }

        @Override
        public WidgetType getWidgetType() {
            return action.enabled ? WidgetType.mouseOver(isMouseOver()) : WidgetType.DISABLED;
        }
    }

    private final ArrayList<Action.Inst> actions;
    private final Consumer<ResourceLocation> callback;

    public GuiActionList(String title, Collection<Action.Inst> a, Consumer<ResourceLocation> c) {
        setTitle(title);
        actions = new ArrayList<>(a);
        actions.sort(null);
        callback = c;
    }

    @Override
    public void addButtons(Panel panel) {
        for (Action.Inst a : actions) {
            panel.add(new ActionButton(panel, a));
        }
    }
}
