package serverutils.utils.mod.cmd.admin;

import java.io.File;
import java.util.UUID;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;

import com.mojang.authlib.GameProfile;

import latmod.lib.*;
import serverutils.lib.*;
import serverutils.lib.api.cmd.*;
import serverutils.lib.api.item.StringIDInvLoader;
import serverutils.utils.world.*;

public class CmdPlayerLM extends CommandSubLM {

    public CmdPlayerLM() {
        super("player_lm", CommandLevel.OP);

        if (ServerUtilitiesLib.DEV_ENV) add(new CmdAddFake("add_fake"));
        add(new CmdDelete("delete"));
        add(new CmdLoadInv("load_inv"));
        add(new CmdSaveInv("save_inv"));
    }

    public static class CmdAddFake extends CommandLM {

        public CmdAddFake(String s) {
            super(s, CommandLevel.OP);
        }

        public String getCommandUsage(ICommandSender ics) {
            return '/' + commandName + " <player>";
        }

        public Boolean getUsername(String[] args, int i) {
            return (i == 0) ? Boolean.FALSE : null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 2);

            UUID id = LMUtils.fromString(args[0]);
            if (id == null) return error(new ChatComponentText("Invalid UUID!"));

            if (LMWorldServer.inst.getPlayer(id) != null || LMWorldServer.inst.getPlayer(args[1]) != null)
                return error(new ChatComponentText("Player already exists!"));

            LMPlayerServer p = new LMPlayerServer(
                    LMWorldServer.inst,
                    LMPlayerServer.nextPlayerID(),
                    new GameProfile(id, args[1]));
            LMWorldServer.inst.playerMap.put(p.getPlayerID(), p);
            p.refreshStats();

            return new ChatComponentText("Fake player " + args[1] + " added!");
        }
    }

    public static class CmdDelete extends CommandLM {

        public CmdDelete(String s) {
            super(s, CommandLevel.OP);
        }

        public String getCommandUsage(ICommandSender ics) {
            return '/' + commandName + " <player>";
        }

        public Boolean getUsername(String[] args, int i) {
            return (i == 0) ? Boolean.FALSE : null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 1);
            LMPlayerServer p = LMPlayerServer.get(args[0]);
            if (p.isOnline()) return error(new ChatComponentText("The player must be offline!"));
            LMWorldServer.inst.playerMap.remove(p.getPlayerID());
            return new ChatComponentText("Player removed!");
        }
    }

    public static class CmdLoadInv extends CommandLM {

        public CmdLoadInv(String s) {
            super(s, CommandLevel.OP);
        }

        public String getCommandUsage(ICommandSender ics) {
            return '/' + commandName + " <player>";
        }

        public Boolean getUsername(String[] args, int i) {
            return (i == 0) ? Boolean.FALSE : null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 1);
            LMPlayerServer p = LMPlayerServer.get(args[0]);
            if (!p.isOnline()) error(new ChatComponentText("The player must be online!"));

            try {
                EntityPlayerMP ep = p.getPlayer();
                String filename = ep.getCommandSenderName();
                if (args.length == 2) filename = "custom/" + args[1];
                // TODO: Backwards Compatibility?
                NBTTagCompound tag = LMNBTUtils.readMap(
                        new File(ServerUtilitiesLib.folderLocal, "serverutils/playerinvs/" + filename + ".dat"));

                StringIDInvLoader.readInvFromNBT(ep.inventory, tag, "Inventory");

                if (ServerUtilitiesLib.isModInstalled(OtherMods.BAUBLES))
                    StringIDInvLoader.readInvFromNBT(BaublesHelper.getBaubles(ep), tag, "Baubles");
            } catch (Exception e) {
                if (ServerUtilitiesLib.DEV_ENV) e.printStackTrace();
                return error(new ChatComponentText("Failed to load inventory!"));
            }

            return new ChatComponentText("Inventory loaded!");
        }
    }

    public static class CmdSaveInv extends CommandLM {

        public CmdSaveInv(String s) {
            super(s, CommandLevel.OP);
        }

        public String getCommandUsage(ICommandSender ics) {
            return '/' + commandName + " <player>";
        }

        public Boolean getUsername(String[] args, int i) {
            return (i == 0) ? Boolean.FALSE : null;
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            checkArgs(args, 1);
            LMPlayerServer p = LMPlayerServer.get(args[0]);
            if (!p.isOnline()) error(new ChatComponentText("The player must be online!"));

            try {
                EntityPlayerMP ep = p.getPlayer();
                NBTTagCompound tag = new NBTTagCompound();
                StringIDInvLoader.writeInvToNBT(ep.inventory, tag, "Inventory");

                if (ServerUtilitiesLib.isModInstalled(OtherMods.BAUBLES))
                    StringIDInvLoader.writeInvToNBT(BaublesHelper.getBaubles(ep), tag, "Baubles");

                String filename = ep.getCommandSenderName();
                if (args.length == 2) filename = "custom/" + args[1];
                // TODO: Backwards Compatibility?
                LMNBTUtils.writeMap(
                        LMFileUtils.newFile(
                                new File(
                                        ServerUtilitiesLib.folderLocal,
                                        "serverutils/playerinvs/" + filename + ".dat")),
                        tag);
            } catch (Exception e) {
                if (ServerUtilitiesLib.DEV_ENV) e.printStackTrace();
                return error(new ChatComponentText("Failed to save inventory!"));
            }

            return new ChatComponentText("Inventory saved!");
        }
    }
}
