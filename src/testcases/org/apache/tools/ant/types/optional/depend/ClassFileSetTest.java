/* 
 * Copyright  2001-2004 Apache Software Foundation
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

package org.apache.tools.ant.types.optional.depend;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Testcase for the Classfileset optional type. 
 * 
 * @author Conor MacNeill
 */
public class ClassFileSetTest extends BuildFileTest {
    public static final String RESULT_FILESET = "result";
    
    public ClassFileSetTest(String name) {
        super(name);
    }

    public void setUp() {
        // share the setup for testing the depend task
        configureProject("src/etc/testcases/taskdefs/optional/depend/depend.xml");
    }

    public void tearDown() {
        executeTarget("clean");
    }

    /**
     * Test basic clasfileset
     */
    public void testBasicSet() {
        Project project = getProject();
        executeTarget("testbasicset");
        FileSet resultFileSet = (FileSet)project.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(project);
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
    public void testSmallSet() {
        Project project = getProject();
        executeTarget("testsmallset");
        FileSet resultFileSet = (FileSet)project.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(project);
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
     * Test conbo classfileset
     */
    public void testComboSet() {
        Project project = getProject();
        executeTarget("testcomboset");
        FileSet resultFileSet = (FileSet)project.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(project);
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
    public void testByReference() {
        executeTarget("testbyreference");
    }
    
    /**
     * Test that classes included in a method "System.out.println(MyClass.class)" are included.
     */
    public void testMethodParam() {
        Project project = getProject();
        executeTarget("testmethodparam");
        FileSet resultFileSet = (FileSet)project.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(project);
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
    public void testMethodParamInner() {
        Project project = getProject();
        executeTarget("testmethodparaminner");
        FileSet resultFileSet = (FileSet)project.getReference(RESULT_FILESET);
        DirectoryScanner scanner = resultFileSet.getDirectoryScanner(project);
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
}
