package serverutils.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import serverutils.lib.config.ConfigGroup;
import serverutils.lib.util.INBTSerializable;
import serverutils.lib.util.NBTUtils;

public class VanishData implements INBTSerializable<NBTTagCompound> {

    public enum DataType {

        INTERACTION("interact"),
        CHAT("chat"),
        COLLISION("collision"),
        DAMAGE_OTHERS("damage_other"),
        SEND_CONNECTION_MESSAGE("send_connection_msg"),
        CONTAINER_READ_ONLY("container_read_only"),
        ITEM_DROPPING("item_dropping");

        public final String name;
        public final String desc;

        DataType(String name) {
            this.name = name;
            this.desc = StatCollector.translateToLocal("player_config.vanish." + name.replace("_", "") + ".tooltip");
        }
    }

    public boolean interaction;
    public boolean chat;
    public boolean damageOthers;
    public boolean collision;
    public boolean sendConnectionMessage;
    public boolean containerReadOnly;
    public boolean itemDropping;

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean("interaction", interaction);
        nbt.setBoolean("chat", chat);
        nbt.setBoolean("damageOthers", damageOthers);
        nbt.setBoolean("collision", collision);
        nbt.setBoolean("sendConnectionMsg", sendConnectionMessage);
        nbt.setBoolean("containerReadOnly", containerReadOnly);
        nbt.setBoolean("itemDropping", itemDropping);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        interaction = nbt.getBoolean("interaction");
        chat = nbt.getBoolean("chat");
        damageOthers = nbt.getBoolean("damageOthers");
        collision = nbt.getBoolean("collision");
        sendConnectionMessage = nbt.getBoolean("sendConnectionMsg");
        containerReadOnly = NBTUtils.getBooleanOrTrue(nbt, "containerReadOnly");
        itemDropping = nbt.getBoolean("itemDropping");
    }

    public void addConfigs(ConfigGroup group) {
        ConfigGroup vanishGroup = group.getGroup("vanish");
        vanishGroup.addBool("interaction", () -> interaction, b -> interaction = b, false);
        vanishGroup.addBool("chat", () -> chat, b -> chat = b, false);
        vanishGroup.addBool("damageOther", () -> damageOthers, b -> damageOthers = b, false);
        vanishGroup.addBool("collision", () -> collision, b -> collision = b, false);
        vanishGroup.addBool("sendConnectionMsg", () -> sendConnectionMessage, b -> sendConnectionMessage = b, false);
        vanishGroup.addBool("containerReadOnly", () -> containerReadOnly, b -> containerReadOnly = b, true);
        vanishGroup.addBool("itemDropping", () -> itemDropping, b -> itemDropping = b, false);
    }

    public boolean setState(DataType type, boolean state) {
        return switch (type) {
            case INTERACTION -> interaction = state;
            case CHAT -> chat = state;
            case DAMAGE_OTHERS -> damageOthers = state;
            case COLLISION -> collision = state;
            case SEND_CONNECTION_MESSAGE -> sendConnectionMessage = state;
            case CONTAINER_READ_ONLY -> containerReadOnly = state;
            case ITEM_DROPPING -> itemDropping = state;
        };
    }

    public boolean toggleState(DataType type) {
        return switch (type) {
            case INTERACTION -> interaction = !interaction;
            case CHAT -> chat = !chat;
            case DAMAGE_OTHERS -> damageOthers = !damageOthers;
            case COLLISION -> collision = !collision;
            case SEND_CONNECTION_MESSAGE -> sendConnectionMessage = !sendConnectionMessage;
            case CONTAINER_READ_ONLY -> containerReadOnly = !containerReadOnly;
            case ITEM_DROPPING -> itemDropping = !itemDropping;
        };
    }
}
