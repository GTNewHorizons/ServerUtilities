package serverutils.command;

import static serverutils.ServerUtilitiesConfig.commands;
import static serverutils.ServerUtilitiesConfig.debugging;

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import serverutils.ServerUtilitiesConfig;
import serverutils.command.chunks.CmdChunks;
import serverutils.command.pausewhenempty.CmdPauseWhenEmpty;
import serverutils.command.pregen.CmdPregen;
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
        event.registerServerCommand(new CmdMySettings());
        event.registerServerCommand(new CmdTeam());

        if (commands.reload) {
            event.registerServerCommand(new CmdReload());
        }

        if (event.getServer().isDedicatedServer()) {
            event.registerServerCommand(new CmdShutdown());

            if (ServerUtilitiesConfig.auto_shutdown.enabled) {
                event.registerServerCommand(new CmdShutdownTime());
            }

            if (ServerUtilitiesConfig.general.enable_pause_when_empty_property) {
                event.registerServerCommand(new CmdPauseWhenEmpty());
            }
        }

        if (commands.inv) {
            event.registerServerCommand(new CmdInv());
        }

        if (commands.warp) {
            event.registerServerCommand(new CmdWarp());
            event.registerServerCommand(new CmdSetWarp());
            event.registerServerCommand(new CmdDelWarp());
        }

        if (commands.home) {
            event.registerServerCommand(new CmdHome());
            event.registerServerCommand(new CmdSetHome());
            event.registerServerCommand(new CmdDelHome());
        }

        if (commands.tpl) {
            event.registerServerCommand(new CmdTplast());
        }

        if (commands.trash_can) {
            event.registerServerCommand(new CmdTrashCan());
        }

        if (commands.back) {
            event.registerServerCommand(new CmdBack());
        }

        if (commands.spawn) {
            event.registerServerCommand(new CmdSpawn());
        }

        if (commands.chunks) {
            event.registerServerCommand(new CmdChunks());
        }

        if (commands.kickme) {
            event.registerServerCommand(new CmdKickme());
        }

        if (commands.ranks) {
            event.registerServerCommand(new CmdRanks());
        }

        if (commands.heal) {
            event.registerServerCommand(new CmdHeal());
        }

        if (commands.killall) {
            event.registerServerCommand(new CmdKillall());
        }

        if (commands.nbtedit) {
            event.registerServerCommand(new CmdEditNBT());
        }

        if (commands.fly) {
            event.registerServerCommand(new CmdFly());
        }

        if (commands.leaderboard) {
            event.registerServerCommand(new CmdLeaderboard());
        }

        if (commands.dump_chunkloaders) {
            event.registerServerCommand(new CmdDumpChunkloaders());
        }

        if (commands.tpa) {
            event.registerServerCommand(new CmdTPA());
            event.registerServerCommand(new CmdTPAccept());
        }

        if (commands.nick) {
            event.registerServerCommand(new CmdNick());
            event.registerServerCommand(new CmdNickFor());
        }

        if (commands.mute) {
            event.registerServerCommand(new CmdMute());
            event.registerServerCommand(new CmdUnmute());
        }

        if (commands.backup) {
            event.registerServerCommand(new CmdBackup());
        }

        if (commands.rtp) {
            event.registerServerCommand(new CmdRTP());
        }

        if (commands.god) {
            event.registerServerCommand(new CmdGod());
        }

        if (commands.rec) {
            event.registerServerCommand(new CmdRec());
        }

        if (commands.dump_permissions) {
            event.registerServerCommand(new CmdDumpPermissions());
        }

        if (debugging.special_commands) {
            event.registerServerCommand(new CmdAddFakePlayer());
        }

        if (commands.dump_stats) {
            event.registerServerCommand(new CmdDumpStats());
        }

        if (commands.vanish) {
            event.registerServerCommand(new CmdVanish());
        }

        if (commands.seek_block) {
            event.registerServerCommand(new CmdSeekBlock());
        }

        if (ServerUtilitiesConfig.commands.pregen) {
            event.registerServerCommand(new CmdPregen());
        }
    }
}
