/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.script;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import java.io.File;

/**
 * Tests the examples of the &lt;scriptdef&gt; task.
 *
 * @author Conor MacNeill
 * @since Ant 1.6
 */
public class ScriptDefTest extends BuildFileTest {

    public ScriptDefTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/script/scriptdef.xml");
    }
    
    public void testSimple() {
        executeTarget("simple");
        // get the fileset and its basedir
        Project project = getProject();
        FileSet fileset = (FileSet) project.getReference("testfileset");
        File baseDir = fileset.getDir(project);
        String log = getLog();
        assertTrue("Expecting attribute value printed", 
            log.indexOf("Attribute attr1 = test") != -1);
            
        assertTrue("Expecting nested element value printed", 
            log.indexOf("Fileset basedir = " + baseDir.getAbsolutePath()) != -1);
    }

    public void testNoLang() {
        expectBuildExceptionContaining("nolang", 
            "Absence of language attribute not detected", 
            "requires a language attribute");
    }

    public void testNoName() {
        expectBuildExceptionContaining("noname", 
            "Absence of name attribute not detected", 
            "scriptdef requires a name attribute");
    }
    
    public void testNestedByClassName() {
        executeTarget("nestedbyclassname");
        // get the fileset and its basedir
        Project project = getProject();
        FileSet fileset = (FileSet) project.getReference("testfileset");
        File baseDir = fileset.getDir(project);
        String log = getLog();
        assertTrue("Expecting attribute value to be printed", 
            log.indexOf("Attribute attr1 = test") != -1);
            
        assertTrue("Expecting nested element value to be printed", 
            log.indexOf("Fileset basedir = " + baseDir.getAbsolutePath()) != -1);
    }

    public void testNoElement() {
        expectOutput("noelement", "Attribute attr1 = test");
    }
    
    public void testException() {
        expectBuildExceptionContaining("exception", 
            "Should have thrown an exception in the script", 
            "TypeError");
    }
    
    public void testDoubleDef() {
        executeTarget("doubledef");
        String log = getLog();
        assertTrue("Task1 did not execute", 
            log.indexOf("Task1") != -1);
        assertTrue("Task2 did not execute", 
            log.indexOf("Task2") != -1);
    }

    public void testDoubleAttribute() {
        expectBuildExceptionContaining("doubleAttributeDef", 
            "Should have detected duplicate attribute definition", 
            "attr1 attribute more than once");
    }
}
