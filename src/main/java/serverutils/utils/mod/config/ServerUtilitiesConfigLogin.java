package serverutils.utils.mod.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import latmod.lib.annotations.Info;
import serverutils.lib.JsonHelper;
import serverutils.lib.api.config.ConfigEntryCustom;
import serverutils.lib.api.item.ItemStackSerializer;

public class ServerUtilitiesConfigLogin {

    @Info("Message of the day. This will be displayed when player joins the server")
    public static final ConfigEntryChatComponentList motd = new ConfigEntryChatComponentList("motd");

    @Info({ "Items to give player when he first joins the server", "Format: StringID Size Metadata",
            "Does not support NBT yet" })
    public static final ConfigEntryItemStackList starting_items = new ConfigEntryItemStackList("starting_items");

    public static class ConfigEntryChatComponentList extends ConfigEntryCustom {

        public final List<IChatComponent> components;

        public ConfigEntryChatComponentList(String id) {
            super(id);
            components = new ArrayList<>();
            components.add(new ChatComponentText("Welcome to the server!"));
        }

        public void func_152753_a(JsonElement o) {
            components.clear();

            if (o.isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray()) {
                    IChatComponent c = JsonHelper.deserializeICC(e);

                    if (c != null) {
                        components.add(c);
                    }
                }
            }
        }

        public JsonElement getSerializableElement() {
            JsonArray a = new JsonArray();

            for (IChatComponent c : components) {
                a.add(JsonHelper.serializeICC(c));
            }

            return a;
        }
    }

    public static class ConfigEntryItemStackList extends ConfigEntryCustom {

        public final List<ItemStack> items;

        public ConfigEntryItemStackList(String id) {
            super(id);
            items = new ArrayList<>();
            items.add(new ItemStack(Items.apple, 16));
        }

        public void func_152753_a(JsonElement o) {
            items.clear();

            if (o.isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray()) {
                    ItemStack is = ItemStackSerializer.deserialize(e);

                    if (is != null) {
                        items.add(is);
                    }
                }
            }
        }

        public JsonElement getSerializableElement() {
            JsonArray a = new JsonArray();

            for (ItemStack is : items) {
                a.add(ItemStackSerializer.serialize(is));
            }

            return a;
        }
    }
}
