package serverutils.lib.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;

import serverutils.lib.io.DataReader;

public class NetUtils {

    public static String getHostAddress() {
        try {
            return StringUtils.emptyIfNull(InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            return "";
        }
    }

    public static String getPublicAddress(Proxy proxy) {
        try {
            return DataReader.get(new URL("https://api.ipify.org"), DataReader.TEXT, proxy).safeString();
        } catch (Exception e) {
            return "";
        }
    }

    // Misc //

    public static boolean openURI(URI uri) throws Exception {
        Class<?> oclass = Class.forName("java.awt.Desktop");
        Object object = oclass.getMethod("getDesktop").invoke(null);
        oclass.getMethod("browse", URI.class).invoke(object, uri);
        return true;
    }

    public static void moveBytes(InputStream is, OutputStream os, boolean close) throws Exception {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer, 0, buffer.length)) > 0) {
            os.write(buffer, 0, len);
        }
        os.flush();

        if (close) {
            is.close();
            os.close();
        }
    }
}
