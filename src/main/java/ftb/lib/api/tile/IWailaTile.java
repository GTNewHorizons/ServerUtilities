package ftb.lib.api.tile;

import java.util.List;

import net.minecraft.item.ItemStack;

import ftb.lib.api.waila.WailaDataAccessor;

public interface IWailaTile {

    public static interface Stack extends IWailaTile {

        ItemStack getWailaStack(WailaDataAccessor data);
    }

    public static interface Head extends IWailaTile {

        void addWailaHead(WailaDataAccessor data, List<String> info);
    }

    public static interface Body extends IWailaTile {

        void addWailaBody(WailaDataAccessor data, List<String> info);
    }

    public static interface Tail extends IWailaTile {

        void addWailaTail(WailaDataAccessor data, List<String> info);
    }
}
