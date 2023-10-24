package serverutils.lib.gui.misc;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

public abstract class BlockGuiSupplier {

    public final Object mod;
    public final int id;

    public BlockGuiSupplier(Object _mod, int _id) {
        mod = _mod;
        id = _id;
    }

    public void open(EntityPlayer player, int posx, int posy, int posz) {
        player.openGui(mod, id, player.worldObj, posx, posy, posz);
    }

    @Nullable
    public abstract Container getContainer(EntityPlayer player, TileEntity tileEntity);

    public abstract Object getGui(Container container);
}
