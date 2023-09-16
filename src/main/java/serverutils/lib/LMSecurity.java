package serverutils.lib;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

import serverutils.lib.api.friends.ILMPlayer;

public class LMSecurity {

    private int ownerID;
    public PrivacyLevel level;

    public LMSecurity(Object o) {
        setOwner(o);
        level = PrivacyLevel.PUBLIC;
    }

    public int getOwnerID() {
        return ownerID;
    }

    public ILMPlayer getOwner() {
        return (ServerUtilitiesLib.serverUtilitiesIntegration == null) ? null
                : ServerUtilitiesLib.serverUtilitiesIntegration.getLMPlayer(ownerID);
    }

    public void setOwner(Object o) {
        ownerID = 0;

        if (o != null && ServerUtilitiesLib.serverUtilitiesIntegration != null) {
            ILMPlayer p = ServerUtilitiesLib.serverUtilitiesIntegration.getLMPlayer(o);
            if (p != null) ownerID = p.getPlayerID();
        }
    }

    public void readFromNBT(NBTTagCompound tag, String s) {
        if (tag.hasKey(s)) {
            NBTTagCompound tag1 = tag.getCompoundTag(s);
            ownerID = tag1.getInteger("Owner");
            level = PrivacyLevel.VALUES_3[tag1.getByte("Level")];
        } else {
            ownerID = 0;
            level = PrivacyLevel.PUBLIC;
        }
    }

    public void writeToNBT(NBTTagCompound tag, String s) {
        if (ownerID > 0 || level != PrivacyLevel.PUBLIC) {
            NBTTagCompound tag1 = new NBTTagCompound();
            tag1.setInteger("Owner", ownerID);
            tag1.setByte("Level", (byte) level.ID);
            tag.setTag(s, tag1);
        }
    }

    public boolean hasOwner() {
        return getOwner() != null;
    }

    public final boolean isOwner(EntityPlayer ep) {
        if (ServerUtilitiesLib.serverUtilitiesIntegration == null) return true;
        return isOwner(ServerUtilitiesLib.serverUtilitiesIntegration.getLMPlayer(ep));
    }

    public boolean isOwner(ILMPlayer player) {
        return hasOwner() && ownerID == player.getPlayerID();
    }

    public final boolean canInteract(EntityPlayer ep) {
        if (ServerUtilitiesLib.serverUtilitiesIntegration == null) return true;
        return canInteract(ServerUtilitiesLib.serverUtilitiesIntegration.getLMPlayer(ep));
    }

    public boolean canInteract(ILMPlayer playerLM) {
        if (ServerUtilitiesLib.serverUtilitiesIntegration == null) return true;
        if (level == PrivacyLevel.PUBLIC || getOwner() == null) return true;
        if (playerLM == null) return false;
        if (isOwner(playerLM)) return true;
        if (playerLM != null && playerLM.isOnline() && playerLM.allowInteractSecure()) return true;
        if (level == PrivacyLevel.PRIVATE) return false;
        ILMPlayer owner = getOwner();
        if (level == PrivacyLevel.FRIENDS && owner.isFriend(playerLM)) return true;

        return false;
    }

    public void printOwner(ICommandSender ep) {
        ep.addChatMessage(
                new ChatComponentTranslation(
                        "serverlib.owner",
                        hasOwner() ? getOwner().getProfile().getName() : "null"));
    }
}
