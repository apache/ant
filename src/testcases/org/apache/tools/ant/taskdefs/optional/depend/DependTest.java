/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.depend;

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
 * Testcase for the Depend optional task. 
 * 
 * @author Conor MacNeill
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
        Project project = getProject();
        executeTarget("testdirect");
        Hashtable files = getResultFiles();
        assertEquals("Depend did not leave correct number of files", 2, 
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
        Project project = getProject();
        executeTarget("testclosure");
        Hashtable files = getResultFiles();
        assertEquals("Depend did not leave correct number of files", 1, 
            files.size());
        assertTrue("Result did not contain D.class", 
            files.containsKey("D.class"));
    }

    /**
     * Test that inner class dependencies trigger deletion of the outer class
     */
    public void testInner() {
        Project project = getProject();
        executeTarget("testinner");
        assertEquals("Depend did not leave correct number of files", 0, 
            getResultFiles().size());
    }

    /**
     * Test that multi-leve inner class dependencies trigger deletion of 
     * the outer class
     */
    public void testInnerInner() {
        Project project = getProject();
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
        FileSet resultFileSet = (FileSet)project.getReference(RESULT_FILESET);
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
        Project project = getProject();
        executeTarget("testinnerclosure");
        assertEquals("Depend did not leave correct number of files", 2, 
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
