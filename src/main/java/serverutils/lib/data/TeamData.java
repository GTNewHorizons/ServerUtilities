package serverutils.lib.data;

public abstract class TeamData implements NBTDataStorage.Data {

    public final ForgeTeam team;

    public TeamData(ForgeTeam t) {
        team = t;
    }

    @Override
    public final int hashCode() {
        return getId().hashCode() * 31 + team.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof TeamData) {
            return team.equalsTeam(((TeamData) o).team) && getId().equals(((TeamData) o).getId());
        }

        return false;
    }

    public final String toString() {
        return team.getId() + ':' + getId();
    }
}
