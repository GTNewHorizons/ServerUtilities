package serverutils.serverlib.integration;

import serverutils.serverlib.events.client.CustomClickEvent;
import serverutils.serverlib.lib.gui.GuiContainerWrapper;
import mezz.jei.Internal;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.bookmarks.BookmarkList;
import mezz.jei.config.KeyBindings;
import mezz.jei.input.InputHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

import java.util.Arrays;
import java.util.Optional;

@NEIPlugin
public class ServerLibNEIIntegration implements IModPlugin
{
	public static INeiRuntime RUNTIME;
	private static Optional<BookmarkList> bookmarkList;

	@Override
	public void onRuntimeAvailable(INeiRuntime r)
	{
		RUNTIME = r;
	}

	@Override
	public void register(IModRegistry registry)
	{
		try
		{
			registry.addGlobalGuiHandlers(new NEIGlobalGuiHandler());
		}
		catch (RuntimeException | LinkageError ignored)
		{
			// only JEI 4.14.0 or higher supports addGlobalGuiHandlers
		}

		try
		{
			registry.addGhostIngredientHandler(GuiContainerWrapper.class, new NEIGhostItemHandler());
		}
		catch (RuntimeException | LinkageError ignored)
		{
		}

		MinecraftForge.EVENT_BUS.register(ServerLibNEIIntegration.class);
	}

	/**
	 * FIXME: Remove hacks when JEI API supports ingredients in non-container GuiScreens
	 */
	public static void handleIngredientKey(int key, Object object)
	{
		if (RUNTIME != null)
		{
			if (KeyBindings.showRecipe.isActiveAndMatches(key))
			{
				showRecipe(object);
			}
			else if (KeyBindings.showUses.isActiveAndMatches(key))
			{
				showUses(object);
			}
			else if (KeyBindings.bookmark.isActiveAndMatches(key))
			{
				addBookmark(object);
			}
		}
	}

	public static void showRecipe(Object object)
	{
		RUNTIME.getRecipesGui().show(RUNTIME.getRecipeRegistry().createFocus(IFocus.Mode.OUTPUT, object));
	}

	public static void showUses(Object object)
	{
		RUNTIME.getRecipesGui().show(RUNTIME.getRecipeRegistry().createFocus(IFocus.Mode.INPUT, object));
	}

	@SuppressWarnings({"deprecation", "OptionalAssignedToNull", "OptionalIsPresent"})
	public static void addBookmark(Object object)
	{
		if (bookmarkList == null)
		{
			try
			{
				bookmarkList = Optional.of((BookmarkList) ReflectionHelper.findField(InputHandler.class, "bookmarkList").get(ReflectionHelper.findField(Internal.class, "inputHandler").get(null)));
			}
			catch (Exception ex)
			{
				bookmarkList = Optional.empty();
			}
		}

		if (bookmarkList.isPresent())
		{
			bookmarkList.get().add(object);
		}
	}

	@SubscribeEvent
	public static void onCustomClick(CustomClickEvent event)
	{
		if (event.getID().getNamespace().equals("jeicategory"))
		{
			if (RUNTIME != null)
			{
				RUNTIME.getRecipesGui().showCategories(Arrays.asList(event.getID().getPath().split(";")));
			}
		}
	}
}
