/** (C) Copyright 2004 Hewlett-Packard Development Company, LP

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 For more information: www.smartfrog.org

 */


package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapterFactory;

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
     * A unit test for JUnit
     */
    public void testForking() throws Exception {
        executeTarget("testForking");
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
}

