package serverutils.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatComponentTranslationFormatException;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.StringUtils;

public class CmdDumpStats extends CmdBase {

    private static final File statDumpFile = new File(ServerUtilities.SERVER_FOLDER + "stat_dump.txt");

    public CmdDumpStats() {
        super("dump_stats", Level.OP_OR_SP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        FileUtils.delete(statDumpFile);
        List<String> output = new ArrayList<>();
        List<StatBase> stats = StatList.allStats;

        List<String> achievementStats = new ArrayList<>();
        List<String> craftStats = new ArrayList<>();
        List<String> useStats = new ArrayList<>();
        List<String> itemsBrokeStats = new ArrayList<>();
        List<String> blocksMinedStats = new ArrayList<>();
        List<String> miscStats = new ArrayList<>();
        for (StatBase stat : stats) {
            String statId = stat.statId;

            // These two have to be added differently later on
            if (statId.startsWith("stat.entityKilledBy") || statId.startsWith("stat.killEntity")) {
                continue;
            }

            String statStr = getStatString(stat);
            if (statId.contains("achievement.")) {
                achievementStats.add(statStr);
                continue;
            } else if (statId.contains("craftItem.")) {
                craftStats.add(statStr);
                continue;
            } else if (statId.contains("useItem.")) {
                useStats.add(statStr);
                continue;
            } else if (statId.contains("breakItem.")) {
                itemsBrokeStats.add(statStr);
                continue;
            } else if (statId.contains("mineBlock.")) {
                blocksMinedStats.add(statStr);
                continue;
            }

            miscStats.add(statStr);
        }

        List<String> killStats = new ArrayList<>();
        for (EntityList.EntityEggInfo eggInfo : EntityList.entityEggs.values()) {
            String killedById = eggInfo.field_151513_e == null ? "null" : eggInfo.field_151513_e.statId;
            String killsId = eggInfo.field_151512_d == null ? "null" : eggInfo.field_151512_d.statId;
            String entityArg = StatCollector
                    .translateToLocal("entity." + EntityList.getStringFromID(eggInfo.spawnedID) + ".name");
            killStats.add(
                    killedById + " || " + StatCollector.translateToLocalFormatted("stat.entityKilledBy", entityArg, 0));
            killStats.add(killsId + " || " + StatCollector.translateToLocalFormatted("stat.entityKills", 0, entityArg));
        }

        addSection(output, "Misc. Stats", miscStats);
        addSection(output, "Achievement Stats", achievementStats);
        addSection(output, "Craft Stats", craftStats);
        addSection(output, "Use Stats", useStats);
        addSection(output, "Items Broke Stats", itemsBrokeStats);
        addSection(output, "Blocks Mined Stats", blocksMinedStats);
        addSection(output, "Kill Stats", killStats);

        FileUtils.saveSafe(statDumpFile, output);
        sender.addChatMessage(
                new ChatComponentText("Dumped " + stats.size() + " stats to file " + statDumpFile.getPath()));
    }

    private String getStatString(StatBase stat) {
        String base = stat.statId + " || ";
        IChatComponent statName = stat.statName;
        if (statName instanceof ChatComponentTranslation translation) {
            try {
                return base + translation.getUnformattedText();
            } catch (ChatComponentTranslationFormatException e) {
                return base + stat.statId;
            }
        }

        return base + statName.getUnformattedText();
    }

    private void addSection(List<String> output, String title, List<String> stats) {
        output.add("--------------------");
        output.add(title);
        output.add("--------------------");
        stats.sort(StringUtils.IGNORE_CASE_COMPARATOR);
        output.addAll(stats);
    }

}
