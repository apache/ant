import org.apache.tools.ant.BuildFileTest;

public class FindTest extends BuildFileTest {

    public FindTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("build.xml");
    }

    public void testSimple() {
        expectLog("use.simple", "test-value");
    }
}
