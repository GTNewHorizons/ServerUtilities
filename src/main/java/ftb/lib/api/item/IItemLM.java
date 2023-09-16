package ftb.lib.api.item;

import net.minecraft.item.Item;

public interface IItemLM {

    String getItemID();

    void onPostLoaded();

    void loadRecipes();

    Item getItem();
}
