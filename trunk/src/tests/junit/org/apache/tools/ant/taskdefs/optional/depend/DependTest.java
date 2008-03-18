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

package org.apache.tools.ant.taskdefs.optional.depend;

import java.util.Hashtable;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * Testcase for the Depend optional task.
 *
 */
public class DependTest extends BuildFileTest {
    public static final String RESULT_FILESET = "result";

    public static final String TEST_BUILD_FILE
        = "src/etc/testcases/taskdefs/optional/depend/depend.xml";

    public DependTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(TEST_BUILD_FILE);
    }

    public void tearDown() {
        executeTarget("clean");
    }

    /**
     * Test direct dependency removal
     */
    public void testDirect() {
        executeTarget("testdirect");
        Hashtable files = getResultFiles();
        assertEquals("Depend did not leave correct number of files", 3,
            files.size());
        assertTrue("Result did not contain A.class",
            files.containsKey("A.class"));
        assertTrue("Result did not contain D.class",
            files.containsKey("D.class"));
    }

    /**
     * Test dependency traversal (closure)
     */
    public void testClosure() {
        executeTarget("testclosure");
        Hashtable files = getResultFiles();
        assertTrue("Depend did not leave correct number of files", 
            files.size() <= 2);
        assertTrue("Result did not contain D.class",
            files.containsKey("D.class"));
    }

    /**
     * Test that inner class dependencies trigger deletion of the outer class
     */
    public void testInner() {
        executeTarget("testinner");
        assertEquals("Depend did not leave correct number of files", 0,
            getResultFiles().size());
    }

    /**
     * Test that multi-leve inner class dependencies trigger deletion of
     * the outer class
     */
    public void testInnerInner() {
        executeTarget("testinnerinner");
        assertEquals("Depend did not leave correct number of files", 0,
            getResultFiles().size());
    }

    /**
     * Test that an exception is thrown when there is no source
     */
    public void testNoSource() {
        expectBuildExceptionContaining("testnosource",
            "No source specified", "srcdir attribute must be set");
    }

    /**
     * Test that an exception is thrown when the source attribute is empty
     */
    public void testEmptySource() {
        expectBuildExceptionContaining("testemptysource",
            "No source specified", "srcdir attribute must be non-empty");
    }

    /**
     * Read the result fileset into a Hashtable
     *
     * @return a Hashtable containing the names of the files in the result
     * fileset
     */
    private Hashtable getResultFiles() {
        FileSet resultFileSet = (FileSet) project.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(project);
        String[] scannedFiles = scanner.getIncludedFiles();
        Hashtable files = new Hashtable();
        for (int i = 0; i < scannedFiles.length; ++i) {
            files.put(scannedFiles[i], scannedFiles[i]);
        }
        return files;
    }


    /**
     * Test mutual dependency between inner and outer do not cause both to be
     * deleted
     */
    public void testInnerClosure() {
        executeTarget("testinnerclosure");
        assertEquals("Depend did not leave correct number of files", 4,
            getResultFiles().size());
    }

    /**
     * Test the operation of the cache
     */
    public void testCache() {
        executeTarget("testcache");
    }

    /**
     * Test the detection and warning of non public classes
     */
    public void testNonPublic() {
        executeTarget("testnonpublic");
        String log = getLog();
        assertTrue("Expected warning about APrivate",
            log.indexOf("The class APrivate in file") != -1);
        assertTrue("but has not been deleted because its source file "
            + "could not be determined",
            log.indexOf("The class APrivate in file") != -1);
    }

}
