package serverutils.ranks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigNull;
import serverutils.lib.config.ConfigString;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.config.RankConfigValueInfo;
import serverutils.lib.util.FinalIDObject;
import serverutils.lib.util.StringJoiner;

public class Rank extends FinalIDObject implements Comparable<Rank> {

    public static final String NODE_PARENT = "parent";
    public static final String NODE_DEFAULT_PLAYER = "default_player_rank";
    public static final String NODE_DEFAULT_OP = "default_op_rank";
    public static final String NODE_PRIORITY = "priority";
    public static final String NODE_COMMAND = "command";

    @Deprecated
    public static final String NODE_POWER = "power";

    public static class Entry implements Comparable<Entry> {

        public final String node;
        public String value = "";
        public String comment = "";

        public Entry(String n) {
            node = n;
        }

        @Override
        public int compareTo(Entry o) {
            return node.compareTo(o.node);
        }

        @Override
        public String toString() {
            return node + ":" + value;
        }
    }

    public final Ranks ranks;
    private int priority;
    protected IChatComponent displayName;
    protected Set<Rank> parents;
    public final Map<String, Entry> permissions;
    public String comment;

    public Rank(Ranks r, String id) {
        super(id);
        displayName = new ChatComponentText(getId());
        displayName.getChatStyle().setColor(EnumChatFormatting.DARK_GREEN);
        ranks = r;
        permissions = new LinkedHashMap<>();
        comment = "";
        priority = -1;
    }

    public int getPriority() {
        if (priority == -1) {
            String s = getLocalPermission(NODE_POWER);
            String s1 = getLocalPermission(NODE_PRIORITY);
            int pow = s.isEmpty() ? 0 : Integer.parseInt(s);
            int pri = s1.isEmpty() ? 0 : Integer.parseInt(s1);
            int actualPriority = Math.max(pri, pow);

            priority = MathHelper.clamp_int(actualPriority, 0, Integer.MAX_VALUE - 1);
        }

        return priority;
    }

    public boolean isPlayer() {
        return false;
    }

    public void clearCache() {
        parents = null;
        priority = -1;
    }

    public IChatComponent getDisplayName() {
        return displayName;
    }

    public Set<Rank> getParents() {
        if (parents == null) {
            List<Rank> list = new ArrayList<>();

            for (String s : getLocalPermission(NODE_PARENT).split(",")) {
                Rank r = ranks.getRank(s.trim());

                if (r != null && !r.isPlayer()) {
                    list.add(r);
                }
            }

            list.sort(null);
            parents = new LinkedHashSet<>(list);
        }

        return parents;
    }

    public Set<Rank> getActualParents() {
        return getParents();
    }

    public boolean addParent(@Nullable Rank rank) {
        if (rank == null || rank.isPlayer()) {
            return false;
        }

        parents = getParents();

        if (parents.add(rank)) {
            setPermission(NODE_PARENT, StringJoiner.with(", ").join(parents));
            parents = null;
            return true;
        }

        return false;
    }

    public boolean removeParent(Rank rank) {
        parents = getParents();

        if (parents.remove(rank)) {
            setPermission(NODE_PARENT, StringJoiner.with(", ").join(parents));
            parents = null;
            return true;
        }

        return false;
    }

    public boolean clearParents() {
        priority = -1;
        parents = null;
        return setPermission(NODE_PARENT, "") != null;
    }

    @Nullable
    public Entry setPermission(String node, @Nullable Object value) {
        String v = value == null ? "" : value.toString();

        if (v.isEmpty()) {
            return permissions.remove(node);
        }

        Entry entry = permissions.get(node);

        if (entry != null) {
            if (!entry.value.equals(v)) {
                entry.value = v;
                return entry;
            }

            return null;
        }

        entry = new Entry(node);
        entry.value = v;
        permissions.put(node, entry);
        return entry;
    }

    public String getLocalPermission(String node) {
        Entry entry = permissions.get(node);
        return entry == null ? "" : entry.value;
    }

    public String getPermission(String node) {
        return getPermission(node, node, false);
    }

    public String getPermission(String originalNode, String node, boolean recursive) {
        String s = getLocalPermission(node);

        if (!s.isEmpty()) {
            return s;
        }

        for (Rank parent : getActualParents()) {
            s = parent.getPermission(node);

            if (!s.isEmpty()) {
                return s;
            }
        }

        if (recursive) {
            int i = node.lastIndexOf('.');

            if (i != -1) {
                return getPermission(originalNode, node.substring(0, i), true);
            } else if (!node.equals("*")) {
                return getPermission(originalNode, "*", true);
            }
        }

        return "";
    }

    public ConfigValue getPermissionValue(String node) {
        return getPermissionValue(node, node, false);
    }

    public ConfigValue getPermissionValue(String originalNode, String node, boolean recursive) {
        String s = getPermission(originalNode, node, recursive);

        if (s.isEmpty()) {
            return ConfigNull.INSTANCE;
        } else if (s.equals("true")) {
            return new ConfigBoolean(true);
        } else if (s.equals("false")) {
            return new ConfigBoolean(false);
        }

        RankConfigValueInfo info = RankConfigAPI.getHandler().getInfo(originalNode);

        if (info != null) {
            ConfigValue value = info.defaultValue.copy();
            value.setValueFromString(null, s, false);
            return value;
        }

        return new ConfigString(s);
    }

    public boolean add() {
        return ranks.ranks.put(getId(), this) != this;
    }

    public boolean remove() {
        if (ranks.ranks.remove(getId()) != null) {
            for (Rank rank : ranks.ranks.values()) {
                rank.removeParent(this);
            }

            for (Rank rank : ranks.playerRanks.values()) {
                rank.removeParent(this);
            }

            return true;
        }

        return false;
    }

    @Override
    public int compareTo(Rank o) {
        return Integer.compare(o.getPriority(), getPriority());
    }

    public boolean isDefaultPlayerRank() {
        return getLocalPermission(NODE_DEFAULT_PLAYER).equals("true");
    }

    public boolean isDefaultOPRank() {
        return getLocalPermission(NODE_DEFAULT_OP).equals("true");
    }
}
