package serverutils.command;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import serverutils.ServerUtilitiesConfig;
import serverutils.command.chunks.CmdChunks;
import serverutils.command.ranks.CmdRanks;
import serverutils.command.team.CmdTeam;
import serverutils.command.tp.CmdBack;
import serverutils.command.tp.CmdDelHome;
import serverutils.command.tp.CmdDelWarp;
import serverutils.command.tp.CmdHome;
import serverutils.command.tp.CmdRTP;
import serverutils.command.tp.CmdSetHome;
import serverutils.command.tp.CmdSetWarp;
import serverutils.command.tp.CmdSpawn;
import serverutils.command.tp.CmdTPA;
import serverutils.command.tp.CmdTPAccept;
import serverutils.command.tp.CmdTplast;
import serverutils.command.tp.CmdWarp;

public class ServerUtilitiesCommands {

    public static void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CmdReload());
        event.registerServerCommand(new CmdMySettings());
        event.registerServerCommand(new CmdTeam());

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
        if (ServerUtilitiesConfig.debugging.special_commands) {
            event.registerServerCommand(new CmdAddFakePlayer());
        }
    }
}
