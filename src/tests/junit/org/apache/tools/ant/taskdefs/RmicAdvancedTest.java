/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.rmic.DefaultRmicAdapter;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapterFactory;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * Date: 04-Aug-2004
 * Time: 22:15:46
 */
public class RmicAdvancedTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * The JUnit setup method
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/rmic/rmic.xml");
    }

    /**
     * verify that "default" binds us to the default compiler
     */
    @Test
    public void testDefault() {
        buildRule.executeTarget("testDefault");
    }

    /**
     * verify that "default" binds us to the default compiler
     */
    @Test
    public void testDefaultDest() {
        buildRule.executeTarget("testDefaultDest");
    }

    /**
     * verify that "" binds us to the default compiler
     */
    @Test
    public void testEmpty() {
        buildRule.executeTarget("testEmpty");
    }

    /**
     * verify that "" binds us to the default compiler
     */
    @Test
    public void testEmptyDest() {
        buildRule.executeTarget("testEmptyDest");
    }

    /**
     * test sun's rmic compiler
     */
    @Test
    public void testRmic() {
        buildRule.executeTarget("testRmic");
    }

    /**
     * test sun's rmic compiler
     */
    @Test
    public void testRmicDest() {
        buildRule.executeTarget("testRmicDest");
    }

    /**
     * test sun's rmic compiler strips
     * out -J arguments when not forking
     */
    @Test
    public void testRmicJArg() {
        buildRule.executeTarget("testRmicJArg");
    }

    /**
     * test sun's rmic compiler strips
     * out -J arguments when not forking
     */
    @Test
    public void testRmicJArgDest() {
        buildRule.executeTarget("testRmicJArgDest");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testKaffe() {
        buildRule.executeTarget("testKaffe");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testKaffeDest() {
        buildRule.executeTarget("testKaffeDest");
    }

    // WLrmic tests don't work
    /**
     * test weblogic
     */
    @Test
    @Ignore("WLRmic tests don't work")
    public void XtestWlrmic() {
        buildRule.executeTarget("testWlrmic");
    }

    /**
     *  test weblogic's stripping of -J args
     */
    @Test
    @Ignore("WLRmic tests don't work")
    public void XtestWlrmicJArg() {
        buildRule.executeTarget("testWlrmicJArg");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testForking() {
        buildRule.executeTarget("testForking");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testForkingAntClasspath() {
        buildRule.executeTarget("testForkingAntClasspath");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testForkingAntClasspathDest() {
        buildRule.executeTarget("testForkingAntClasspathDest");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testAntClasspath() {
        buildRule.executeTarget("testAntClasspath");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testAntClasspathDest() {
        buildRule.executeTarget("testAntClasspathDest");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testBadName() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
        buildRule.executeTarget("testBadName");
    }

    /**
     * load an adapter by name
     */
    @Test
    public void testExplicitClass() {
        buildRule.executeTarget("testExplicitClass");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testWrongClass() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(RmicAdapterFactory.ERROR_NOT_RMIC_ADAPTER);
        buildRule.executeTarget("testWrongClass");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testDefaultBadClass() {
        assumeFalse("Current system is Java 15 or newer",
                    JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage(Rmic.ERROR_RMIC_FAILED);
        try {
            buildRule.executeTarget("testDefaultBadClass");
        } finally {
            // don't look for much text here as it is vendor and version dependent
            assertThat(buildRule.getLog(), containsString("unimplemented.class"));
        }
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testMagicProperty() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
        buildRule.executeTarget("testMagicProperty");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testMagicPropertyOverridesEmptyString() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
        buildRule.executeTarget("testMagicPropertyOverridesEmptyString");
    }

    @Test
    public void testMagicPropertyIsEmptyString() {
        buildRule.executeTarget("testMagicPropertyIsEmptyString");
    }

    @Test
    @Ignore("Previously named to prevent execution")
    public void NotestFailingAdapter() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(Rmic.ERROR_RMIC_FAILED);
        buildRule.executeTarget("testFailingAdapter");
    }

    /**
     * test that version 1.1 stubs are good
     */
    @Test
    public void testVersion11() {
        buildRule.executeTarget("testVersion11");
    }

    /**
     * test that version 1.1 stubs are good
     */
    @Test
    public void testVersion11Dest() {
        buildRule.executeTarget("testVersion11Dest");
    }

    /**
     * test that version 1.2 stubs are good
     */
    @Test
    public void testVersion12() {
        buildRule.executeTarget("testVersion12");
    }

    /**
     * test that version 1.2 stubs are good
     */
    @Test
    public void testVersion12Dest() {
        buildRule.executeTarget("testVersion12Dest");
    }

    /**
     * test that version compat stubs are good
     */
    @Test
    public void testVersionCompat() {
        buildRule.executeTarget("testVersionCompat");
    }

    /**
     * test that version compat stubs are good
     */
    @Test
    public void testVersionCompatDest() {
        buildRule.executeTarget("testVersionCompatDest");
    }

    /**
     * test that passes -Xnew to sun's rmic running in a different VM.
     */
    @Test
    public void testXnewForked() {
        assumeFalse("Current system is Java 9 or newer",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        buildRule.executeTarget("testXnewForked");
    }

    /**
     * test that passes -Xnew to sun's rmic running in a different VM.
     */
    @Test
    public void testXnewForkedJava9plus() {
        assumeTrue("Current system is Java 8 or older",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        assumeFalse("Current system is Java 15 or newer",
                    JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("JDK9 has removed support for -Xnew");
        buildRule.executeTarget("testXnewForked");
    }

    /**
     * test that passes -Xnew to sun's rmic running in a different VM.
     */
    @Test
    public void testXnewForkedDest() {
        assumeFalse("Current system is Java 9 or newer",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        buildRule.executeTarget("testXnewForkedDest");
    }

    /**
     * test that passes -Xnew to sun's rmic running in a different VM.
     */
    @Test
    public void testXnewForkedDestJava9plus() {
        assumeTrue("Current system is Java 8 or older",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        assumeFalse("Current system is Java 15 or newer",
                    JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("JDK9 has removed support for -Xnew");
        buildRule.executeTarget("testXnewForkedDest");
    }

    /**
     * test that runs the new xnew compiler adapter.
     */
    @Test
    public void testXnewCompiler() {
        assumeFalse("Current system is Java 9 or newer",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        buildRule.executeTarget("testXnewCompiler");
    }

    /**
     * test that runs the new xnew compiler adapter.
     */
    @Test
    public void testXnewCompilerJava9plus() {
        assumeTrue("Current system is Java 8 or older",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        assumeFalse("Current system is Java 15 or newer",
                JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("JDK9 has removed support for -Xnew");
        buildRule.executeTarget("testXnewCompiler");
    }

    /**
     * test that runs the new xnew compiler adapter.
     */
    @Test
    public void testXnewCompilerDest() {
        assumeFalse("Current system is Java 9 or newer",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        buildRule.executeTarget("testXnewCompilerDest");
    }

    /**
     * test that runs the new xnew compiler adapter.
     */
    @Test
    public void testXnewCompilerDestJava9plus() {
        assumeTrue("Current system is Java 8 or older",
                JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        assumeFalse("Current system is Java 15 or newer",
                JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("JDK9 has removed support for -Xnew");
        buildRule.executeTarget("testXnewCompilerDest");
    }

    /**
     * test that verifies that IDL compiles.
     */
    @Test
    public void testIDL() {
        assumeFalse("Current system is Java 11 or newer", JavaEnvUtils.isAtLeastJavaVersion("11"));
        buildRule.executeTarget("testIDL");
    }

    /**
     * test that verifies that IDL compiles.
     */
    @Test
    public void testIDLJava11plus() {
        assumeTrue("Current system is Java 10 or older", JavaEnvUtils.isAtLeastJavaVersion("11"));
        assumeFalse("Current system is Java 15 or newer",
                JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("this rmic implementation doesn't support the -idl switch");
        buildRule.executeTarget("testIDL");
    }

    /**
     * test that verifies that IDL compiles.
     */
    @Test
    public void testIDLDest() {
        assumeFalse("Current system is Java 11 or newer", JavaEnvUtils.isAtLeastJavaVersion("11"));
        buildRule.executeTarget("testIDLDest");
    }

    /**
     * test that verifies that IDL compiles.
     */
    @Test
    public void testIDLDestJava11plus() {
        assumeTrue("Current system is Java 10 or older", JavaEnvUtils.isAtLeastJavaVersion("11"));
        assumeFalse("Current system is Java 15 or newer",
                JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("this rmic implementation doesn't support the -idl switch");
        buildRule.executeTarget("testIDL");
   }

    /**
     * test that verifies that IIOP compiles.
     */
    @Test
    public void testIIOP() {
        assumeFalse("Current system is Java 11 or newer", JavaEnvUtils.isAtLeastJavaVersion("11"));
        buildRule.executeTarget("testIIOP");
    }

    /**
     * test that verifies that IIOP compiles.
     */
    @Test
    public void testIIOPJava11plus() {
        assumeTrue("Current system is Java 10 or older", JavaEnvUtils.isAtLeastJavaVersion("11"));
        assumeFalse("Current system is Java 15 or newer",
                JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("this rmic implementation doesn't support the -iiop switch");
        buildRule.executeTarget("testIIOP");
    }

    /**
     * test that verifies that IIOP compiles.
     */
    @Test
    public void testIIOPDest() {
        assumeFalse("Current system is Java 11 or newer", JavaEnvUtils.isAtLeastJavaVersion("11"));
        buildRule.executeTarget("testIIOPDest");
    }

    /**
     * test that verifies that IIOP compiles.
     */
    @Test
    public void testIIOPDestJava11plus() {
        assumeTrue("Current system is Java 10 or older", JavaEnvUtils.isAtLeastJavaVersion("11"));
        assumeFalse("Current system is Java 15 or newer",
                JavaEnvUtils.isAtLeastJavaVersion("15"));
        thrown.expect(BuildException.class);
        thrown.expectMessage("this rmic implementation doesn't support the -iiop switch");
        buildRule.executeTarget("testIIOP");
    }

    /**
     * this little bunny verifies that we can load stuff, and that
     * a failure to execute is turned into a fault
     */
    public static class FailingRmicAdapter extends DefaultRmicAdapter {
        public static final String LOG_MESSAGE = "hello from FailingRmicAdapter";

        /**
         * Executes the task.
         *
         * @return false -always
         */
        public boolean execute() throws BuildException {
            getRmic().log(LOG_MESSAGE);
            return false;
        }
    }
}

