package ftb.lib.mod.client;

import java.util.UUID;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.particle.EntityReddustFX;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.EnumScreen;
import ftb.lib.EventBusHelper;
import ftb.lib.FTBLib;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.config.ClientConfigRegistry;
import ftb.lib.api.config.ConfigEntryBool;
import ftb.lib.api.config.ConfigEntryEnum;
import ftb.lib.api.config.ConfigEntryString;
import ftb.lib.api.gui.LMGuiHandler;
import ftb.lib.api.gui.LMGuiHandlerRegistry;
import ftb.lib.api.gui.PlayerActionRegistry;
import ftb.lib.api.tile.IGuiTile;
import ftb.lib.mod.FTBLibModCommon;
import ftb.lib.mod.cmd.CmdReloadClient;
import latmod.lib.LMColorUtils;
import latmod.lib.LMUtils;

@SideOnly(Side.CLIENT)
public class FTBLibModClient extends FTBLibModCommon {

    public static final ConfigEntryBool item_ore_names = new ConfigEntryBool("item_ore_names", false);
    public static final ConfigEntryBool item_reg_names = new ConfigEntryBool("item_reg_names", false);

    public static final ConfigEntryEnum<EnumScreen> notifications = new ConfigEntryEnum<>(
            "notifications",
            EnumScreen.values(),
            EnumScreen.SCREEN,
            false);
    public static final ConfigEntryString reload_client_cmd = new ConfigEntryString(
            "reload_client_cmd",
            "reload_client");

    public void preInit() {
        EventBusHelper.register(FTBLibClientEventHandler.instance);
        EventBusHelper.register(FTBLibRenderHandler.instance);
        LMGuiHandlerRegistry.add(FTBLibGuiHandler.instance);

        // For Dev reasons, see DevConsole
        FTBLib.userIsLatvianModder = FTBLibClient.mc.getSession().func_148256_e().getId()
                .equals(LMUtils.fromString("5afb9a5b207d480e887967bc848f9a8f"));

        ClientConfigRegistry.addGroup("ftbl", FTBLibModClient.class);
        ClientConfigRegistry.add(PlayerActionRegistry.configGroup);

        ClientCommandHandler.instance.registerCommand(new CmdReloadClient());

        FTBLibActions.init();
    }

    public void postInit() {
        ClientConfigRegistry.provider().getConfigGroup();
    }

    public String translate(String key, Object... obj) {
        return I18n.format(key, obj);
    }

    public boolean isShiftDown() {
        return GuiScreen.isShiftKeyDown();
    }

    public boolean isCtrlDown() {
        return GuiScreen.isCtrlKeyDown();
    }

    public boolean isTabDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_TAB);
    }

    public boolean inGameHasFocus() {
        return FTBLibClient.mc.inGameHasFocus;
    }

    public EntityPlayer getClientPlayer() {
        return FMLClientHandler.instance().getClientPlayerEntity();
    }

    public EntityPlayer getClientPlayer(UUID id) {
        return FTBLibClient.getPlayerSP(id);
    }

    public World getClientWorld() {
        return FMLClientHandler.instance().getWorldClient();
    }

    public double getReachDist(EntityPlayer ep) {
        if (ep == null) return 0D;
        else if (ep instanceof EntityPlayerMP) return super.getReachDist(ep);
        PlayerControllerMP c = FTBLibClient.mc.playerController;
        return (c == null) ? 0D : c.getBlockReachDistance();
    }

    public void spawnDust(World w, double x, double y, double z, int col) {
        EntityReddustFX fx = new EntityReddustFX(w, x, y, z, 0F, 0F, 0F) {};

        float alpha = LMColorUtils.getAlpha(col) / 255F;
        float red = LMColorUtils.getRed(col) / 255F;
        float green = LMColorUtils.getGreen(col) / 255F;
        float blue = LMColorUtils.getBlue(col) / 255F;
        if (alpha == 0F) alpha = 1F;

        fx.setRBGColorF(red, green, blue);
        fx.setAlphaF(alpha);
        FTBLibClient.mc.effectRenderer.addEffect(fx);
    }

    public boolean openClientGui(EntityPlayer ep, String mod, int id, NBTTagCompound data) {
        LMGuiHandler h = LMGuiHandlerRegistry.get(mod);

        if (h != null) {
            GuiScreen g = h.getGui(ep, id, data);

            if (g != null) {
                FTBLibClient.openGui(g);
                return true;
            }
        }

        return false;
    }

    public void openClientTileGui(EntityPlayer ep, IGuiTile t, NBTTagCompound data) {
        if (ep != null && t != null) {
            GuiScreen g = t.getGui(ep, data);
            if (g != null) FTBLibClient.openGui(g);
        }
    }
}
