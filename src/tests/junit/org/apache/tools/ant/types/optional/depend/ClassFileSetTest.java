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

package org.apache.tools.ant.types.optional.depend;

import java.io.File;
import java.util.Hashtable;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for the Classfileset optional type.
 *
 */
public class ClassFileSetTest {
    public static final String RESULT_FILESET = "result";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        // share the setup for testing the depend task
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/depend/depend.xml");
    }

    /**
     * Test basic classfileset
     */
    @Test
    public void testBasicSet() {
        Project p = buildRule.getProject();
        buildRule.executeTarget("testbasicset");
        FileSet resultFileSet = (FileSet)p.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(p);
        String[] scannedFiles = scanner.getIncludedFiles();
        Hashtable files = new Hashtable();
        for (int i = 0; i < scannedFiles.length; ++i) {
            files.put(scannedFiles[i], scannedFiles[i]);
        }
        assertEquals("Classfileset did not pick up expected number of "
            + "class files", 4, files.size());
        assertTrue("Result did not contain A.class",
            files.containsKey("A.class"));
        assertTrue("Result did not contain B.class",
            files.containsKey("B.class"));
        assertTrue("Result did not contain C.class",
            files.containsKey("C.class"));
        assertTrue("Result did not contain D.class",
            files.containsKey("D.class"));
    }

    /**
     * Test small classfileset
     */
    @Test
    public void testSmallSet() {
        Project p = buildRule.getProject();
        buildRule.executeTarget("testsmallset");
        FileSet resultFileSet = (FileSet)p.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(p);
        String[] scannedFiles = scanner.getIncludedFiles();
        Hashtable files = new Hashtable();
        for (int i = 0; i < scannedFiles.length; ++i) {
            files.put(scannedFiles[i], scannedFiles[i]);
        }
        assertEquals("Classfileset did not pick up expected number of "
            + "class files", 2, files.size());
        assertTrue("Result did not contain B.class",
            files.containsKey("B.class"));
        assertTrue("Result did not contain C.class",
            files.containsKey("C.class"));
    }

    /**
     * Test combo classfileset
     */
    @Test
    public void testComboSet() {
        Project p = buildRule.getProject();
        buildRule.executeTarget("testcomboset");
        FileSet resultFileSet = (FileSet)p.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(p);
        String[] scannedFiles = scanner.getIncludedFiles();
        Hashtable files = new Hashtable();
        for (int i = 0; i < scannedFiles.length; ++i) {
            files.put(scannedFiles[i], scannedFiles[i]);
        }
        assertEquals("Classfileset did not pick up expected number of "
            + "class files", 1, files.size());
        assertTrue("Result did not contain C.class",
            files.containsKey("C.class"));
    }

    /**
     * Test that you can pass a classfileset by reference to a fileset.
     */
    @Test
    public void testByReference() {
        buildRule.executeTarget("testbyreference");
    }

    /**
     * Test that classes included in a method "System.out.println(MyClass.class)" are included.
     */
    @Test
    public void testMethodParam() {
        Project p = buildRule.getProject();
        buildRule.executeTarget("testmethodparam");
        FileSet resultFileSet = (FileSet)p.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(p);
        String[] scannedFiles = scanner.getIncludedFiles();
        Hashtable files = new Hashtable();
        for (int i = 0; i < scannedFiles.length; ++i) {
            files.put(scannedFiles[i], scannedFiles[i]);
        }
        assertEquals("Classfileset did not pick up expected number of "
            + "class files", 5, files.size());
        assertTrue("Result did not contain A.class",
            files.containsKey("A.class"));
        assertTrue("Result did not contain B.class",
            files.containsKey("B.class"));
        assertTrue("Result did not contain C.class",
            files.containsKey("C.class"));
        assertTrue("Result did not contain D.class",
            files.containsKey("D.class"));
        assertTrue("Result did not contain E.class",
            files.containsKey("E.class"));
    }

    /**
     * Test that classes included in a method "System.out.println(Outer.Inner.class)" are included.
     */
    @Test
    public void testMethodParamInner() {
        Project p = buildRule.getProject();
        buildRule.executeTarget("testmethodparaminner");
        FileSet resultFileSet = (FileSet)p.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(p);
        String[] scannedFiles = scanner.getIncludedFiles();
        Hashtable files = new Hashtable();
        for (int i = 0; i < scannedFiles.length; ++i) {
            files.put(scannedFiles[i], scannedFiles[i]);
        }
        assertEquals("Classfileset did not pick up expected number of "
            + "class files", 4, files.size());
        assertTrue("Result did not contain test" + File.separator + "Outer$Inner.class",
            files.containsKey("test" + File.separator + "Outer$Inner.class"));
        assertTrue("Result did not contain test" + File.separator + "Outer.class",
            files.containsKey("test" + File.separator + "Outer.class"));
        assertTrue("Result did not contain test" + File.separator + "ContainsOnlyInner.class",
            files.containsKey("test" + File.separator + "ContainsOnlyInner.class"));
        assertTrue("Result did not contain test" + File.separator + "ContainsOnlyInner.class",
            files.containsKey("test" + File.separator + "MethodParam.class"));
    }

    @Test
    public void testResourceCollection() {
        buildRule.executeTarget("testresourcecollection");
    }

}
