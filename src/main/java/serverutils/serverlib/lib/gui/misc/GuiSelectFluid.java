package serverutils.serverlib.lib.gui.misc;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.gui.IOpenableGui;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.SimpleTextButton;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.icon.Icon;
import serverutils.serverlib.lib.util.InvUtils;
import serverutils.serverlib.lib.util.misc.MouseButton;

public class GuiSelectFluid extends GuiButtonListBase {

	private final IOpenableGui callbackGui;
	private final Supplier<Fluid> defaultFluid;
	private final Consumer<Fluid> callback;

	public GuiSelectFluid(IOpenableGui g, Supplier<Fluid> def, Consumer<Fluid> c) {
		setTitle(I18n.format("serverlib.select_fluid.gui"));
		setHasSearchBox(true);
		callbackGui = g;
		defaultFluid = def;
		callback = c;
	}

	@Override
	public void addButtons(Panel panel) {
		if (defaultFluid.get() == null) {
			panel.add(new SimpleTextButton(panel, I18n.format("serverlib.select_fluid.none"), GuiIcons.BARRIER) {

				@Override
				public void onClicked(MouseButton button) {
					GuiHelper.playClickSound();
					callbackGui.openGui();
					callback.accept(null);
				}
			});
		}

		for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
			FluidStack fluidStack = new FluidStack(fluid, InvUtils.BUCKET_VOLUME);

			panel.add(
					new SimpleTextButton(
							panel,
							fluid.getLocalizedName(fluidStack),
							Icon.getIcon(fluid.getStillIcon().getIconName())
									.withTint(Color4I.rgb(fluid.getColor(fluidStack)))) {

						@Override
						public void onClicked(MouseButton button) {
							GuiHelper.playClickSound();
							callbackGui.openGui();
							callback.accept(fluid);
						}

						@Override
						public Object getIngredientUnderMouse() {
							return new FluidStack(fluid, InvUtils.BUCKET_VOLUME);
						}
					});
		}
	}
}