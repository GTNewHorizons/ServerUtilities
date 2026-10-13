package serverutils.task.backup;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selects which backups to keep. Entries must be independent: deleting one never affects another. */
public final class BackupRetention {

    // 1970-01-05 was a Monday; day-sized buckets share its UTC midnight alignment.
    private static final long BUCKET_ORIGIN = 4 * 86_400_000L;

    private BackupRetention() {}

    static final class Rule {

        final String text;
        final long age;
        final long interval;

        Rule(String text, long age, long interval) {
            this.text = text;
            this.age = age;
            this.interval = interval;
        }
    }

    static List<Rule> parse(List<String> policy) {
        List<Rule> rules = new ArrayList<>();
        for (String value : policy) {
            String[] parts = value == null ? new String[0] : value.trim().split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid retention rule: " + value);
            String age = parts[0].trim();
            String interval = parts[1].trim();
            rules.add(
                    new Rule(
                            age + ":" + interval,
                            age.equals("forever") ? Long.MAX_VALUE : BackupDuration.parse(age),
                            interval.equals("all") ? 0 : BackupDuration.parse(interval)));
        }
        return rules;
    }

    public static final class Plan<T extends BackupEntry> {

        public final Map<T, String> keep = new LinkedHashMap<>();
        public final Map<T, String> delete = new LinkedHashMap<>();
        final Set<T> sizeCandidates = new LinkedHashSet<>();
        final Set<T> sizeExempt = new HashSet<>();
        final List<T> entries = new ArrayList<>();
        final Map<T, String[]> labels = new HashMap<>();
        final Map<T, Long> keptUntil = new HashMap<>();
        final Set<T> preserved = new HashSet<>();
        public long remainingSize;
        public long remainingRotationSize;

        private void keep(T entry, String reason) {
            keep.merge(entry, reason, (previous, added) -> previous + "; " + added);
        }

        /** Every scanned entry, oldest first. */
        public List<T> entries() {
            return entries;
        }

        /** Short display reason: a lang key followed by its arguments. */
        public String[] label(T entry) {
            return labels.getOrDefault(entry, reason("rule", ""));
        }

        /** When finite rules stop keeping this entry, or -1 if no finite rule decides it. */
        public long keptUntil(T entry) {
            return keptUntil.getOrDefault(entry, -1L);
        }

        /** Kept regardless of rules: unreadable, future-dated or protected custom entries. */
        public boolean isPreserved(T entry) {
            return preserved.contains(entry);
        }
    }

    public static <T extends BackupEntry> Plan<T> plan(List<T> entries, List<String> policy, boolean deleteCustom,
            long maxSize) throws InterruptedIOException {
        Plan<T> plan = select(entries, parse(policy), deleteCustom, maxSize);
        checkInterrupted();
        return plan;
    }

    static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup retention cancelled");
    }

    static <T extends BackupEntry> Plan<T> select(List<T> input, List<Rule> rules, boolean deleteCustom, long maxSize) {
        long now = System.currentTimeMillis();
        List<T> entries = new ArrayList<>(input);
        entries.sort(
                Comparator.comparingLong((T entry) -> entry.createdAt()).reversed().thenComparing(BackupEntry::label));
        Plan<T> plan = new Plan<>();
        Set<T> protectedEntries = new HashSet<>();
        Set<String> worlds = new HashSet<>();
        Set<T> latest = new HashSet<>();
        Map<T, Rule> longestRule = new HashMap<>();
        Map<T, Rule> replacedIn = new HashMap<>();
        for (T entry : entries) {
            plan.remainingSize = Math.addExact(plan.remainingSize, entry.size());
            if (entry.problem() != null || entry.createdAt() > now || (!deleteCustom && entry.isCustom())) {
                plan.sizeExempt.add(entry);
                plan.keep(
                        entry,
                        entry.problem() != null ? entry.problem()
                                : entry.createdAt() > now ? "Timestamp is in the future" : "Protected custom backup");
                plan.labels.put(
                        entry,
                        reason(entry.problem() != null ? "unreadable" : entry.createdAt() > now ? "future" : "custom"));
                plan.preserved.add(entry);
                protectedEntries.add(entry);
                continue;
            }
            if (worlds.add(entry.world())) {
                plan.keep(entry, "Latest backup for world");
                latest.add(entry);
                protectedEntries.add(entry);
            }
        }
        for (Rule rule : rules) {
            Map<String, Set<Long>> buckets = new HashMap<>();
            for (T entry : entries) {
                if (entry.problem() != null || entry.createdAt() > now
                        || (!deleteCustom && entry.isCustom())
                        || now - entry.createdAt() > rule.age)
                    continue;
                if (rule.interval == 0 || buckets.computeIfAbsent(entry.world(), key -> new HashSet<>())
                        .add(Math.floorDiv(entry.createdAt() - BUCKET_ORIGIN, rule.interval))) {
                    plan.keep(entry, rule.text);
                    longestRule.merge(entry, rule, (a, b) -> b.age > a.age ? b : a);
                    if (rule.age == Long.MAX_VALUE) {
                        protectedEntries.add(entry);
                        plan.sizeExempt.add(entry);
                    }
                } else {
                    // The finest interval best explains why a newer backup took this one's place.
                    replacedIn.merge(entry, rule, (a, b) -> b.interval < a.interval ? b : a);
                }
            }
        }
        for (T entry : entries) {
            if (!plan.keep.containsKey(entry)) {
                plan.delete.put(entry, "Outside retention policy or superseded in time bucket");
                plan.remainingSize -= entry.size();
            } else if (!plan.sizeExempt.contains(entry)) {
                plan.remainingRotationSize = Math.addExact(plan.remainingRotationSize, entry.size());
            }
        }
        // Only finite-retention entries may be sacrificed to the size limit, oldest first.
        for (int i = entries.size() - 1; i >= 0; i--) {
            T entry = entries.get(i);
            if (protectedEntries.contains(entry) || !plan.keep.containsKey(entry)) continue;
            plan.sizeCandidates.add(entry);
            if (maxSize <= 0 || plan.remainingRotationSize <= maxSize) continue;
            plan.keep.remove(entry);
            plan.delete.put(entry, "Folder size limit");
            plan.remainingSize -= entry.size();
            plan.remainingRotationSize -= entry.size();
        }
        for (int i = entries.size() - 1; i >= 0; i--) {
            T entry = entries.get(i);
            plan.entries.add(entry);
            if (plan.preserved.contains(entry)) continue;
            Rule rule = longestRule.get(entry);
            if (plan.delete.containsKey(entry)) {
                Rule replaced = replacedIn.get(entry);
                plan.labels.put(
                        entry,
                        "Folder size limit".equals(plan.delete.get(entry)) ? reason("size")
                                : replaced != null ? reason("replaced", replaced.text) : reason("older"));
            } else if (latest.contains(entry)) {
                plan.labels.put(entry, rule == null ? reason("latest") : reason("latest_rule", rule.text));
            } else {
                plan.labels.put(entry, reason("rule", rule.text));
                if (rule.age != Long.MAX_VALUE) plan.keptUntil.put(entry, entry.createdAt() + rule.age);
            }
        }
        return plan;
    }

    private static String[] reason(String name, String... args) {
        String[] reason = new String[args.length + 1];
        reason[0] = "cmd.backup_prune_reason_" + name;
        System.arraycopy(args, 0, reason, 1, args.length);
        return reason;
    }
}
