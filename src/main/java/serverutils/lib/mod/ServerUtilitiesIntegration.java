package serverutils.lib.mod;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import latmod.lib.ByteIOStream;
import latmod.lib.util.Phase;
import serverutils.lib.api.EventServerUtilitiesReload;
import serverutils.lib.api.EventServerUtilitiesWorldClient;
import serverutils.lib.api.EventServerUtilitiesWorldServer;
import serverutils.lib.api.friends.ILMPlayer;

public interface ServerUtilitiesIntegration {

    void onReloaded(EventServerUtilitiesReload e);

    void onServerUtilitiesWorldServer(EventServerUtilitiesWorldServer e);

    void onServerUtilitiesWorldClient(EventServerUtilitiesWorldClient e);

    void onServerUtilitiesWorldServerClosed();

    void onServerTick(World w);

    void onPlayerJoined(EntityPlayerMP player, Phase phase);

    ILMPlayer getLMPlayer(Object player);

    String[] getPlayerNames(boolean online);

    void writeWorldData(ByteIOStream io, EntityPlayerMP ep);

    void readWorldData(ByteIOStream io);

    boolean hasClientWorld();

    void renderWorld(float pt);

    void onTooltip(ItemTooltipEvent e);

    void onRightClick(PlayerInteractEvent e);
}
