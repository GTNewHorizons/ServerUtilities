package serverutils.net;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import serverutils.integration.navigator.NavigatorIntegration;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageNavigatorUpdateKnown extends MessageToClient {

    private LongSet toRemove;

    public MessageNavigatorUpdateKnown() {}

    public MessageNavigatorUpdateKnown(@Nonnull LongSet toRemove) {
        this.toRemove = toRemove;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        for (long pos : toRemove) {
            data.writeLong(pos);
        }
    }

    @Override
    public void readData(DataIn data) {
        toRemove = new LongOpenHashSet();
        while (data.isReadable()) {
            toRemove.add(data.readLong());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        for (long pos : toRemove) {
            NavigatorIntegration.removeChunk(CoordinatePacker.unpackX(pos), CoordinatePacker.unpackZ(pos));
        }
    }
}
