package serverutils.lib.util;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void getRelativePath_DropsDotSegments() {
        assertEquals("World/level.dat", FileUtils.getRelativePath(new File(new File(".", "World"), "level.dat")));
        assertEquals("World/level.dat", FileUtils.getRelativePath(new File("./World/./level.dat")));
        assertEquals("World/level.dat", FileUtils.getRelativePath(new File("World/level.dat").getAbsoluteFile()));
    }

    @Test
    public void getRelativePath_KeepsParentSegmentsOutsideRunFolder() {
        assertEquals("../backups/a.zip", FileUtils.getRelativePath(new File("../backups/a.zip")));
        assertEquals("../backups/a.zip", FileUtils.getRelativePath(new File("./World/../../backups/a.zip")));
    }
}
