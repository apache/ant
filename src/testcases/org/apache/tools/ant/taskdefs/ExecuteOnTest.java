/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.GregorianCalendar;

import junit.framework.ComparisonFailure;

/**
 * @author Matt Benson
 */
public class ExecuteOnTest extends BuildFileTest {
    private static final String BUILD_PATH = "src/etc/testcases/taskdefs/exec/";
    private static final String BUILD_FILE = BUILD_PATH + "apply.xml";

    public ExecuteOnTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(BUILD_FILE);
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testNoRedirect() {
        executeTarget("no-redirect");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }

        String log = getLog();
        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = log.indexOf(x + " out");
        int yout = log.indexOf(y + " out");
        int zout = log.indexOf(z + " out");
        int xerr = log.indexOf(x + " err");
        int yerr = log.indexOf(y + " err");
        int zerr = log.indexOf(z + " err");
        assertFalse("xout < 0", xout < 0);
        assertFalse("yout < 0", yout < 0);
        assertFalse("zout < 0", zout < 0);
        assertFalse("xerr < 0", xerr < 0);
        assertFalse("yerr < 0", yerr < 0);
        assertFalse("zerr < 0", zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirect1() {
        executeTarget("redirect1");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
        } catch (IOException eyeOhEx) {
        }
        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualOut.indexOf(x + " err");
        int yerr = actualOut.indexOf(y + " err");
        int zerr = actualOut.indexOf(z + " err");
        assertFalse("xout < 0", xout < 0);
        assertFalse("yout < 0", yout < 0);
        assertFalse("zout < 0", zout < 0);
        assertFalse("xerr < 0", xerr < 0);
        assertFalse("yerr < 0", yerr < 0);
        assertFalse("zerr < 0", zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirect2() {
        executeTarget("redirect2");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " err");
        int yerr = actualErr.indexOf(y + " err");
        int zerr = actualErr.indexOf(z + " err");
        assertFalse("xout < 0", xout < 0);
        assertFalse("yout < 0", yout < 0);
        assertFalse("zout < 0", zout < 0);
        assertFalse("xerr < 0", xerr < 0);
        assertFalse("yerr < 0", yerr < 0);
        assertFalse("zerr < 0", zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirect3() {
        executeTarget("redirect3");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
        } catch (IOException eyeOhEx) {
        }
        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = getLog().indexOf(x + " err");
        int yerr = getLog().indexOf(y + " err");
        int zerr = getLog().indexOf(z + " err");
        assertFalse("xout < 0", xout < 0);
        assertFalse("yout < 0", yout < 0);
        assertFalse("zout < 0", zout < 0);
        assertFalse("xerr < 0", xerr < 0);
        assertFalse("yerr < 0", yerr < 0);
        assertFalse("zerr < 0", zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
        assertPropertyEquals("redirect.out", x + " out");
    }

    public void testRedirect4() {
        executeTarget("redirect4");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " err");
        int yerr = actualErr.indexOf(y + " err");
        int zerr = actualErr.indexOf(z + " err");
        assertFalse("xout < 0", xout < 0);
        assertFalse("yout < 0", yout < 0);
        assertFalse("zout < 0", zout < 0);
        assertFalse("xerr < 0", xerr < 0);
        assertFalse("yerr < 0", yerr < 0);
        assertFalse("zerr < 0", zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
        assertPropertyEquals("redirect.out", x + " out");
        assertPropertyEquals("redirect.err", x + " err");
    }

    public void testRedirect5() {
        testRedirect5or6("redirect5");
    }

    public void testRedirect6() {
        testRedirect5or6("redirect6");
    }

    private void testRedirect5or6(String target) {
        executeTarget(target);
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }
        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        assertPropertyEquals("redirect.out", "blah y z");
        assertPropertyEquals("redirect.err", "");
        assertEquals("unexpected content in redirect.out",
            "blah y z\nx blah z\nx y blah\n", actualOut);
        assertEquals("unexpected content in redirect.err", null, actualErr);
    }

    public void testRedirect7() {
        executeTarget("redirect7");
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }
        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        assertPropertyEquals("redirect.out", "blah y z");
        assertPropertyUnset("redirect.err");
        assertEquals("unexpected content in redirect.out",
            "x y blah\n", actualOut);
        assertEquals("unexpected content in redirect.err", null, actualErr);
    }

}
