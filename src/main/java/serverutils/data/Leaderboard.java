package serverutils.data;

import java.util.Comparator;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Predicate;

import net.minecraft.stats.StatBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;

public class Leaderboard {

    public final ResourceLocation id;
    private final IChatComponent title;
    private final Function<ForgePlayer, IChatComponent> playerToValue;
    private final Comparator<ForgePlayer> comparator;
    private final Predicate<ForgePlayer> validValue;

    public Leaderboard(ResourceLocation _id, IChatComponent t, Function<ForgePlayer, IChatComponent> v,
            Comparator<ForgePlayer> c, Predicate<ForgePlayer> vv) {
        id = _id;
        title = t;
        playerToValue = v;
        comparator = c
                .thenComparing((o1, o2) -> o1.getDisplayNameString().compareToIgnoreCase(o2.getDisplayNameString()));
        validValue = vv;
    }

    public final IChatComponent getTitle() {
        return title;
    }

    public final Comparator<ForgePlayer> getComparator() {
        return comparator;
    }

    public final IChatComponent createValue(ForgePlayer player) {
        return playerToValue.apply(player);
    }

    public final boolean hasValidValue(ForgePlayer player) {
        return validValue.test(player);
    }

    public static class FromStat extends Leaderboard {

        public static final IntFunction<IChatComponent> DEFAULT = value -> new ChatComponentText(
                value <= 0 ? "0" : Integer.toString(value));
        public static final DoubleFunction<IChatComponent> PERCENTAGE = value -> new ChatComponentText(
                String.format("%.2f%%", value * 100.0));
        public static final IntFunction<IChatComponent> TIME = value -> new ChatComponentText(
                "[" + (int) (value / 72000D + 0.5D) + "h] " + Ticks.get(value).toTimeString());
        public static final LongFunction<IChatComponent> LONG_TIME = value -> new ChatComponentText(
                "[" + (long) (value / 72000D + 0.5D) + "h] " + Ticks.get(value).toTimeString());

        public FromStat(ResourceLocation id, IChatComponent t, StatBase statBase, boolean from0to1,
                IntFunction<IChatComponent> valueToString) {
            super(id, t, player -> valueToString.apply(player.stats().writeStat(statBase)), (o1, o2) -> {
                int i = Integer.compare(o1.stats().writeStat(statBase), o2.stats().writeStat(statBase));
                return from0to1 ? i : -i;
            }, player -> player.stats().writeStat(statBase) > 0);
        }

        public FromStat(ResourceLocation id, StatBase statBase, boolean from0to1,
                IntFunction<IChatComponent> valueToString) {
            this(id, StringUtils.color(statBase.func_150951_e(), null), statBase, from0to1, valueToString);
        }
    }
}
