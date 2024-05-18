package serverutils.net;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import serverutils.command.CmdEditNBT;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToServer;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.BlockUtils;

public class MessageEditNBTResponse extends MessageToServer {

    private NBTTagCompound info, mainNbt;

    public MessageEditNBTResponse() {}

    public MessageEditNBTResponse(NBTTagCompound i, NBTTagCompound nbt) {
        info = i;
        mainNbt = nbt;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeNBT(info);
        data.writeNBT(mainNbt);
    }

    @Override
    public void readData(DataIn data) {
        info = data.readNBT();
        mainNbt = data.readNBT();
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (CmdEditNBT.EDITING.get(player.getGameProfile().getId()).equals(info)) {
            CmdEditNBT.EDITING.remove(player.getGameProfile().getId());

            switch (info.getString("type")) {
                case "player": {
                    ForgePlayer player1 = Universe.get().getPlayer(info.getString("id"));

                    if (player1 != null) {
                        player1.setPlayerNBT(mainNbt);
                    }

                    break;
                }
                case "block": {
                    int x = info.getInteger("x");
                    int y = info.getInteger("y");
                    int z = info.getInteger("z");

                    if (player.worldObj.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
                        TileEntity tile = player.worldObj.getTileEntity(x, y, z);

                        if (tile != null) {
                            mainNbt.setInteger("x", x);
                            mainNbt.setInteger("y", y);
                            mainNbt.setInteger("z", z);
                            mainNbt.setString("id", info.getString("id"));
                            tile.readFromNBT(mainNbt);
                            tile.markDirty();
                            BlockUtils.notifyBlockUpdate(tile.getWorldObj(), x, y, z, null);
                        }
                    }

                    break;
                }
                case "entity": {
                    Entity entity = player.worldObj.getEntityByID(info.getInteger("id"));

                    if (entity != null) {
                        entity.readFromNBT(mainNbt);

                        if (entity.isEntityAlive()) {
                            player.worldObj.updateEntityWithOptionalForce(entity, true);
                        }
                    }

                    break;
                }
                case "item": {
                    ItemStack stack = ItemStack.loadItemStackFromNBT(mainNbt);
                    player.setCurrentItemOrArmor(0, stack);
                }
            }
        }
    }
}
