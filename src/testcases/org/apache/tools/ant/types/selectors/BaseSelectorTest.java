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

package org.apache.tools.ant.types.selectors;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;

/**
 * Base test case for Selectors. Provides a shared test as well as
 * a test bed for selecting on, and a helper method for determining
 * whether selections are correct.
 *
 */
public abstract class BaseSelectorTest extends TestCase {

    private Project project;
    private TaskdefForMakingBed tbed = null;
    protected String basedirname = "src/etc/testcases/types";
    protected String beddirname = basedirname + "/selectortest";
    protected String mirrordirname = basedirname + "/selectortest2";
    protected File basedir = new File(System.getProperty("root"), basedirname);
    protected File beddir = new File(System.getProperty("root"), beddirname);
    protected File mirrordir = new File(System.getProperty("root"), mirrordirname);
    protected String[] filenames = {".","asf-logo.gif.md5","asf-logo.gif.bz2",
            "asf-logo.gif.gz","copy.filterset.filtered","zip/asf-logo.gif.zip",
            "tar/asf-logo.gif.tar","tar/asf-logo-huge.tar.gz",
            "tar/gz/asf-logo.gif.tar.gz","tar/bz2/asf-logo.gif.tar.bz2",
            "tar/bz2/asf-logo-huge.tar.bz2","tar/bz2"};
    protected File[] files = new File[filenames.length];
    protected File[] mirrorfiles = new File[filenames.length];

    public BaseSelectorTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.init();
        project.setBaseDir(basedir);
        for (int x = 0; x < files.length; x++) {
            files[x] = new File(beddir,filenames[x]);
            mirrorfiles[x] = new File(mirrordir,filenames[x]);
        }
    }

    /**
     * Override this in child classes to return a specific Selector
     */
    public abstract BaseSelector getInstance();


    /**
     * Return a preconfigured selector (with a set reference to
     * project instance).
     * @return the selector
     */
    public BaseSelector getSelector() {
        BaseSelector selector = getInstance();
        selector.setProject( getProject() );
        return selector;
    }


    public Project getProject() {
        return project;
    }

    /**
     * This is a test that all Selectors derived from BaseSelector can
     * use. It calls the setError() method and checks to ensure that a
     * BuildException is thrown as a result.
     */
    public void testRespondsToError() {
        BaseSelector s = getInstance();
        if (s == null) {
            return;
        }
        s.setError("test error");
        try {
            s.isSelected(beddir,filenames[0],files[0]);
            fail("Cannot cause BuildException when setError() is called");
        } catch (BuildException be) {
            assertEquals("test error",
                         be.getMessage());
        }
    }


    /**
     * This is a helper method that takes a selector and calls its
     * isSelected() method on each file in the testbed. It returns
     * a string of "T"s amd "F"s
     */
    public String selectionString(FileSelector selector) {
        return selectionString(beddir,files,selector);
    }

    /**
     * This is a helper method that takes a selector and calls its
     * isSelected() method on each file in the mirror testbed. This
     * variation is used for dependency checks and to get around the
     * limitations in the touch task when running JDK 1.1. It returns
     * a string of "T"s amd "F"s.
     */
    public String mirrorSelectionString(FileSelector selector) {
        return selectionString(mirrordir,mirrorfiles,selector);
    }

    /**
     * Worker method for the two convenience methods above. Applies a
     * selector on a set of files passed in and returns a string of
     * "T"s amd "F"s from applying the selector to each file.
     */
    public String selectionString(File basedir, File[] files, FileSelector selector) {
        StringBuffer buf = new StringBuffer();
        for (int x = 0; x < files.length; x++) {
            if (selector.isSelected(basedir,filenames[x],files[x])) {
                buf.append('T');
            }
            else {
                buf.append('F');
            }
        }
        return buf.toString();
    }

    /**
     * Does the selection test for a given selector and prints the
     * filenames of the differing files (selected but shouldn't,
     * not selected but should).
     * @param selector  The selector to test
     * @param expected  The expected result
     */
    public void performTests(FileSelector selector, String expected) {
        String result = selectionString(selector);
        String diff = diff(expected, result);
        String resolved = resolve(diff);
        assertEquals("Differing files: " + resolved, result, expected);
    }

    /**
     *  Checks which files are selected and shouldn't be or which
     *  are not selected but should.
     *  @param expected    String containing 'F's and 'T's
     *  @param result      String containing 'F's and 'T's
     *  @return Difference as String containing '-' (equal) and
     *          'X' (difference).
     */
    public String diff(String expected, String result) {
        int length1 = expected.length();
        int length2 = result.length();
        int min = (length1 > length2) ? length2 : length1;
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<min; i++) {
            sb.append(
                  (expected.charAt(i) == result.charAt(i))
                ? "-"
                : "X"
            );
        }
        return sb.toString();
    }


    /**
     * Resolves a diff-String (@see diff()) against the (inherited) filenames-
     * and files arrays.
     * @param filelist    Diff-String
     * @return String containing the filenames for all differing files,
     *         separated with semicolons ';'
     */
    public String resolve(String filelist) {
        StringBuffer sb = new StringBuffer();
        int min = (filenames.length > filelist.length())
                ? filelist.length()
                : filenames.length;
        for (int i=0; i<min; i++) {
            if ('X'==filelist.charAt(i)) {
                sb.append(filenames[i]);
                sb.append(";");
            }
        }
        return sb.toString();
    }


    /**
     * <p>Creates a testbed. We avoid the dreaded "test" word so that we
     * don't falsely identify this as a test to be run. The actual
     * setting up of the testbed is done in the
     * <code>src/etc/testcases/types/selectors.xml</code> build file.</p>
     *
     * <p>Note that the right way to call this is within a try block,
     * with a finally clause that calls cleanupBed(). You place tests of
     * the isSelected() method within the try block.</p>
     */
    protected void makeBed() {
        tbed = new TaskdefForMakingBed("setupfiles");
        tbed.setUp();
        tbed.makeTestbed();
    }

    /**
     * Cleans up the testbed by calling a target in the
     * <code>src/etc/testcases/types/selectors.xml</code> file.
     */
    protected void cleanupBed() {
        if (tbed != null) {
            tbed.tearDown();
            tbed = null;
        }
    }


    /**
     * <p>Creates a mirror of the testbed for use in dependency checks.</p>
     *
     * <p>Note that the right way to call this is within a try block,
     * with a finally clause that calls cleanupMirror(). You place tests of
     * the isSelected() method within the try block.</p>
     */
    protected void makeMirror() {
        tbed = new TaskdefForMakingBed("mirrorfiles");
        tbed.setUp();
        tbed.makeMirror();
    }

    /**
     * Cleans up the mirror testbed by calling a target in the
     * <code>src/etc/testcases/types/selectors.xml</code> file.
     */
    protected void cleanupMirror() {
        if (tbed != null) {
            tbed.deleteMirror();
            tbed = null;
        }
    }

    private class TaskdefForMakingBed extends BuildFileTest {

        TaskdefForMakingBed(String name) {
            super(name);
        }

        public void setUp() {
            configureProject("src/etc/testcases/types/selectors.xml");
        }

        public void tearDown() {
            executeTarget("cleanup");
        }

        public void makeTestbed() {
            executeTarget("setupfiles");
        }

        public void makeMirror() {
            executeTarget("mirrorfiles");
        }

        public void deleteMirror() {
            executeTarget("cleanup.mirrorfiles");
        }
    }



}
