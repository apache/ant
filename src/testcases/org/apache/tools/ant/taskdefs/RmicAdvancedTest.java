/*
 * Copyright  2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapterFactory;
import org.apache.tools.ant.taskdefs.rmic.DefaultRmicAdapter;

/**
 * Date: 04-Aug-2004
 * Time: 22:15:46
 */
public class RmicAdvancedTest extends BuildFileTest {

    public RmicAdvancedTest(String name) {
        super(name);
    }

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/rmic/";

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject(TASKDEFS_DIR + "rmic.xml");
    }

    /**
     * The teardown method for JUnit
     */
    public void tearDown() {
        executeTarget("teardown");
    }

    /**
     * verify that "default" binds us to the default compiler
     */
    public void testDefault() throws Exception {
        executeTarget("testDefault");
    }

    /**
     * verify that "" binds us to the default compiler
     */
    public void testEmpty() throws Exception {
        executeTarget("testEmpty");
    }
    /**
     * A unit test for JUnit
     */
    public void testRmic() throws Exception {
        executeTarget("testRmic");
    }
    /**
     * A unit test for JUnit
     */
    public void testKaffe() throws Exception {
        executeTarget("testKaffe");
    }

    /**
     * A unit test for JUnit
     */
    public void testWlrmic() throws Exception {
        executeTarget("testWlrmic");
    }

    /**
     * test the forking compiler
     */
    public void NotestForking() throws Exception {
        executeTarget("testForking");
    }

    /**
     * test the forking compiler
     */
    public void NotestForkingAntClasspath() throws Exception {
        executeTarget("testForkingAntClasspath");
    }

    /**
     * test the forking compiler
     */
    public void testAntClasspath() throws Exception {
        executeTarget("testAntClasspath");
    }

    /**
     * A unit test for JUnit
     */
    public void testBadName() throws Exception {
        expectBuildExceptionContaining("testBadName",
                "compiler not known",
                RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
    }

    /**
     * A unit test for JUnit
     */
    public void testWrongClass() throws Exception {
        expectBuildExceptionContaining("testWrongClass",
                "class not an RMIC adapter",
                RmicAdapterFactory.ERROR_NOT_RMIC_ADAPTER);
    }


    /**
     * A unit test for JUnit
     */
    public void testDefaultBadClass() throws Exception {
        expectBuildExceptionContaining("testDefaultBadClass",
                "expected the class to fail",
                Rmic.ERROR_RMIC_FAILED);
        //dont look for much text here as it is vendor and version dependent
        assertLogContaining("unimplemented.class");
    }


    /**
     * A unit test for JUnit
     */
    public void testMagicProperty() throws Exception {
        expectBuildExceptionContaining("testMagicProperty",
                "magic property not working",
                RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
    }

    /**
     * A unit test for JUnit
     */
    public void testMagicPropertyOverridesEmptyString() throws Exception {
        expectBuildExceptionContaining("testMagicPropertyOverridesEmptyString",
                "magic property not working",
                RmicAdapterFactory.ERROR_UNKNOWN_COMPILER);
    }


    /**
     * test the forking compiler
     */
    public void testMagicPropertyIsEmptyString() throws Exception {
        executeTarget("testMagicPropertyIsEmptyString");
    }


    public void NotestFailingAdapter() throws Exception {
        expectBuildExceptionContaining("testFailingAdapter",
                "expected failures to propagate",
                Rmic.ERROR_RMIC_FAILED);
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

