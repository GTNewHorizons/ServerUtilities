package serverutils.old.mod.handlers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;

import com.google.gson.JsonObject;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import latmod.lib.LMFileUtils;
import latmod.lib.LMJsonUtils;
import latmod.lib.LMStringUtils;
import latmod.lib.MathHelperLM;
import latmod.lib.util.Phase;
import serverutils.lib.LMNBTUtils;
import serverutils.old.api.EventLMWorldServer;
import serverutils.old.mod.ServerUtilities;
import serverutils.old.mod.config.ServerUtilitiesConfigGeneral;
import serverutils.old.world.LMPlayer;
import serverutils.old.world.LMPlayerServer;
import serverutils.old.world.LMWorldServer;
import serverutils.old.world.claims.ClaimedChunks;

public class ServerUtiltiesWorldEventHandler {

    @SubscribeEvent
    public void worldLoaded(net.minecraftforge.event.world.WorldEvent.Load e) {
        if (e.world instanceof WorldServer) ServerUtilitiesChunkEventHandler.instance.markDirty(e.world);

        // Move /world/latmod/LMPlayers.txt to /world/LatMod/LMPlayers.txt
        try {
            Path worldPath = e.world.getSaveHandler().getWorldDirectory().toPath();
            Path oldFile = worldPath.resolve("latmod").resolve("LMPlayers.txt");
            Path newFile = worldPath.resolve("LatMod").resolve("LMPlayers.txt");
            if (oldFile.toFile().exists() && !newFile.toFile().exists()) {
                ServerUtilities.logger.info("Attempting to move " + oldFile + " to " + newFile);
                Files.move(oldFile, newFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SubscribeEvent
    public void worldSaved(net.minecraftforge.event.world.WorldEvent.Save e) {
        if (e.world.provider.dimensionId == 0 && e.world instanceof WorldServer) {
            new EventLMWorldServer.Saved(LMWorldServer.inst).post();

            JsonObject group = new JsonObject();
            LMWorldServer.inst.save(group, Phase.PRE);

            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound players = new NBTTagCompound();
            LMWorldServer.inst.writePlayersToServer(players);
            tag.setTag("Players", players);
            tag.setInteger("LastID", LMPlayerServer.lastPlayerID);
            LMNBTUtils.writeMap(new File(LMWorldServer.inst.latmodFolder, "LMPlayers.dat"), tag);

            LMWorldServer.inst.save(group, Phase.POST);
            LMJsonUtils.toJson(new File(LMWorldServer.inst.latmodFolder, "LMWorld.json"), group);

            group = new JsonObject();
            LMWorldServer.inst.claimedChunks.save(group);
            LMJsonUtils.toJson(new File(LMWorldServer.inst.latmodFolder, "ClaimedChunks.json"), group);

            // Export player list //

            try {
                ArrayList<String> l = new ArrayList<>();
                int[] list = LMWorldServer.inst.getAllPlayerIDs();
                Arrays.sort(list);

                for (int i = 0; i < list.length; i++) {
                    LMPlayer p = LMWorldServer.inst.getPlayer(list[i]);

                    StringBuilder sb = new StringBuilder();
                    sb.append(LMStringUtils.fillString(Integer.toString(p.getPlayerID()), ' ', 6));
                    sb.append(LMStringUtils.fillString(p.getProfile().getName(), ' ', 21));
                    sb.append(p.getStringUUID());
                    l.add(sb.toString());
                }

                LMFileUtils.save(new File(LMWorldServer.inst.latmodFolder, "LMPlayers.txt"), l);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onMobSpawned(net.minecraftforge.event.entity.EntityJoinWorldEvent e) {
        if (!e.world.isRemote && !isEntityAllowed(e.entity)) {
            e.entity.setDead();
            e.setCanceled(true);
        }
    }

    private boolean isEntityAllowed(Entity e) {
        if (e instanceof EntityPlayer) return true;

        if (ServerUtilitiesConfigGeneral.isEntityBanned(e.getClass())) return false;

        if (ServerUtilitiesConfigGeneral.safe_spawn.getAsBoolean()
                && ClaimedChunks.isInSpawnD(e.dimension, e.posX, e.posZ)) {
            if (e instanceof IMob) return false;
            else if (e instanceof EntityChicken && e.riddenByEntity != null) return false;
        }

        return true;
    }

    @SubscribeEvent
    public void onExplosionStart(net.minecraftforge.event.world.ExplosionEvent.Start e) {
        if (e.world.isRemote) return;
        int dim = e.world.provider.dimensionId;
        int cx = MathHelperLM.chunk(e.explosion.explosionX);
        int cz = MathHelperLM.chunk(e.explosion.explosionZ);
        if (!LMWorldServer.inst.claimedChunks.allowExplosion(dim, cx, cz)) e.setCanceled(true);
    }
}
