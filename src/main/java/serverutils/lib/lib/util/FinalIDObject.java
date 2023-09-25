package serverutils.lib.lib.util;

public class FinalIDObject implements IWithID {

    private final String id;

    public FinalIDObject(String _id, int flags) {
        id = StringUtils.getID(_id, flags);
    }

    public FinalIDObject(String id) {
        this(id, StringUtils.FLAG_ID_DEFAULTS);
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof IWithID && id.equals(((IWithID) o).getId());
    }
}
