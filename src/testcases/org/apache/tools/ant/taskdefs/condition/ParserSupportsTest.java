package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildFileTest;

/**
 
 */
public class ParserSupportsTest extends BuildFileTest {

    public ParserSupportsTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/conditions/parsersupports.xml");
    }

    public void testEmpty() throws Exception {
        expectBuildExceptionContaining("testEmpty",
                ParserSupports.ERROR_NO_ATTRIBUTES,
                ParserSupports.ERROR_NO_ATTRIBUTES);
    }

    public void testBoth() throws Exception {
        expectBuildExceptionContaining("testBoth",
                ParserSupports.ERROR_BOTH_ATTRIBUTES,
                ParserSupports.ERROR_BOTH_ATTRIBUTES);
    }

    public void testNamespaces() throws Exception {
        executeTarget("testNamespaces");
    }

    public void testPropertyNoValue() throws Exception {
        expectBuildExceptionContaining("testPropertyNoValue",
                ParserSupports.ERROR_NO_VALUE,
                ParserSupports.ERROR_NO_VALUE);
    }

    public void testUnknownProperty() throws Exception {
        executeTarget("testUnknownProperty");
    }
    public void NotestPropertyInvalid() throws Exception {
        executeTarget("testPropertyInvalid");
    }
    public void NotestXercesProperty() throws Exception {
        executeTarget("testXercesProperty");
    }
}
