package ftb.lib.api.tile;

import net.minecraft.entity.player.EntityPlayer;

public interface ISecureTile {

    boolean canPlayerInteract(EntityPlayer ep, boolean breakBlock);

    void onPlayerNotOwner(EntityPlayer ep, boolean breakBlock);
}
