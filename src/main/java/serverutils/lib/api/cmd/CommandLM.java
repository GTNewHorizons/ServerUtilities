package serverutils.lib.api.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMListUtils;
import serverutils.lib.ServerUtilitiesLib;

public abstract class CommandLM extends CommandBase {

    public final String commandName;
    public final CommandLevel level;

    public CommandLM(String s, CommandLevel l) {
        commandName = ((s == null) ? "" : s).trim();
        level = (l == null || commandName.isEmpty()) ? CommandLevel.NONE : l;
    }

    public int getRequiredPermissionLevel() {
        return level.requiredPermsLevel();
    }

    public boolean canCommandSenderUseCommand(ICommandSender ics) {
        if (ServerUtilitiesLib.getEffectiveSide().isClient()) return true;
        return level != CommandLevel.NONE && (level == CommandLevel.ALL || !ServerUtilitiesLib.isDedicatedServer()
                || super.canCommandSenderUseCommand(ics));
    }

    public final String getCommandName() {
        return commandName;
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName;
    }

    public final void processCommand(ICommandSender ics, String[] args) throws CommandException {
        if (!level.isEnabled()) throw new FeatureDisabledException();
        if (args == null) args = new String[0];
        IChatComponent s = onCommand(ics, args);
        if (s != null) ServerUtilitiesLib.printChat(ics, s);
        onPostCommand(ics, args);
    }

    public abstract IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException;

    public static IChatComponent error(IChatComponent c) {
        c.getChatStyle().setColor(EnumChatFormatting.RED);
        return c;
    }

    public void onPostCommand(ICommandSender ics, String[] args) throws CommandException {}

    @SuppressWarnings("all")
    public final List<String> addTabCompletionOptions(ICommandSender ics, String[] args) {
        if (!level.isEnabled()) return null;
        try {
            String[] s = getTabStrings(ics, args, args.length - 1);
            if (s != null && s.length > 0) {
                if (sortStrings(ics, args, args.length - 1)) Arrays.sort(s);
                return getListOfStringsMatchingLastWord(args, s);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Boolean getUsername(String[] args, int i) {
        return null;
    }

    public final boolean isUsernameIndex(String[] args, int i) {
        return false;
    }

    public static boolean isArg(String[] args, int i, String... s) {
        if (args != null && i >= 0 && i < args.length) {
            for (int j = 0; j < s.length; j++) if (args[i].equals(s[j])) return true;
        }

        return false;
    }

    public String[] getTabStrings(ICommandSender ics, String args[], int i) throws CommandException {
        Boolean b = getUsername(args, i);
        if (b == null) return new String[0];
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            return ServerUtilitiesLib.serverUtilitiesIntegration.getPlayerNames(b);
        ArrayList<String> l = new ArrayList<>();
        for (EntityPlayerMP ep : ServerUtilitiesLib.getAllOnlinePlayers(null)) l.add(ep.getGameProfile().getName());
        return LMListUtils.toStringArray(l);
    }

    public boolean sortStrings(ICommandSender ics, String args[], int i) {
        return getUsername(args, i) == null;
    }

    public static void checkArgs(String[] args, int i) throws CommandException {
        if (args == null || args.length < i) throw new MissingArgsException();
    }

    public static void checkArgsStrong(String[] args, int i) throws CommandException {
        if (args == null || args.length != i) throw new MissingArgsException();
    }

    public static List<EntityPlayerMP> findPlayers(ICommandSender ics, String arg) throws CommandException {
        EntityPlayerMP player = null;

        if (arg.equals("@a")) return ServerUtilitiesLib.getAllOnlinePlayers(null);
        else if (arg.equals("@r")) {
            List<EntityPlayerMP> l = ServerUtilitiesLib.getAllOnlinePlayers(null);
            if (!l.isEmpty()) player = l.get(ics.getEntityWorld().rand.nextInt(l.size()));
        } else if (arg.equals("@p")) {
            if (ics instanceof EntityPlayerMP) return Collections.singletonList((EntityPlayerMP) ics);

            List<EntityPlayerMP> l = ServerUtilitiesLib.getAllOnlinePlayers(null);
            if (l.size() < 2) return l;

            EntityPlayerMP closest = null;
            double distSq = Double.POSITIVE_INFINITY;
            ChunkCoordinates c = ics.getPlayerCoordinates();

            for (EntityPlayerMP ep : l) {
                if (closest == null) closest = ep;
                else {
                    double d = ep.getDistanceSq(c.posX + 0.5D, c.posY + 0.5D, c.posZ + 0.5D);

                    if (d < distSq) {
                        distSq = d;
                        closest = ep;
                    }
                }
            }

            return Collections.singletonList(closest);
        } else {
            player = (EntityPlayerMP) ics.getEntityWorld().getPlayerEntityByName(arg);
        }

        if (player == null) return new ArrayList<>();
        return Collections.singletonList(player);
    }
}
