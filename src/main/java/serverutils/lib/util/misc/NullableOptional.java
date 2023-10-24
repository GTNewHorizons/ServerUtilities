package serverutils.lib.util.misc;

import javax.annotation.Nullable;

import serverutils.lib.util.CommonUtils;

public abstract class NullableOptional<T> {

    private static final NullableOptional<?> EMPTY = new NullableOptional<Object>() {

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public boolean isNonNull() {
            return false;
        }

        @Nullable
        @Override
        public Object get() {
            throw new IllegalStateException();
        }

        @Override
        public Object getNonNull() {
            throw new NullPointerException();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "empty";
        }
    };

    private static final NullableOptional<?> NULL = new NullableOptional<Object>() {

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public boolean isNonNull() {
            return false;
        }

        @Nullable
        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object getNonNull() {
            throw new NullPointerException();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "null";
        }
    };

    private static class Value<E> extends NullableOptional<E> {

        private final E value;

        private Value(E v) {
            value = v;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public boolean isNonNull() {
            return true;
        }

        @Override
        public E get() {
            return value;
        }

        @Override
        public E getNonNull() {
            return value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Value && value.equals(((Value) obj).value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static NullableOptional empty() {
        return EMPTY;
    }

    public static <E> NullableOptional<E> of(@Nullable E object) {
        return object == null ? CommonUtils.cast(NULL) : new Value<>(object);
    }

    private NullableOptional() {}

    public abstract boolean isPresent();

    public abstract boolean isNonNull();

    @Nullable
    public abstract T get();

    public abstract T getNonNull();
}
