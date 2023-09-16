package ftb.lib.api.waila;

import java.util.List;

import net.minecraft.item.ItemStack;

public abstract class BasicWailaHandler {

    public final WailaType[] types;

    public BasicWailaHandler(EventRegisterWaila e, WailaType... t) {
        types = t;
    }

    public ItemStack getWailaStack(WailaDataAccessor data) {
        return null;
    }

    public List<String> getWailaHead(ItemStack is, List<String> l, WailaDataAccessor data) {
        return l;
    }

    public List<String> getWailaBody(ItemStack is, List<String> l, WailaDataAccessor data) {
        return l;
    }

    public List<String> getWailaTail(ItemStack is, List<String> l, WailaDataAccessor data) {
        return l;
    }
}
