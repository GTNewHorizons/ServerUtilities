package serverutils.command.client;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import serverutils.lib.command.CmdBase;

public class CommandListAchievements extends CmdBase {

    public CommandListAchievements() {
        super("list_achievements", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, final String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) {
            return;
        }

        // List<Advancement> list = new ArrayList<>();

        // for (Advancement a :
        // Minecraft.getMinecraft().thePlayer.getStatFileWriter().getAdvancementManager().getAdvancementList().getAdvancements())
        // {
        // if (a.getDisplay() != null) {
        // list.add(a);
        // }
        // }

        // list.sort((o1, o2) ->
        // o1.getDisplay().getTitle().getUnformattedText().compareToIgnoreCase(o2.getDisplay().getTitle().getUnformattedText()));

        // GuiButtonListBase gui = new GuiButtonListBase() {
        // @Override
        // public void addButtons(Panel panel) {
        // for (Advancement advancement : list) {
        // panel.add(new SimpleTextButton(panel, advancement.getDisplay().getTitle().getFormattedText(),
        // ItemIcon.getItemIcon(advancement.getDisplay().getIcon())) {
        // @Override
        // public void onClicked(MouseButton button) {
        // GuiHelper.playClickSound();
        // GuiScreen.setClipboardString(advancement.getId().toString());
        // closeGui();
        // }

        // @Override
        // public void addMouseOverText(List<String> list) {
        // super.addMouseOverText(list);
        // list.add(EnumChatFormatting.GRAY + advancement.getId().toString());

        // for (Map.Entry<String, Criterion> entry : advancement.getCriteria().entrySet()) {
        // list.add(EnumChatFormatting.DARK_GRAY + "- " + entry.getKey());
        // }
        // }
        // });
        // }
        // }
        // };

        // gui.setTitle(I18n.format("gui.advancements"));
        // gui.setHasSearchBox(true);
        // gui.openGuiLater();
    }
}
