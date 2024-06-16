package serverutils.lib.util;

public class FinalIDObject implements IWithID {

    private final String id;
    private final int idFlag;

    public FinalIDObject(String _id, int flags) {
        id = StringUtils.getID(_id, flags);
        idFlag = flags;
    }

    public FinalIDObject(String id) {
        this(id, StringUtils.FLAG_ID_DEFAULTS);
    }

    @Override
    public final String getId() {
        return id;
    }

    public final int getIdFlag() {
        return idFlag;
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
