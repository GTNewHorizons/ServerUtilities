package serverutils.utils.command;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.utils.command.chunks.CmdChunks;
import serverutils.utils.command.ranks.CmdRanks;
import serverutils.utils.command.tp.CmdBack;
import serverutils.utils.command.tp.CmdDelHome;
import serverutils.utils.command.tp.CmdDelWarp;
import serverutils.utils.command.tp.CmdHome;
import serverutils.utils.command.tp.CmdRTP;
import serverutils.utils.command.tp.CmdSetHome;
import serverutils.utils.command.tp.CmdSetWarp;
import serverutils.utils.command.tp.CmdSpawn;
import serverutils.utils.command.tp.CmdTPA;
import serverutils.utils.command.tp.CmdTPAccept;
import serverutils.utils.command.tp.CmdTplast;
import serverutils.utils.command.tp.CmdWarp;

public class ServerUtilitiesCommands {

    public static void registerCommands(FMLServerStartingEvent event) {
        if (event.getServer().isDedicatedServer()) {
            event.registerServerCommand(new CmdShutdown());

            if (ServerUtilitiesConfig.auto_shutdown.enabled) {
                event.registerServerCommand(new CmdShutdownTime());
            }
        }

        if (ServerUtilitiesConfig.commands.inv) {
            event.registerServerCommand(new CmdInv());
        }

        if (ServerUtilitiesConfig.commands.warp) {
            event.registerServerCommand(new CmdWarp());
            event.registerServerCommand(new CmdSetWarp());
            event.registerServerCommand(new CmdDelWarp());
        }

        if (ServerUtilitiesConfig.commands.home) {
            event.registerServerCommand(new CmdHome());
            event.registerServerCommand(new CmdSetHome());
            event.registerServerCommand(new CmdDelHome());
        }

        if (ServerUtilitiesConfig.commands.tpl) {
            event.registerServerCommand(new CmdTplast());
        }

        if (ServerUtilitiesConfig.commands.trash_can) {
            event.registerServerCommand(new CmdTrashCan());
        }

        if (ServerUtilitiesConfig.commands.back) {
            event.registerServerCommand(new CmdBack());
        }

        if (ServerUtilitiesConfig.commands.spawn) {
            event.registerServerCommand(new CmdSpawn());
        }

        if (ServerUtilitiesConfig.commands.chunks) {
            event.registerServerCommand(new CmdChunks());
        }

        if (ServerUtilitiesConfig.commands.kickme) {
            event.registerServerCommand(new CmdKickme());
        }

        if (ServerUtilitiesConfig.commands.ranks) {
            event.registerServerCommand(new CmdRanks());
        }

        if (ServerUtilitiesConfig.commands.heal) {
            event.registerServerCommand(new CmdHeal());
        }

        if (ServerUtilitiesConfig.commands.killall) {
            event.registerServerCommand(new CmdKillall());
        }

        if (ServerUtilitiesConfig.commands.nbtedit) {
            event.registerServerCommand(new CmdEditNBT());
        }

        if (ServerUtilitiesConfig.commands.fly) {
            event.registerServerCommand(new CmdFly());
        }

        if (ServerUtilitiesConfig.commands.leaderboard) {
            event.registerServerCommand(new CmdLeaderboard());
        }

        if (ServerUtilitiesConfig.commands.dump_chunkloaders) {
            event.registerServerCommand(new CmdDumpChunkloaders());
        }

        if (ServerUtilitiesConfig.commands.tpa) {
            event.registerServerCommand(new CmdTPA());
            event.registerServerCommand(new CmdTPAccept());
        }

        if (ServerUtilitiesConfig.commands.nick) {
            event.registerServerCommand(new CmdNick());
            event.registerServerCommand(new CmdNickFor());
        }

        if (ServerUtilitiesConfig.commands.mute) {
            event.registerServerCommand(new CmdMute());
            event.registerServerCommand(new CmdUnmute());
        }

        if (ServerUtilitiesConfig.commands.backup) {
            event.registerServerCommand(new CmdBackup());
        }

        if (ServerUtilitiesConfig.commands.rtp) {
            event.registerServerCommand(new CmdRTP());
        }

        if (ServerUtilitiesConfig.commands.god) {
            event.registerServerCommand(new CmdGod());
        }

        if (ServerUtilitiesConfig.commands.rec) {
            event.registerServerCommand(new CmdRec());
        }
        if (ServerUtilitiesConfig.commands.dump_permissions) {
            event.registerServerCommand(new CmdDumpPermissions());
        }
    }
}
