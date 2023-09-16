package serverutils.utils.mod.handlers.serverlib;

import java.io.File;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.*;

import com.google.gson.JsonElement;

import latmod.lib.*;
import latmod.lib.util.Phase;
import serverutils.lib.*;
import serverutils.lib.api.*;
import serverutils.lib.api.friends.ILMPlayer;
import serverutils.lib.api.item.LMInvUtils;
import serverutils.lib.mod.ServerUtilitiesIntegration;
import serverutils.utils.api.*;
import serverutils.utils.api.guide.ServerGuideFile;
import serverutils.utils.badges.ServerBadges;
import serverutils.utils.mod.ServerUtilsTicks;
import serverutils.utils.mod.config.*;
import serverutils.utils.mod.handlers.ServerUtilitiesChunkEventHandler;
import serverutils.utils.net.*;
import serverutils.utils.world.*;
import serverutils.utils.world.claims.ClaimedChunks;
import serverutils.utils.world.ranks.Ranks;

public class ServerUtilitiesLibraryIntegration implements ServerUtilitiesIntegration {

    private static boolean first_login, send_all;

    public void onReloaded(EventServerUtilitiesReload e) {
        ServerUtilitiesConfigGeneral.onReloaded(e.world.side);

        if (e.world.side.isServer()) {
            if (LMWorldServer.inst == null) return;

            for (LMPlayerServer p : LMWorldServer.inst.playerMap.values()) p.refreshStats();

            ServerGuideFile.CachedInfo.reload();
            Ranks.reload();
            ServerBadges.reload();

            ServerUtilitiesChunkEventHandler.instance.markDirty(null);
        }
    }

    public final void onServerUtilitiesWorldServer(EventServerUtilitiesWorldServer e) {
        File latmodFolder = new File(ServerUtilitiesLib.folderWorld, "LatMod/");

        LMWorldServer.inst = new LMWorldServer(latmodFolder);

        File file = new File(latmodFolder, "LMWorld.json");
        JsonElement obj = LMJsonUtils.fromJson(file);
        if (obj.isJsonObject()) LMWorldServer.inst.load(obj.getAsJsonObject(), Phase.PRE);

        new EventLMWorldServer.Loaded(LMWorldServer.inst, Phase.PRE).post();

        NBTTagCompound tagPlayers = LMNBTUtils.readMap(new File(latmodFolder, "LMPlayers.dat"));
        if (tagPlayers != null && tagPlayers.hasKey("Players")) {
            LMPlayerServer.lastPlayerID = tagPlayers.getInteger("LastID");
            LMWorldServer.inst.readPlayersFromServer(tagPlayers.getCompoundTag("Players"));
        }

        for (LMPlayerServer p : LMWorldServer.inst.playerMap.values()) p.setPlayer(null);

        if (obj.isJsonObject()) LMWorldServer.inst.load(obj.getAsJsonObject(), Phase.POST);

        file = new File(latmodFolder, "ClaimedChunks.json");

        if (file.exists()) {
            obj = LMJsonUtils.fromJson(file);
            if (obj.isJsonObject()) LMWorldServer.inst.claimedChunks.load(obj.getAsJsonObject());
        }

        new EventLMWorldServer.Loaded(LMWorldServer.inst, Phase.POST).post();

        ServerUtilsTicks.serverStarted();
    }

    public void onServerUtilitiesWorldClient(EventServerUtilitiesWorldClient e) {}

    public final void onServerUtilitiesWorldServerClosed() {
        LMWorldServer.inst.close();
        LMWorldServer.inst = null;
    }

    public final void onServerTick(World w) {
        if (w.provider.dimensionId == 0) {
            ServerUtilsTicks.update();
        }
    }

    public final void onPlayerJoined(EntityPlayerMP ep, Phase phase) {
        LMPlayerServer p = LMWorldServer.inst.getPlayer(ep);

        if (phase == Phase.PRE) {
            first_login = (p == null);
            send_all = false;

            if (first_login) {
                p = new LMPlayerServer(LMWorldServer.inst, LMPlayerServer.nextPlayerID(), ep.getGameProfile());
                LMWorldServer.inst.playerMap.put(p.getPlayerID(), p);
                send_all = true;
            } else if (!p.getProfile().getName().equals(ep.getCommandSenderName())) {
                p.setProfile(ep.getGameProfile());
                send_all = true;
            }

            p.setPlayer(ep);
        } else {
            p.refreshStats();

            new EventLMPlayerServer.LoggedIn(p, ep, first_login).post();
            new MessageLMPlayerLoggedIn(p, first_login, true).sendTo(send_all ? null : ep);
            for (EntityPlayerMP ep1 : ServerUtilitiesLib.getAllOnlinePlayers(ep))
                new MessageLMPlayerLoggedIn(p, first_login, false).sendTo(ep1);

            if (first_login) {
                for (ItemStack is : ServerUtilitiesConfigLogin.starting_items.items) {
                    LMInvUtils.giveItem(ep, is);
                }
            }

            // new MessageLMPlayerInfo(p.playerID).sendTo(null);

            for (IChatComponent c : ServerUtilitiesConfigLogin.motd.components) {
                ep.addChatMessage(c);
            }

            Backups.hadPlayer = true;

            p.checkNewFriends();
            new MessageAreaUpdate(p, p.getPos(), 3, 3).sendTo(ep);
            ServerBadges.sendToPlayer(ep);

            ServerUtilitiesChunkEventHandler.instance.markDirty(null);
        }
    }

    public final ILMPlayer getLMPlayer(Object player) {
        LMWorld w = LMWorld.getWorld();
        return (w == null) ? null : w.getPlayer(player);
    }

    public final String[] getPlayerNames(boolean online) {
        return LMWorldServer.inst.getAllPlayerNames(Boolean.valueOf(online));
    }

    public final void writeWorldData(ByteIOStream io, EntityPlayerMP ep) {
        LMPlayerServer p = LMWorldServer.inst.getPlayer(ep);
        io.writeInt(p.getPlayerID());
        LMWorldServer.inst.writeDataToNet(io, p, true);
    }

    public void readWorldData(ByteIOStream io) {}

    public boolean hasClientWorld() {
        return false;
    }

    public void renderWorld(float pt) {}

    public void onTooltip(ItemTooltipEvent e) {}

    public void onRightClick(PlayerInteractEvent e) {
        if (e.entityPlayer instanceof FakePlayer || e.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) return;
        else if (!ClaimedChunks.canPlayerInteract(
                e.entityPlayer,
                new ChunkCoordinates(e.x, e.y, e.z),
                e.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK))
            e.setCanceled(true);
    }
}
