package serverutils.lib.api.block;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.item.IItemLM;

public interface IBlockLM extends IItemLM {

    Class<? extends ItemBlock> getItemBlock();

    @SideOnly(Side.CLIENT)
    void addInformation(ItemStack is, EntityPlayer ep, List<String> l, boolean adv);
}
