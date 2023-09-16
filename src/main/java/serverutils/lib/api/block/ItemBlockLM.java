package serverutils.lib.api.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemBlockLM extends ItemBlock {

    public IBlockLM blockLM;

    public ItemBlockLM(Block b) {
        super(b);
        setHasSubtypes(true);
        setMaxDamage(0);

        blockLM = (IBlockLM) b;
    }

    public int getMetadata(int m) {
        return m;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack is, EntityPlayer ep, List l, boolean adv) {
        blockLM.addInformation(is, ep, l, adv);
    }
}
