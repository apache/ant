/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapterFactory;
import org.apache.tools.ant.taskdefs.rmic.DefaultRmicAdapter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Date: 04-Aug-2004
 * Time: 22:15:46
 */
public class RmicAdvancedTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/rmic/";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    /**
     * The JUnit setup method
     */
    @Before
    public void setUp() throws Exception {
        buildRule.configureProject(TASKDEFS_DIR + "rmic.xml");
    }

    /**
     * verify that "default" binds us to the default compiler
     */
    @Test
    public void testDefault() throws Exception {
        buildRule.executeTarget("testDefault");
    }

    /**
     * verify that "default" binds us to the default compiler
     */
    @Test
    public void testDefaultDest() throws Exception {
        buildRule.executeTarget("testDefaultDest");
    }

    /**
     * verify that "" binds us to the default compiler
     */
    @Test
    public void testEmpty() throws Exception {
        buildRule.executeTarget("testEmpty");
    }

    /**
     * verify that "" binds us to the default compiler
     */
    @Test
    public void testEmptyDest() throws Exception {
        buildRule.executeTarget("testEmptyDest");
    }

    /**
     * test sun's rmic compiler
     */
    @Test
    public void testRmic() throws Exception {
        buildRule.executeTarget("testRmic");
    }

    /**
     * test sun's rmic compiler
     */
    @Test
    public void testRmicDest() throws Exception {
        buildRule.executeTarget("testRmicDest");
    }

    /**
     * test sun's rmic compiler strips
     * out -J arguments when not forking
     */
    @Test
    public void testRmicJArg() throws Exception {
        buildRule.executeTarget("testRmicJArg");
    }

    /**
     * test sun's rmic compiler strips
     * out -J arguments when not forking
     */
    @Test
    public void testRmicJArgDest() throws Exception {
        buildRule.executeTarget("testRmicJArgDest");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testKaffe() throws Exception {
        buildRule.executeTarget("testKaffe");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testKaffeDest() throws Exception {
        buildRule.executeTarget("testKaffeDest");
    }

    // WLrmic tests don't work
    /**
     * test weblogic
     */
    @Test
    @Ignore("WLRmin tests don't work")
    public void XtestWlrmic() throws Exception {
        buildRule.executeTarget("testWlrmic");
    }

    /**
     *  test weblogic's stripping of -J args
     */
    @Test
    @Ignore("WLRmin tests don't work")
    public void XtestWlrmicJArg() throws Exception {
        buildRule.executeTarget("testWlrmicJArg");
    }

    /**
     * test the forking compiler
     */
    @Test
    @Ignore("WLRmin tests don't work")
    public void NotestForking() throws Exception {
        buildRule.executeTarget("testForking");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testForkingAntClasspath() throws Exception {
        buildRule.executeTarget("testForkingAntClasspath");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testForkingAntClasspathDest() throws Exception {
        buildRule.executeTarget("testForkingAntClasspathDest");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testAntClasspath() throws Exception {
        buildRule.executeTarget("testAntClasspath");
    }

    /**
     * test the forking compiler
     */
    @Test
    public void testAntClasspathDest() throws Exception {
        buildRule.executeTarget("testAntClasspathDest");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testBadName() throws Exception {
        try {
            buildRule.executeTarget("testBadName");
            fail("Compile not known");
        } catch (BuildException ex) {
            AntAssert.assertContains(RmicAdapterFactory.ERROR_UNKNOWN_COMPILER, ex.getMessage());
        }
    }

    /**
     * load an adapter by name
     */
    @Test
    public void testExplicitClass() throws Exception {
        buildRule.executeTarget("testExplicitClass");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testWrongClass() throws Exception {
        try {
            buildRule.executeTarget("testWrongClass");
            fail("Class not an RMIC adapter");
        } catch (BuildException ex) {
            AntAssert.assertContains(RmicAdapterFactory.ERROR_NOT_RMIC_ADAPTER, ex.getMessage());
        }
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testDefaultBadClass() throws Exception {
        try {
            buildRule.executeTarget("testDefaultBadClass");
            fail("expected the class to fail");
        } catch(BuildException ex) {
            AntAssert.assertContains(Rmic.ERROR_RMIC_FAILED, ex.getMessage());
        }
        //dont look for much text here as it is vendor and version dependent
        AntAssert.assertContains("unimplemented.class", buildRule.getLog());
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testMagicProperty() throws Exception {
        try {
            buildRule.executeTarget("testMagicProperty");
            fail("Magic property not working");
        } catch (BuildException ex) {
            AntAssert.assertContains(RmicAdapterFactory.ERROR_UNKNOWN_COMPILER, ex.getMessage());
        }
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testMagicPropertyOverridesEmptyString() throws Exception {
        try {
            buildRule.executeTarget("testMagicPropertyOverridesEmptyString");
            fail("Magic property not working");
        } catch (BuildException ex) {
            AntAssert.assertContains(RmicAdapterFactory.ERROR_UNKNOWN_COMPILER, ex.getMessage());
        }
    }


    @Test
    public void testMagicPropertyIsEmptyString() throws Exception {
        buildRule.executeTarget("testMagicPropertyIsEmptyString");
    }


    @Test
    @Ignore("Previously named to prevent execution")
    public void NotestFailingAdapter() throws Exception {
        try {
            buildRule.executeTarget("testFailingAdapter");
            fail("Expected failures to propogate");
        } catch (BuildException ex) {
            AntAssert.assertContains(Rmic.ERROR_RMIC_FAILED, ex.getMessage());
        }
    }


    /**
     * test that version 1.1 stubs are good
     * @throws Exception
     */
    @Test
    public void testVersion11() throws Exception {
        buildRule.executeTarget("testVersion11");
    }

    /**
     * test that version 1.1 stubs are good
     * @throws Exception
     */
    @Test
    public void testVersion11Dest() throws Exception {
        buildRule.executeTarget("testVersion11Dest");
    }

    /**
     * test that version 1.2 stubs are good
     *
     * @throws Exception
     */
    @Test
    public void testVersion12() throws Exception {
        buildRule.executeTarget("testVersion12");
    }

    /**
     * test that version 1.2 stubs are good
     *
     * @throws Exception
     */
    @Test
    public void testVersion12Dest() throws Exception {
        buildRule.executeTarget("testVersion12Dest");
    }

    /**
     * test that version compat stubs are good
     *
     * @throws Exception
     */
    @Test
    public void testVersionCompat() throws Exception {
        buildRule.executeTarget("testVersionCompat");
    }

    /**
     * test that version compat stubs are good
     *
     * @throws Exception
     */
    @Test
    public void testVersionCompatDest() throws Exception {
        buildRule.executeTarget("testVersionCompatDest");
    }

    /**
     * test that passes -Xnew to sun's rmic.
     *
     * @throws Exception
     */
    @Test
    public void testXnew() throws Exception {
        buildRule.executeTarget("testXnew");
    }

    /**
     * test that passes -Xnew to sun's rmic.
     *
     * @throws Exception
     */
    @Test
    public void testXnewDest() throws Exception {
        buildRule.executeTarget("testXnewDest");
    }

    /**
     * test that passes -Xnew to sun's rmic running in a different VM.
     *
     * @throws Exception
     */
    @Test
    public void testXnewForked() throws Exception {
        buildRule.executeTarget("testXnewForked");
    }

    /**
     * test that passes -Xnew to sun's rmic running in a different VM.
     *
     * @throws Exception
     */
    @Test
    public void testXnewForkedDest() throws Exception {
        buildRule.executeTarget("testXnewForkedDest");
    }

    /**
     * test that runs the new xnew compiler adapter.
     *
     * @throws Exception
     */
    @Test
    public void testXnewCompiler() throws Exception {
        buildRule.executeTarget("testXnewCompiler");
    }

    /**
     * test that runs the new xnew compiler adapter.
     *
     * @throws Exception
     */
    @Test
    public void testXnewCompilerDest() throws Exception {
        buildRule.executeTarget("testXnewCompilerDest");
    }

    /**
     * test that verifies that IDL compiles.
     *
     * @throws Exception
     */
    @Test
    public void testIDL() throws Exception {
        buildRule.executeTarget("testIDL");
    }

    /**
     * test that verifies that IDL compiles.
     *
     * @throws Exception
     */
    @Test
    public void testIDLDest() throws Exception {
        buildRule.executeTarget("testIDLDest");
    }

    /**
     * test that verifies that IIOP compiles.
     *
     * @throws Exception
     */
    @Test
    public void testIIOP() throws Exception {
        buildRule.executeTarget("testIIOP");
    }

    /**
     * test that verifies that IIOP compiles.
     *
     * @throws Exception
     */
    @Test
    public void testIIOPDest() throws Exception {
        buildRule.executeTarget("testIIOPDest");
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

