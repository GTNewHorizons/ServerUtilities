package serverutils.lib.util;

import java.util.Collection;
import java.util.function.Function;

public abstract class StringJoiner {

    private static final Function<Object, String> DEFAULT_STRING_GETTER = String::valueOf;

    public static StringJoiner with(String string) {
        if (string.isEmpty()) {
            return WithString.WITH_NOTHING;
        } else if (string.length() == 1) {
            return with(string.charAt(0));
        } else if (string.equals(WithString.WITH_COMMA_AND_SPACE.s)) {
            return WithString.WITH_COMMA_AND_SPACE;
        }

        return new WithString(string);
    }

    public static StringJoiner with(char character) {
        return switch (character) {
            case ',' -> WithChar.WITH_COMMA;
            case '.' -> WithChar.WITH_PERIOD;
            case ' ' -> WithChar.WITH_SPACE;
            default -> new WithChar(character);
        };
    }

    public static StringJoiner properties() {
        return new PropertiesJoiner();
    }

    private static class WithString extends StringJoiner {

        private static final WithString WITH_NOTHING = new WithString("");
        private static final WithString WITH_COMMA_AND_SPACE = new WithString(", ");

        private final String s;

        private WithString(String _s) {
            s = _s;
        }

        @Override
        protected void append(StringBuilder builder) {
            builder.append(s);
        }
    }

    private static class WithChar extends StringJoiner {

        private static final WithChar WITH_COMMA = new WithChar(',');
        private static final WithChar WITH_PERIOD = new WithChar('.');
        private static final WithChar WITH_SPACE = new WithChar(' ');

        private final char c;

        private WithChar(char _c) {
            c = _c;
        }

        @Override
        protected void append(StringBuilder builder) {
            builder.append(c);
        }
    }

    private static class PropertiesJoiner extends StringJoiner {

        private int index = 0;

        @Override
        protected void append(StringBuilder builder) {
            if (index % 2 == 0) {
                builder.append(", ");
            } else {
                builder.append('=');
            }

            index++;
        }
    }

    protected abstract void append(StringBuilder builder);

    public String joinObjects(Object... objects) {
        if (objects.length == 0) {
            return "";
        } else if (objects.length == 1) {
            return String.valueOf(objects[0]);
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (Object object : objects) {
            if (first) {
                first = false;
            } else {
                append(builder);
            }

            builder.append(object);
        }

        return builder.toString();
    }

    public String joinStrings(String[] strings, int start, int end) {
        if (strings.length == 0) {
            return "";
        } else if (strings.length == 1 && start == 0) {
            return strings[0];
        }

        StringBuilder builder = new StringBuilder();

        for (int i = start; i < end; i++) {
            if (i > start) {
                append(builder);
            }

            builder.append(strings[i]);
        }

        return builder.toString();
    }

    public String joinStrings(String[] strings) {
        return joinStrings(strings, 0, strings.length);
    }

    public <T> String join(Iterable<T> objects, Function<T, String> stringGetter) {
        if (objects instanceof Collection) {
            Collection<T> c = (Collection<T>) objects;

            if (c.isEmpty()) {
                return "";
            } else if (c.size() == 1) {
                return stringGetter.apply(c.iterator().next());
            }
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (T object : objects) {
            if (first) {
                first = false;
            } else {
                append(builder);
            }

            builder.append(stringGetter.apply(object));
        }

        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public String join(Iterable iterable) {
        return join(iterable, DEFAULT_STRING_GETTER);
    }
}
