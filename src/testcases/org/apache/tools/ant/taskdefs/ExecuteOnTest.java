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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Unit test for the &lt;apply&gt; task.
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
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirect1() throws IOException {
        executeTarget("redirect1");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirect.out");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualOut.indexOf(x + " err");
        int yerr = actualOut.indexOf(y + " err");
        int zerr = actualOut.indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirect2() throws IOException {
        executeTarget("redirect2");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirect.out");
        String actualErr = getFileString("redirect.err");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " err");
        int yerr = actualErr.indexOf(y + " err");
        int zerr = actualErr.indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirect3() throws IOException {
        executeTarget("redirect3");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirect.out");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = getLog().indexOf(x + " err");
        int yerr = getLog().indexOf(y + " err");
        int zerr = getLog().indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);

        String outProperty = getProject().getProperty("redirect.out");
        int pxout = outProperty.indexOf(x + " out");
        int pyout = outProperty.indexOf(y + " out");
        int pzout = outProperty.indexOf(z + " out");
        assertFalse("pxout=" + pxout, pxout < 0);
        assertFalse("pyout=" + pyout, pyout < 0);
        assertFalse("pzout=" + pzout, pzout < 0);
        assertFalse("pyout < pxout", pyout < pxout);
        assertFalse("pzout < pyout", pzout < pyout);
    }

    public void testRedirect4() throws IOException {
        executeTarget("redirect4");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirect.out");
        String actualErr = getFileString("redirect.err");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " err");
        int yerr = actualErr.indexOf(y + " err");
        int zerr = actualErr.indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);

        String outProperty = getProject().getProperty("redirect.out");
        int pxout = outProperty.indexOf(x + " out");
        int pyout = outProperty.indexOf(y + " out");
        int pzout = outProperty.indexOf(z + " out");
        assertFalse("pxout=" + pxout, pxout < 0);
        assertFalse("pyout=" + pyout, pyout < 0);
        assertFalse("pzout=" + pzout, pzout < 0);
        assertFalse("pyout < pxout", pyout < pxout);
        assertFalse("pzout < pyout", pzout < pyout);

        String errorProperty = getProject().getProperty("redirect.err");
        int pxerr = errorProperty.indexOf(x + " err");
        int pyerr = errorProperty.indexOf(y + " err");
        int pzerr = errorProperty.indexOf(z + " err");
        assertFalse("pxerr=" + pxerr, pxerr < 0);
        assertFalse("pyerr=" + pyerr, pyerr < 0);
        assertFalse("pzerr=" + pzerr, pzerr < 0);
        assertFalse("pyerr < pxerr", pyerr < pxerr);
        assertFalse("pzerr < pyerr", pzerr < pyerr);
    }

    public void testRedirect5() throws IOException {
        testRedirect5or6("redirect5");
    }

    public void testRedirect6() throws IOException {
        testRedirect5or6("redirect6");
    }

    private void testRedirect5or6(String target) throws IOException {
        executeTarget(target);
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }

        assertPropertyEquals("redirect.out", getProject().replaceProperties(
            "blah y z${line.separator}x blah z${line.separator}x y blah"));
        assertPropertyEquals("redirect.err", "");
        assertEquals("unexpected output",
            "blah y z\nx blah z\nx y blah\n", getFileString("redirect.out"));
        assertNull("unexpected error output", getFileString("redirect.err"));
    }

    public void testRedirect7() throws IOException {
        executeTarget("redirect7");
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }

        assertPropertyEquals("redirect.out", "blah y z");
        assertPropertyUnset("redirect.err");
        assertEquals("unexpected output",
            "x y blah\n", getFileString("redirect.out"));
        assertNull("unexpected error output", getFileString("redirect.err"));
    }

    public void testRedirector1() {
        executeTarget("init");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        expectBuildException("redirector1", "cannot have > 1 nested <redirector>s");
    }

    public void testRedirector2() throws IOException {
        executeTarget("redirector2");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }

        String actualOut = getFileString("redirector.out");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualOut.indexOf(x + " err");
        int yerr = actualOut.indexOf(y + " err");
        int zerr = actualOut.indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirector3() throws IOException {
        executeTarget("redirector3");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }

        String actualOut = getFileString("redirector.out");
        String actualErr = getFileString("redirector.err");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " err");
        int yerr = actualErr.indexOf(y + " err");
        int zerr = actualErr.indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirector4() throws IOException {
        executeTarget("redirector4");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirector.out");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = getLog().indexOf(x + " err");
        int yerr = getLog().indexOf(y + " err");
        int zerr = getLog().indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);

        String outProperty = getProject().getProperty("redirector.out");
        int pxout = outProperty.indexOf(x + " out");
        int pyout = outProperty.indexOf(y + " out");
        int pzout = outProperty.indexOf(z + " out");
        assertFalse("pxout=" + pxout, pxout < 0);
        assertFalse("pyout=" + pyout, pyout < 0);
        assertFalse("pzout=" + pzout, pzout < 0);
        assertFalse("pyout < pxout", pyout < pxout);
        assertFalse("pzout < pyout", pzout < pyout);
    }

    public void testRedirector5() throws IOException {
        testRedirector5or6("redirector5");
    }

    public void testRedirector6() throws IOException {
        testRedirector5or6("redirector6");
    }

    private void testRedirector5or6(String target) throws IOException {
        executeTarget(target);
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirector.out");
        String actualErr = getFileString("redirector.err");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " err");
        int yerr = actualErr.indexOf(y + " err");
        int zerr = actualErr.indexOf(z + " err");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);

        String outProperty = getProject().getProperty("redirector.out");
        int pxout = outProperty.indexOf(x + " out");
        int pyout = outProperty.indexOf(y + " out");
        int pzout = outProperty.indexOf(z + " out");
        assertFalse("pxout=" + pxout, pxout < 0);
        assertFalse("pyout=" + pyout, pyout < 0);
        assertFalse("pzout=" + pzout, pzout < 0);
        assertFalse("pyout < pxout", pyout < pxout);
        assertFalse("pzout < pyout", pzout < pyout);

        String errorProperty = getProject().getProperty("redirector.err");
        int pxerr = errorProperty.indexOf(x + " err");
        int pyerr = errorProperty.indexOf(y + " err");
        int pzerr = errorProperty.indexOf(z + " err");
        assertFalse("pxerr=" + pxerr, pxerr < 0);
        assertFalse("pyerr=" + pyerr, pyerr < 0);
        assertFalse("pzerr=" + pzerr, pzerr < 0);
        assertFalse("pyerr < pxerr", pyerr < pxerr);
        assertFalse("pzerr < pyerr", pzerr < pyerr);
    }

    public void testRedirector7() throws IOException {
        executeTarget("redirector7");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String actualOut = getFileString("redirector.out");
        String actualErr = getFileString("redirector.err");

        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = actualOut.indexOf(x + " out");
        int yout = actualOut.indexOf(y + " out");
        int zout = actualOut.indexOf(z + " out");
        int xerr = actualErr.indexOf(x + " ERROR!!!");
        int yerr = actualErr.indexOf(y + " ERROR!!!");
        int zerr = actualErr.indexOf(z + " ERROR!!!");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);

        String outProperty = getProject().getProperty("redirector.out");
        int pxout = outProperty.indexOf(x + " out");
        int pyout = outProperty.indexOf(y + " out");
        int pzout = outProperty.indexOf(z + " out");
        assertFalse("pxout=" + pxout, pxout < 0);
        assertFalse("pyout=" + pyout, pyout < 0);
        assertFalse("pzout=" + pzout, pzout < 0);
        assertFalse("pyout < pxout", pyout < pxout);
        assertFalse("pzout < pyout", pzout < pyout);

        String errorProperty = getProject().getProperty("redirector.err");
        int pxerr = errorProperty.indexOf(x + " ERROR!!!");
        int pyerr = errorProperty.indexOf(y + " ERROR!!!");
        int pzerr = errorProperty.indexOf(z + " ERROR!!!");
        assertFalse("pxerr=" + pxerr, pxerr < 0);
        assertFalse("pyerr=" + pyerr, pyerr < 0);
        assertFalse("pzerr=" + pzerr, pzerr < 0);
        assertFalse("pyerr < pxerr", pyerr < pxerr);
        assertFalse("pzerr < pyerr", pzerr < pyerr);
    }

    public void testRedirector8() throws IOException {
        executeTarget("redirector8");
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }

        assertPropertyEquals("redirector.out", getProject().replaceProperties(
            "blah y z${line.separator}x blah z${line.separator}x y blah"));
        assertPropertyEquals("redirector.err", "");
        assertEquals("unexpected output",
            "blah y z\nx blah z\nx y blah\n", getFileString("redirector.out"));
        assertNull("unexpected error output", getFileString("redirector.err"));
    }

    public void testRedirector9() throws IOException {
        testRedirector9Thru12("redirector9");
    }

    public void testRedirector10() throws IOException {
        testRedirector9Thru12("redirector10");
    }

    public void testRedirector11() throws IOException {
        testRedirector9Thru12("redirector11");
    }

    public void testRedirector12() throws IOException {
        testRedirector9Thru12("redirector12");
    }

    private void testRedirector9Thru12(String target) throws IOException {
        executeTarget(target);
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }

        assertNull("unexpected error output", getFileString("redirector.err"));
        assertPropertyEquals("redirector.out", getProject().replaceProperties(
            "blah after y after z${line.separator}x after blah after z"
            + "${line.separator}x after y after blah"));
        assertPropertyEquals("redirector.err", "");
        assertEquals("unexpected output",
            "blah after y after z\nx after blah after z"
            + "\nx after y after blah\n", getFileString("redirector.out"));
    }

    public void testRedirector13() {
        executeTarget("redirector13");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String log = getLog();
        File x = getProject().resolveFile("x");
        File y = getProject().resolveFile("y");
        File z = getProject().resolveFile("z");
        int xout = log.indexOf(x + " OUTPUT???");
        int yout = log.indexOf(y + " OUTPUT???");
        int zout = log.indexOf(z + " OUTPUT???");
        int xerr = log.indexOf(x + " ERROR!!!");
        int yerr = log.indexOf(y + " ERROR!!!");
        int zerr = log.indexOf(z + " ERROR!!!");
        assertFalse("xout=" + xout, xout < 0);
        assertFalse("yout=" + yout, yout < 0);
        assertFalse("zout=" + zout, zout < 0);
        assertFalse("xerr=" + xerr, xerr < 0);
        assertFalse("yerr=" + yerr, yerr < 0);
        assertFalse("zerr=" + zerr, zerr < 0);
        assertFalse("yout < xout", yout < xout);
        assertFalse("zout < yout", zout < yout);
        assertFalse("yerr < xerr", yerr < xerr);
        assertFalse("zerr < yerr", zerr < yerr);
    }

    public void testRedirector14() throws IOException {
        executeTarget("redirector14");
        if (getProject().getProperty("sed.can.run") == null) {
            return;
        }

        assertEquals("unexpected log content",
            "z after y after blahx after y after blah", getLog());

        assertEquals("unexpected redirector.out content",
            "x after blah after z\n", getFileString("redirector.out"));

        assertNull("unexpected redirector.err content", getFileString("redirector.err"));
    }

    public void testIgnoreMissing() {
        executeTarget("ignoremissing");
    }

    public void testForce() {
        executeTarget("force");
    }

    //borrowed from TokenFilterTest
    private String getFileString(String filename) throws IOException {
        String result = null;
        FileReader reader = null;
        try {
            reader = new FileReader(getProject().resolveFile(filename));
            result = FileUtils.readFully(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignore) {
                }
            }
        }
        return result;
    }

}
