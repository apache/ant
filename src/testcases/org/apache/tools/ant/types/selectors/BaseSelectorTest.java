/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.types.selectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.File;

/**
 * Base test case for Selectors. Provides a shared test as well as
 * a test bed for selecting on, and a helper method for determining
 * whether selections are correct.
 *
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 */
public abstract class BaseSelectorTest extends TestCase {

    private Project project;
    private TaskdefForMakingBed tbed = null;
    protected String basedirname = "src/etc/testcases/types/selectortest";
    protected File basedir = new File(basedirname);
    protected String[] filenames = {".","asf-logo.gif.md5","asf-logo.gif.bz2",
            "asf-logo.gif.gz","copy.filterset.filtered","zip/asf-logo.gif.zip",
            "tar/asf-logo.gif.tar","tar/asf-logo-huge.tar",
            "tar/gz/asf-logo.gif.tar.gz","tar/bz2/asf-logo.gif.tar.bz2",
            "tar/bz2/asf-logo-huge.tar.bz2","tar/bz2"};
    protected File[] files = new File[filenames.length];

    public BaseSelectorTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");
        for (int x = 0; x < files.length; x++) {
            files[x] = new File(basedir,filenames[x]);
        }
    }

    /**
     * Override this in child classes to return a specific Selector
     */
    public abstract BaseSelector getInstance();


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
            s.isSelected(basedir,filenames[0],files[0]);
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
    }



}
