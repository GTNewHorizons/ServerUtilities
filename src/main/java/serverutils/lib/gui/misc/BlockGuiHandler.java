package serverutils.lib.gui.misc;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class BlockGuiHandler implements IGuiHandler {

    private final Int2ObjectMap<BlockGuiSupplier> map = new Int2ObjectOpenHashMap<>();

    public void add(BlockGuiSupplier h) {
        map.put(h.id, h);
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockGuiSupplier supplier = map.get(ID);

        if (supplier != null) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);

            if (tileEntity != null) {
                return supplier.getContainer(player, tileEntity);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockGuiSupplier supplier = map.get(ID);

        if (supplier != null) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);

            if (tileEntity != null) {
                Container container = supplier.getContainer(player, tileEntity);
                return container == null ? null : supplier.getGui(container);
            }
        }

        return null;
    }
}
