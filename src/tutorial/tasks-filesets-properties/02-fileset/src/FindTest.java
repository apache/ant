import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.taskdefs.Property;
import java.io.File;

public class FindTest extends BuildFileTest {

    public FindTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("build.xml");
    }

    public void testMissingFile() {
        Find find = new Find();
        try {
            find.execute();
            fail("No 'no-file'-exception thrown.");
        } catch (Exception e) {
            // exception expected
            String expected = "file not set";
            assertEquals("Wrong exception message.", expected, e.getMessage());
        }
    }

    public void testMissingLocation() {
        Find find = new Find();
        find.setFile("ant.jar");
        try {
            find.execute();
            fail("No 'no-location'-exception thrown.");
        } catch (Exception e) {
            // exception expected
            String expected = "location not set";
            assertEquals("Wrong exception message.", expected, e.getMessage());
        }
    }

    public void testMissingFileset() {
        Find find = new Find();
        find.setFile("ant.jar");
        find.setLocation("location.ant-jar");
        try {
            find.execute();
            fail("No 'no-fileset'-exception thrown.");
        } catch (Exception e) {
            // exception expected
            String expected = "fileset not set";
            assertEquals("Wrong exception message.", expected, e.getMessage());
        }
    }

    public void testFileNotPresent() {
        executeTarget("testFileNotPresent");
        String result = getProject().getProperty("location.ant-jar");
        assertNull("Property set to wrong value.", result);
    }

    public void testFilePresent() {
        executeTarget("testFilePresent");
        String result = getProject().getProperty("location.ant-jar");
        assertNotNull("Property not set.", result);
        assertTrue("Wrong file found.", result.endsWith("ant.jar"));
    }

}
