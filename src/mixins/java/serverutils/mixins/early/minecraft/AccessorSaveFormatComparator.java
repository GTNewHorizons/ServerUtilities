package serverutils.mixins.early.minecraft;

import net.minecraft.world.storage.SaveFormatComparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import serverutils.client.gui.ISaveFormatComparatorWithCheatSetter;

@Mixin(SaveFormatComparator.class)
public interface AccessorSaveFormatComparator extends ISaveFormatComparatorWithCheatSetter {

    @Override
    @Accessor("cheatsEnabled")
    void serverutilities$setCheatsEnabled(boolean cheatsEnabled);
}
