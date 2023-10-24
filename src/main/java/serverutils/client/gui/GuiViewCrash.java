package serverutils.client.gui;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonElement;

import serverutils.ServerUtilities;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.PanelScrollBar;
import serverutils.lib.gui.SimpleButton;
import serverutils.lib.gui.TextField;
import serverutils.lib.gui.Theme;
import serverutils.lib.icon.Icon;
import serverutils.lib.io.DataReader;
import serverutils.lib.io.HttpDataReader;
import serverutils.lib.io.RequestMethod;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.StringUtils;
import serverutils.net.MessageViewCrashDelete;

public class GuiViewCrash extends GuiBase {

    public class ThreadUploadCrash extends Thread {

        @Override
        public void run() {
            try {
                File urlFile = new File(
                        Minecraft.getMinecraft().mcDataDir,
                        ServerUtilities.SERVER_FOLDER + "uploaded_crash_reports/" + name.text[0]);
                String url = DataReader.get(urlFile).safeString();

                if (url.isEmpty()) {
                    URL mclogsURL = new URL("https://api.mclo.gs/1/log");
                    String outText = "content="
                            + URLEncoder.encode(StringUtils.fromStringList(Arrays.asList(text.text)), "UTF-8");

                    JsonElement json = DataReader.get(
                            mclogsURL,
                            RequestMethod.POST,
                            DataReader.URLENCODED,
                            new HttpDataReader.HttpDataOutput.StringOutput(outText),
                            Minecraft.getMinecraft().getProxy()).json();

                    if (json.isJsonObject() && json.getAsJsonObject().has("id")) {
                        url = "https://mclo.gs/" + json.getAsJsonObject().get("id").getAsString();
                        FileUtils.saveSafe(urlFile, url);
                    }
                }

                if (!url.isEmpty()) {
                    IChatComponent link = new ChatComponentTranslation("click_here");
                    link.getChatStyle().setColor(EnumChatFormatting.GOLD);
                    link.getChatStyle()
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(url)));
                    link.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    Minecraft.getMinecraft().thePlayer
                            .addChatMessage(new ChatComponentTranslation("serverutilities.lang.uploaded_crash", link));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private final TextField name;
    private final TextField text;
    private final Panel textPanel;
    private final PanelScrollBar scrollH, scrollV;
    private final Button close, upload, delete, reset;

    public GuiViewCrash(String n, Collection<String> l) {
        name = new TextField(this).setText(n);
        name.setPos(8, 12);

        textPanel = new Panel(this) {

            @Override
            public void addWidgets() {
                add(text);
            }

            @Override
            public void alignWidgets() {
                scrollH.setMaxValue(text.width + 4);
                scrollV.setMaxValue(text.height);
            }

            @Override
            public void drawBackground(Theme theme, int x, int y, int w, int h) {
                theme.drawContainerSlot(x, y, w, h);
            }
        };

        text = new TextField(textPanel);
        text.setX(2);
        text.addFlags(Theme.UNICODE);
        text.setText(StringUtils.fixTabs(String.join("\n", l), 2));

        textPanel.setPos(9, 33);
        textPanel.setUnicode(true);

        scrollH = new PanelScrollBar(this, PanelScrollBar.Plane.HORIZONTAL, textPanel);
        scrollH.setCanAlwaysScroll(true);
        scrollH.setCanAlwaysScrollPlane(false);
        scrollH.setScrollStep(30);

        scrollV = new PanelScrollBar(this, textPanel);
        scrollV.setCanAlwaysScroll(true);
        scrollV.setCanAlwaysScrollPlane(false);
        scrollV.setScrollStep(30);

        close = new SimpleButton(
                this,
                I18n.format("gui.close"),
                GuiIcons.CLOSE,
                (widget, button) -> widget.getGui().closeGui());

        upload = new SimpleButton(
                this,
                I18n.format("serverutilities.lang.upload_crash"),
                GuiIcons.UP,
                (widget, button) -> {
                    new ThreadUploadCrash().start();
                    widget.getGui().closeGui(false);
                });

        delete = new SimpleButton(
                this,
                I18n.format("selectServer.delete"),
                GuiIcons.REMOVE,
                (widget, button) -> openYesNo(
                        I18n.format("delete_item", name.text[0]),
                        "",
                        () -> new MessageViewCrashDelete(name.text[0]).sendToServer()));

        reset = new SimpleButton(this, "", Icon.EMPTY, (widget, button) -> {
            scrollH.setValue(0);
            scrollV.setValue(0);
        });
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
    }

    @Override
    public void addWidgets() {
        add(textPanel);
        add(scrollH);
        add(scrollV);
        add(close);
        add(upload);
        add(delete);
        add(reset);
        add(name);
    }

    @Override
    public void alignWidgets() {
        close.setPosAndSize(width - 24, 8, 20, 20);
        upload.setPosAndSize(width - 48, 8, 20, 20);
        delete.setPosAndSize(width - 72, 8, 20, 20);
        reset.setPos(width - 24, height - 24);
        scrollH.setPosAndSize(8, height - 24, width - 32, 16);
        scrollV.setPosAndSize(width - 24, 32, 16, height - 56);
        textPanel.setSize(scrollH.width - 2, scrollV.height - 2);
        textPanel.alignWidgets();
    }
}
