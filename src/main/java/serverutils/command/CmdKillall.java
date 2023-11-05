package serverutils.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;

public class CmdKillall extends CmdBase {

    private static final Predicate<Entity> ALL = entity -> true;
    private static final Predicate<Entity> DEFAULT = entity -> {
        if (entity instanceof EntityPlayer) {
            return false;
        } else if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
            return true;
        }

        return entity instanceof EntityLivingBase;
    };

    private static final Predicate<Entity> ITEM = entity -> entity instanceof EntityItem;
    private static final Predicate<Entity> XP = entity -> entity instanceof EntityXPOrb;
    private static final Predicate<Entity> MOB = entity -> entity instanceof IMob;
    private static final Predicate<Entity> ANIMAL = entity -> entity instanceof EntityAnimal;
    private static final Predicate<Entity> LIVING = entity -> entity instanceof EntityLivingBase;
    private static final Predicate<Entity> PLAYER = entity -> entity instanceof EntityPlayer;
    private static final Predicate<Entity> NON_LIVING = entity -> !(entity instanceof EntityLivingBase);
    private static final Predicate<Entity> NON_PLAYER = entity -> !(entity instanceof EntityPlayer);
    private static final List<String> TAB = Arrays.asList(
            "default",
            "all",
            "items",
            "xp",
            "monsters",
            "animals",
            "living",
            "players",
            "non_living",
            "non_players");

    public CmdKillall() {
        super("killall", Level.OP);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsFromIterableMatchingLastWord(args, TAB);
        } else if (args.length == 2) {
            return getListOfStringsFromIterableMatchingLastWord(args, CommandUtils.getDimensionNames());
        }

        return super.addTabCompletionOptions(sender, args);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Predicate<Entity> predicate = DEFAULT;
        String type = "default";

        if (args.length >= 1) {
            switch (args[0]) {
                case "all":
                    predicate = ALL;
                    type = "all";
                    break;
                case "item":
                case "items":
                    predicate = ITEM;
                    type = "items";
                    break;
                case "xp":
                case "experience":
                    predicate = XP;
                    type = "xp";
                    break;
                case "mob":
                case "mobs":
                case "monster":
                case "monsters":
                    predicate = MOB;
                    type = "monsters";
                    break;
                case "animal":
                case "animals":
                    predicate = ANIMAL;
                    type = "animals";
                    break;
                case "living":
                case "alive":
                    predicate = LIVING;
                    type = "living";
                    break;
                case "player":
                case "players":
                    predicate = PLAYER;
                    type = "players";
                    break;
                case "non_living":
                    predicate = NON_LIVING;
                    type = "non_living";
                    break;
                case "non_players":
                    predicate = NON_PLAYER;
                    type = "non_players";
                    break;
            }
        }

        OptionalInt dimension = CommandUtils.parseDimension(sender, args, 1);

        int killed = 0;

        for (World world : getCommandSenderAsPlayer(sender).mcServer.worldServers) {
            for (Entity entity : new ArrayList<Entity>(world.loadedEntityList)) {
                if (predicate.test(entity) && (!dimension.isPresent() || dimension.getAsInt() == entity.dimension)) {
                    entity.setDead();
                    killed++;
                }
            }
        }

        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.killed_entities", killed, type));
    }
}
