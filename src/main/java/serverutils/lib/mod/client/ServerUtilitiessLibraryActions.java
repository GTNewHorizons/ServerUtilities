package serverutils.lib.mod.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.EventBusHelper;
import serverutils.lib.api.PlayerAction;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.config.ClientConfigRegistry;
import serverutils.lib.api.friends.ILMPlayer;
import serverutils.lib.api.gui.GuiIcons;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.gui.GuiScreenRegistry;
import serverutils.lib.api.gui.PlayerActionRegistry;
import serverutils.lib.api.notification.ClientNotifications;
import serverutils.lib.mod.ServerUtilitiesLibraryMod;
import serverutils.lib.mod.client.gui.GuiEditConfig;
import serverutils.lib.mod.client.gui.GuiNotifications;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCompat;

@SideOnly(Side.CLIENT)
public class ServerUtilitiessLibraryActions {

    public static void init() {
        EventBusHelper.register(new ServerUtilitiessLibraryActions());

        PlayerActionRegistry.add(notifications);
        PlayerActionRegistry.add(settings);

        GuiScreenRegistry.register("notifications", new GuiScreenRegistry.Entry() {

            public GuiScreen openGui(EntityPlayer ep) {
                return new GuiNotifications(ServerUtilitiesLibraryClient.mc.currentScreen);
            }
        });

        GuiScreenRegistry.register("client_config", new GuiScreenRegistry.Entry() {

            public GuiScreen openGui(EntityPlayer ep) {
                return new GuiEditConfig(
                        ServerUtilitiesLibraryClient.mc.currentScreen,
                        ClientConfigRegistry.provider());
            }
        });
    }

    public static final PlayerAction notifications = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverlib.notifications",
            1000,
            GuiIcons.chat) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ServerUtilitiesLibraryClient.openGui(new GuiNotifications(ServerUtilitiesLibraryClient.mc.currentScreen));
        }

        public String getDisplayName() {
            return ServerUtilitiesLibraryMod.proxy.translate(ServerUtilitiesLibraryModClient.notifications.getFullID());
        }

        public boolean isVisibleFor(ILMPlayer self, ILMPlayer other) {
            return !ClientNotifications.Perm.list.isEmpty();
        }

        public void postRender(int ax, int ay, double z) {
            String n = String.valueOf(ClientNotifications.Perm.list.size());
            int nw = ServerUtilitiesLibraryClient.mc.fontRenderer.getStringWidth(n);
            int width = 16;
            GlStateManager.color(1F, 0.13F, 0.13F, 0.66F);
            GuiLM.drawBlankRect(ax + width - nw, ay - 4, z, nw + 1, 9);
            GlStateManager.color(1F, 1F, 1F, 1F);
            ServerUtilitiesLibraryClient.mc.fontRenderer.drawString(n, ax + width - nw + 1, ay - 3, 0xFFFFFFFF);
        }
    };

    public static final PlayerAction settings = new PlayerAction(
            PlayerAction.Type.SELF,
            "serverlib.settings",
            -1000,
            GuiIcons.settings) {

        public void onClicked(ILMPlayer self, ILMPlayer other) {
            ServerUtilitiesLibraryClient.openGui(
                    new GuiEditConfig(ServerUtilitiesLibraryClient.mc.currentScreen, ClientConfigRegistry.provider()));
        }
    };

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void guiInitEvent(final GuiScreenEvent.InitGuiEvent.Post e) {
        if (!ServerUtilitiesLibraryClient.isIngame()) return;

        if (e.gui instanceof InventoryEffectRenderer) {
            ILMPlayer p = ServerUtilitiesLibraryClient.getClientLMPlayer();
            List<PlayerAction> buttons = PlayerActionRegistry
                    .getPlayerActions(PlayerAction.Type.SELF, p, p, false, false);

            if (!buttons.isEmpty()) {
                Collections.sort(buttons);

                ButtonInvLMRenderer renderer = new ButtonInvLMRenderer(495830, e.gui);
                e.buttonList.add(renderer);

                int xSize = 176;
                int ySize = 166;
                int buttonX = -17;
                int buttonY = 8;

                if (e.gui instanceof GuiContainerCreative) {
                    xSize = 195;
                    ySize = 136;
                    buttonY = 6;
                }
                boolean hasPotions = !e.gui.mc.thePlayer.getActivePotionEffects().isEmpty()
                        && !ServerUtilitiesLibConfigCompat.compat_statusEffectHUD.getAsBoolean();

                if (hasPotions) {
                    buttonX -= 64;
                    buttonY -= 26;
                }

                boolean isTFCLoaded = Loader.isModLoaded("terrafirmacraftplus");

                if (isTFCLoaded) {
                    xSize = 230;
                    ySize = 185;
                }

                int guiLeft = (e.gui.width - xSize) / 2;
                int guiTop = (e.gui.height - ySize) / 2;

                if (hasPotions) guiLeft += 60;

                for (int i = 0; i < buttons.size(); i++) {
                    PlayerAction a = buttons.get(i);
                    ButtonInvLM b;

                    if (hasPotions) {
                        int x = i % 6;
                        int y = i / 6;
                        b = new ButtonInvLM(495830 + i, a, guiLeft + buttonX - 18 * x, guiTop + buttonY - y * 18);
                    } else {
                        int x = i / 8;
                        int y = i % 8;
                        b = new ButtonInvLM(495830 + i, a, guiLeft + buttonX - 18 * x, guiTop + buttonY + 18 * y);
                    }

                    e.buttonList.add(b);
                    renderer.buttons.add(b);
                }
            }
        }
    }

    @SubscribeEvent
    public void guiActionEvent(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if (e.button instanceof ButtonInvLM) {
            PlayerAction b = ((ButtonInvLM) e.button).action;
            ILMPlayer p = ServerUtilitiesLibraryClient.getClientLMPlayer();
            b.onClicked(p, p);
        }
    }

    private static class ButtonInvLM extends GuiButton {

        public final PlayerAction action;

        public ButtonInvLM(int id, PlayerAction b, int x, int y) {
            super(id, x, y, 16, 16, "");
            action = b;
        }

        public void drawButton(Minecraft mc, int mx, int my) {}
    }

    private static class ButtonInvLMRenderer extends GuiButton {

        public final List<ButtonInvLM> buttons;

        public ButtonInvLMRenderer(int id, GuiScreen g) {
            super(id, -1000, -1000, 0, 0, "");
            buttons = new ArrayList<>();
        }

        public void drawButton(Minecraft mc, int mx, int my) {
            zLevel = 0F;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GlStateManager.color(1F, 1F, 1F, 1F);

            for (ButtonInvLM b : buttons) {
                b.action.render(b.xPosition, b.yPosition, zLevel);

                if (mx >= b.xPosition && my >= b.yPosition
                        && mx < b.xPosition + b.width
                        && my < b.yPosition + b.height) {
                    GlStateManager.color(1F, 1F, 1F, 0.3F);
                    GuiLM.drawBlankRect(b.xPosition, b.yPosition, 0D, b.width, b.height);
                    GlStateManager.color(1F, 1F, 1F, 1F);
                }
            }

            for (ButtonInvLM b : buttons) {
                b.action.postRender(b.xPosition, b.yPosition, 0D);

                if (mx >= b.xPosition && my >= b.yPosition
                        && mx < b.xPosition + b.width
                        && my < b.yPosition + b.height) {
                    GlStateManager.pushMatrix();
                    double mx1 = mx - 4D;
                    double my1 = my - 12D;

                    String s = b.action.getDisplayName();
                    int tw = ServerUtilitiesLibraryClient.mc.fontRenderer.getStringWidth(s);

                    mx1 -= tw + 8;
                    my1 += 4;

                    if (mx1 < 4D) mx1 = 4D;
                    if (my1 < 4D) my1 = 4D;

                    GlStateManager.translate(mx1, my1, zLevel);

                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GlStateManager.color(0.13F, 0.13F, 0.13F, 1F);
                    GuiLM.drawBlankRect(-3, -2, zLevel, tw + 6, 12);
                    GlStateManager.color(1F, 1F, 1F, 1F);
                    ServerUtilitiesLibraryClient.mc.fontRenderer.drawString(s, 0, 0, 0xFFFFFFFF);
                    GlStateManager.popMatrix();
                }
            }

            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }
}
