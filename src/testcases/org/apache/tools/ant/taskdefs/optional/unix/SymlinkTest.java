/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

/*
 * Since the initial version of this file was deveolped on the clock on
 * an NSF grant I should say the following boilerplate:
 *
 * This material is based upon work supported by the National Science
 * Foundaton under Grant No. EIA-0196404. Any opinions, findings, and
 * conclusions or recommendations expressed in this material are those
 * of the author and do not necessarily reflect the views of the
 * National Science Foundation.
 */

package org.apache.tools.ant.taskdefs.optional.unix;

import org.apache.tools.ant.taskdefs.condition.Os;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;

/**
 * Test cases for the Symlink task. Link creation, link deletion, recording
 * of links in multiple directories, and restoration of links recorded are
 * all tested. A separate test for the utility method Symlink.deleteSymlink
 * is not included because action="delete" only prints a message and calls
 * Symlink.deleteSymlink, making a separate test redundant.
 *
 * @version $Revision$
 * @author <a href="mailto:gus.heck@olin.edu">Patrick G. Heck</a> 
 */

public class SymlinkTest extends BuildFileTest {

    private Project p;
    private boolean supportsSymlinks = Os.isFamily("unix");
    private boolean testfail = false;

    public SymlinkTest(String name) {
        super(name);
    }

    public void setUp() {
        if (supportsSymlinks) {
            configureProject("src/etc/testcases/taskdefs/optional/symlink.xml");
            executeTarget("setup");
        }
    }


    public void testSingle() {
        testfail = true;
        if (supportsSymlinks) {
            executeTarget("test-single");
            p = getProject();
            assertNotNull("Failed to create file", 
                          p.getProperty("test.single.file.created"));
            assertNotNull("Failed to create link",
                          p.getProperty("test.single.link.created"));
        }
        testfail = false;
    }

    public void testDelete() {
        testfail = true;
        if (supportsSymlinks) {
            executeTarget("test-delete");
            p = getProject();
            String linkDeleted = p.getProperty("test.delete.link.still.there");
            assertNotNull("Actual file deleted by symlink",
                          p.getProperty("test.delete.file.still.there"));
            if (linkDeleted != null) {
                fail(linkDeleted);
            }
        }
        testfail = false;
    }

    public void testRecord() {
        testfail = true;
        if (supportsSymlinks) {
            executeTarget("test-record");
            p = getProject();

            assertNotNull("Failed to create dir1",
                          p.getProperty("test.record.dir1.created"));

            assertNotNull("Failed to create dir2",
                          p.getProperty("test.record.dir2.created"));

            assertNotNull("Failed to create file1",
                          p.getProperty("test.record.file1.created"));

            assertNotNull("Failed to create file2",
                          p.getProperty("test.record.file2.created"));

            assertNotNull("Failed to create fileA",
                          p.getProperty("test.record.fileA.created"));

            assertNotNull("Failed to create fileB",
                          p.getProperty("test.record.fileB.created"));

            assertNotNull("Failed to create fileC",
                          p.getProperty("test.record.fileC.created"));

            assertNotNull("Failed to create link1",
                          p.getProperty("test.record.link1.created"));

            assertNotNull("Failed to create link2",
                          p.getProperty("test.record.link2.created"));

            assertNotNull("Failed to create link3",
                          p.getProperty("test.record.link3.created"));

            assertNotNull("Failed to create dirlink",
                          p.getProperty("test.record.dirlink.created"));

            assertNotNull("Couldn't record links in dir1",
                          p.getProperty("test.record.dir1.recorded"));

            assertNotNull("Couldn't record links in dir2",
                          p.getProperty("test.record.dir2.recorded"));

            String dir3rec = p.getProperty("test.record.dir3.recorded");

            if (dir3rec != null) {
                fail(dir3rec);
            }

        }
        testfail = false;
    }

    public void testRecreate() {
        testfail = true;
        if (supportsSymlinks) {
            executeTarget("test-recreate");
            p = getProject();
            String link1Rem = p.getProperty("test.recreate.link1.not.removed");
            String link2Rem = p.getProperty("test.recreate.link2.not.removed");
            String link3Rem = p.getProperty("test.recreate.link3.not.removed");
            String dirlinkRem = p.getProperty("test.recreate.dirlink.not.removed");
            if (link1Rem != null) {
                fail(link1Rem);
            }
            if (link2Rem != null) {
                fail(link2Rem);
            }
            if (link3Rem != null) {
                fail(link3Rem);
            }
            if (dirlinkRem != null) {
                fail(dirlinkRem);
            }
            assertNotNull("Failed to recreate link1",
                          p.getProperty("test.recreate.link1.recreated"));
            assertNotNull("Failed to recreate link2",
                          p.getProperty("test.recreate.link2.recreated"));
            assertNotNull("Failed to recreate link3",
                          p.getProperty("test.recreate.link3.recreated"));
            assertNotNull("Failed to recreate dirlink",
                          p.getProperty("test.recreate.dirlink.recreated"));
        }
        testfail = false;
    }

    public void tearDown() {
        if (supportsSymlinks && !testfail) {
            executeTarget("teardown");
        }
    }

}
