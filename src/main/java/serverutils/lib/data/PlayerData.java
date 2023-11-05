package serverutils.lib.data;

public abstract class PlayerData implements NBTDataStorage.Data {

    public final ForgePlayer player;

    public PlayerData(ForgePlayer p) {
        player = p;
    }

    @Override
    public final int hashCode() {
        return getId().hashCode() * 31 + player.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PlayerData playerData) {
            return player.equalsPlayer(playerData.player) && getId().equals(playerData.getId());
        }

        return false;
    }

    public final String toString() {
        return player.getName() + ':' + getId();
    }
}
