package serverutils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilities;

@SideOnly(Side.CLIENT)
public class TransferClientHandler {

    public static void execute(String host, int port) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            if (mc.func_147104_D() == null) {
                ServerUtilities.LOGGER.warn("Ignoring transfer request in singleplayer");
                return;
            }

            String address = port == 25565 ? host : host + ":" + port;
            ServerData serverData = new ServerData("Transfer", address);
            mc.displayGuiScreen(new GuiTransferring(serverData));
        });
    }

    @SideOnly(Side.CLIENT)
    static class GuiTransferring extends GuiScreen {

        private final ServerData serverData;

        GuiTransferring(ServerData serverData) {
            this.serverData = serverData;
        }

        @Override
        public void updateScreen() {
            this.mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), this.mc, this.serverData));
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            this.drawCenteredString(
                    this.fontRendererObj,
                    "Transferring...",
                    this.width / 2,
                    this.height / 2 - 50,
                    0xFFFFFF);
            super.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}
