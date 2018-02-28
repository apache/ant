import org.apache.tools.ant.BuildFileTest;

public class HelloWorldTest extends BuildFileTest {

    public HelloWorldTest(String s) {
        super(s);
    }

    public void setUp() {
        // initialize Ant
        configureProject("build.xml");
    }

    public void testWithout() {
        executeTarget("use.without");
        assertEquals("Message was logged but should not.", getLog(), "");
    }

    public void testMessage() {
        // execute target 'use.nestedText' and expect a message
        // 'attribute-text' in the log
        expectLog("use.message", "attribute-text");
    }

    public void testFail() {
        // execute target 'use.fail' and expect a BuildException
        // with text 'Fail requested.'
        expectBuildException("use.fail", "Fail requested.");
    }

    public void testNestedText() {
        expectLog("use.nestedText", "nested-text");
    }

    public void testNestedElement() {
        executeTarget("use.nestedElement");
        assertLogContaining("Nested Element 1");
        assertLogContaining("Nested Element 2");
    }
}
