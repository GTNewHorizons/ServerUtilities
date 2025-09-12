package serverutils.watchdog;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public class ServerHangWatchdog implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();
    private final DedicatedServer server;
    private final long maxTickTime;

    public ServerHangWatchdog(DedicatedServer server) {
        this.server = server;
        this.maxTickTime = ((IMaxTickTimeDedicatedServer) server).serverutilities$getMaxTickTime();
    }

    public void run() {
        while (this.server.isServerRunning()) {
            long i = ((IMaxTickTimeMinecraftServer) this.server).serverutilities$getCurrentTime();
            long j = MinecraftServer.getSystemTimeMillis();
            long k = j - i;

            if (k > this.maxTickTime) {
                LOGGER.fatal(
                        "A single server tick took {} seconds (should be max {})",
                        String.format("%.2f", (float) k / 1000.0F),
                        String.format("%.2f", 0.05F));
                LOGGER.fatal("Considering it to be crashed, server will forcibly shutdown.");
                ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
                ThreadInfo[] athreadinfo = threadmxbean.dumpAllThreads(true, true);
                StringBuilder stringbuilder = new StringBuilder();
                Error error = new Error(
                        String.format(
                                "ServerHangWatchdog detected that a single server tick took %.2f seconds (should be max 0.05)",
                                k / 1000F)); // Forge: don't just make a crash report with a seemingly-inexplicable
                                             // Error

                for (ThreadInfo threadinfo : athreadinfo) {
                    if (Objects.equals(threadinfo.getThreadName(), "Server thread")) {
                        error.setStackTrace(threadinfo.getStackTrace());
                    }

                    stringbuilder.append((Object) threadinfo);
                    stringbuilder.append("\n");
                }

                CrashReport crashreport = new CrashReport("Watching Server", error);
                this.server.addServerInfoToCrashReport(crashreport);
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Thread Dump");
                crashreportcategory.addCrashSection("Threads", stringbuilder);
                File file1 = new File(
                        new File(this.server.getDataDirectory(), "crash-reports"),
                        "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

                if (crashreport.saveToFile(file1)) {
                    LOGGER.error("This crash report has been saved to: {}", (Object) file1.getAbsolutePath());
                } else {
                    LOGGER.error("We were unable to save this crash report to disk.");
                }

                this.scheduleHalt();
            }

            try {
                Thread.sleep(i + this.maxTickTime - j);
            } catch (InterruptedException var15) {
                ;
            }
        }
    }

    private void scheduleHalt() {
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                public void run() {
                    FMLCommonHandler.instance().exitJava(1, true);
                }
            }, 10000L);
            FMLCommonHandler.instance().exitJava(1, false);
        } catch (Throwable var2) {
            FMLCommonHandler.instance().exitJava(1, true);
        }
    }
}
