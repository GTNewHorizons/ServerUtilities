package serverutils.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;

public class WitcheryCompat {

    static public boolean isVampire(EntityPlayer player) {
        ExtendedPlayer playerEx = ExtendedPlayer.get(player);
        return playerEx != null && playerEx.isVampire();
    }

    static public boolean isSleepInCoffin(World world, EntityPlayer player) {
        ChunkCoordinates loc = player.playerLocation;
        return world.getBlock(loc.posX, loc.posY, loc.posZ) == Witchery.Blocks.COFFIN;
    }
}
