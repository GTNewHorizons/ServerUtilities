package serverutils.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.command.CmdTreeHelp;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.math.MathUtils;
import serverutils.lib.util.StringUtils;
import serverutils.net.MessageEditNBT;
import serverutils.net.MessageEditNBTRequest;

public class CmdEditNBT extends CmdTreeBase {

    public static Map<UUID, NBTTagCompound> EDITING = new HashMap<>();

    public CmdEditNBT() {
        super("nbtedit");
        addSubcommand(new CmdBlock());
        addSubcommand(new CmdEntity());
        addSubcommand(new CmdPlayer());
        addSubcommand(new CmdItem());
        addSubcommand(new CmdTreeHelp(this));
    }

    private static void addInfo(NBTTagList list, IChatComponent key, IChatComponent value) {
        list.appendTag(
                new NBTTagString(
                        IChatComponent.Serializer.func_150696_a(
                                StringUtils.color(key, EnumChatFormatting.BLUE).appendText(": ")
                                        .appendSibling(StringUtils.color(value, EnumChatFormatting.GOLD)))));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            new MessageEditNBTRequest().sendTo(getCommandSenderAsPlayer(sender));
        } else {
            super.processCommand(sender, args);
        }
    }

    public static class CmdNBT extends CmdBase {

        private CmdNBT(String id) {
            super(id, Level.OP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            NBTTagCompound info = new NBTTagCompound();
            NBTTagCompound nbt = editNBT(player, info, args);

            if (info.hasKey("type")) {
                info.setLong("random", MathUtils.RAND.nextLong());
                EDITING.put(player.getGameProfile().getId(), info);
                new MessageEditNBT(info, nbt).sendTo(player);
            }
        }

        public NBTTagCompound editNBT(EntityPlayerMP player, NBTTagCompound info, String[] args)
                throws CommandException {
            return new NBTTagCompound();
        }
    }

    private static class CmdBlock extends CmdNBT {

        private CmdBlock() {
            super("block");
        }

        @Override
        public NBTTagCompound editNBT(EntityPlayerMP player, NBTTagCompound info, String[] args)
                throws CommandException {
            checkArgs(player, args, 3);
            int x = parseIntBounded(player, args[0], -30000000, 30000000);
            int y = parseIntBounded(player, args[1], -30000000, 30000000);
            int z = parseIntBounded(player, args[2], -30000000, 30000000);

            TileEntity tile = player.worldObj.getTileEntity(x, y, z);
            NBTTagCompound nbt = new NBTTagCompound();

            if (tile == null) {
                player.addChatMessage(
                        ServerUtilities.lang(
                                player,
                                "commands.nbtedit.tile_not_found",
                                player.worldObj.getBlock(x, y, z).getLocalizedName()));
            } else {
                info.setString("type", "block");
                info.setInteger("x", x);
                info.setInteger("y", y);
                info.setInteger("z", z);
                tile.writeToNBT(nbt);
                nbt.removeTag("x");
                nbt.removeTag("y");
                nbt.removeTag("z");
                info.setString("id", nbt.getString("id"));
                nbt.removeTag("id");

                NBTTagList list = new NBTTagList();
                addInfo(list, new ChatComponentText("Class"), new ChatComponentText(tile.getClass().getName()));
                String key = (String) TileEntity.classToNameMap.get(tile.getClass());
                addInfo(
                        list,
                        new ChatComponentText("ID"),
                        new ChatComponentText(key == null ? "null" : key.toString()));
                addInfo(
                        list,
                        new ChatComponentText("Block"),
                        new ChatComponentText(tile.getBlockType().getLocalizedName()));
                addInfo(
                        list,
                        new ChatComponentText("Block Class"),
                        new ChatComponentText(tile.getBlockType().getClass().getName()));
                addInfo(
                        list,
                        new ChatComponentText("Position"),
                        new ChatComponentText("[" + x + ", " + y + ", " + z + "]"));

                ModContainer mod = null;
                for (ModContainer m : Loader.instance().getModList()) {
                    if (tile.getClass().getName().contains(m.getModId())) {
                        mod = m;
                        break;
                    }
                }

                addInfo(
                        list,
                        new ChatComponentText("Mod"),
                        new ChatComponentText(mod == null ? "null" : mod.getName()));
                addInfo(
                        list,
                        new ChatComponentText("Ticking"),
                        new ChatComponentText(tile.canUpdate() ? "true" : "false"));
                info.setTag("text", list);

                IChatComponent title = new ChatComponentText(tile.getClass().getSimpleName());

                info.setString("title", IChatComponent.Serializer.func_150696_a(title));
            }

            return nbt;
        }
    }

    private static class CmdEntity extends CmdNBT {

        private CmdEntity() {
            super("entity");
        }

        @Override
        public NBTTagCompound editNBT(EntityPlayerMP player, NBTTagCompound info, String[] args)
                throws CommandException {
            checkArgs(player, args, 1);
            int id = parseInt(player, args[0]);
            Entity entity = player.worldObj.getEntityByID(id);
            NBTTagCompound nbt = new NBTTagCompound();

            if (entity != null) {
                info.setString("type", "entity");
                info.setInteger("id", id);
                entity.writeToNBT(nbt);

                NBTTagList list = new NBTTagList();
                addInfo(list, new ChatComponentText("Class"), new ChatComponentText(entity.getClass().getName()));
                String key = EntityList.getEntityString(entity);
                addInfo(list, new ChatComponentText("ID"), new ChatComponentText(key == null ? "null" : key));

                ModContainer mod = null;
                for (ModContainer m : Loader.instance().getModList()) {
                    if (entity.getClass().getName().contains(m.getModId())) {
                        mod = m;
                        break;
                    }
                }

                addInfo(
                        list,
                        new ChatComponentText("Mod"),
                        new ChatComponentText(mod == null ? "null" : mod.getName()));
                info.setTag("text", list);
                info.setString("title", IChatComponent.Serializer.func_150696_a(new ChatComponentText(key)));
            }

            return nbt;
        }
    }

    private static class CmdPlayer extends CmdNBT {

        private CmdPlayer() {
            super("player");
        }

        @Override
        public List<String> getCommandAliases() {
            return Collections.singletonList("me");
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return index == 0;
        }

        @Override
        public NBTTagCompound editNBT(EntityPlayerMP player, NBTTagCompound info, String[] args)
                throws CommandException {
            ForgePlayer p = CommandUtils.getSelfOrOther(player, args, 0);
            info.setBoolean("online", p.isOnline());
            info.setString("type", "player");
            info.setString("id", p.getId().toString());
            NBTTagCompound nbt = p.getPlayerNBT();
            nbt.removeTag("id");

            NBTTagList list = new NBTTagList();
            addInfo(list, new ChatComponentText("Name"), new ChatComponentText(player.getGameProfile().getName()));
            addInfo(list, new ChatComponentText("Display Name"), new ChatComponentText(player.getDisplayName()));
            addInfo(list, new ChatComponentText("UUID"), new ChatComponentText(player.getUniqueID().toString()));
            addInfo(list, new ChatComponentText("Team"), new ChatComponentText(p.team.getId()));
            info.setTag("text", list);
            info.setString(
                    "title",
                    IChatComponent.Serializer.func_150696_a(new ChatComponentText(player.getDisplayName())));
            return nbt;
        }
    }

    private static class CmdItem extends CmdNBT {

        private CmdItem() {
            super("item");
        }

        @Override
        public NBTTagCompound editNBT(EntityPlayerMP player, NBTTagCompound info, String[] args) {
            info.setString("type", "item");
            return player.getHeldItem().writeToNBT(new NBTTagCompound());
        }
    }
}
