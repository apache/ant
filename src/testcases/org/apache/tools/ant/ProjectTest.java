/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Very limited test class for Project. Waiting to be extended.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 */
public class ProjectTest extends TestCase {

    private Project p;
    private String root;
    private MockBuildListener mbl;

    public ProjectTest(String name) {
        super(name);
    }

    public void setUp() {
        p = new Project();
        p.init();
        root = new File(File.separator).getAbsolutePath();
        mbl = new MockBuildListener(p);
    }

    public void testDataTypes() throws BuildException {
        assertNull("dummy is not a known data type", 
                   p.createDataType("dummy"));
        Object o = p.createDataType("fileset");
        assertNotNull("fileset is a known type", o);
        assertTrue("fileset creates FileSet", o instanceof FileSet);
        assertTrue("PatternSet", 
               p.createDataType("patternset") instanceof PatternSet);
        assertTrue("Path", p.createDataType("path") instanceof Path);
    }

    /**
     * This test has been a starting point for moving the code to FileUtils.
     */
    public void testResolveFile() {
        /*
         * Start with simple absolute file names.
         */
        assertEquals(File.separator, 
                     p.resolveFile("/", null).getPath());
        assertEquals(File.separator, 
                     p.resolveFile("\\", null).getPath());

        /*
         * throw in drive letters
         */
        String driveSpec = "C:";
        assertEquals(driveSpec + "\\", 
                     p.resolveFile(driveSpec + "/", null).getPath());
        assertEquals(driveSpec + "\\", 
                     p.resolveFile(driveSpec + "\\", null).getPath());
        String driveSpecLower = "c:";
        assertEquals(driveSpec + "\\", 
                     p.resolveFile(driveSpecLower + "/", null).getPath());
        assertEquals(driveSpec + "\\", 
                     p.resolveFile(driveSpecLower + "\\", null).getPath());
        /*
         * promised to eliminate consecutive slashes after drive letter.
         */
        assertEquals(driveSpec + "\\", 
                     p.resolveFile(driveSpec + "/////", null).getPath());
        assertEquals(driveSpec + "\\", 
                     p.resolveFile(driveSpec + "\\\\\\\\\\\\", null).getPath());

        /*
         * Now test some relative file name magic.
         */
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("./4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile(".\\4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("./.\\4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("../3/4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("..\\3\\4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("../../5/.././2/./3/6/../4", new File(localize("/1/2/3"))).getPath());
        assertEquals(localize("/1/2/3/4"),
                     p.resolveFile("..\\../5/..\\./2/./3/6\\../4", new File(localize("/1/2/3"))).getPath());

    }

    /**
     * adapt file separators to local conventions
     */
    private String localize(String path) {
        path = root + path.substring(1);
        return path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    }
    

    private void assertTaskDefFails(final Class taskClass, 
                                       final String message) {
        final String dummyName = "testTaskDefinitionDummy";
        try {
            mbl.addBuildEvent(message, Project.MSG_ERR);
            p.addTaskDefinition(dummyName, taskClass);
            fail("expected BuildException(\""+message+"\", Project.MSG_ERR) when adding task " + taskClass);
        }
        catch(BuildException e) {
            assertEquals(message, e.getMessage());
            mbl.assertEmpty();
            assertTrue(!p.getTaskDefinitions().containsKey(dummyName));
        }
    }
    
    public void testAddTaskDefinition() {
        p.addBuildListener(mbl);

        p.addTaskDefinition("Ok", DummyTaskOk.class);
        assertEquals(DummyTaskOk.class, p.getTaskDefinitions().get("Ok"));
        p.addTaskDefinition("OkNonTask", DummyTaskOkNonTask.class);
        assertEquals(DummyTaskOkNonTask.class, p.getTaskDefinitions().get("OkNonTask"));
        mbl.assertEmpty();

        assertTaskDefFails(DummyTaskPrivate.class,   DummyTaskPrivate.class   + " is not public");

        try {
            assertTaskDefFails(DummyTaskProtected.class, 
                               DummyTaskProtected.class + " is not public");
        } catch (AssertionFailedError e) {
            /*
             * I don't understand this, but this is what happens with
             * > java -fullversion
             * java full version "Linux_JDK_1.1.8_v3_green_threads"
             * from time to time
             */
            assertTrue(JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1));
            assertTaskDefFails(DummyTaskProtected.class, 
                               "No public no-arg constructor in " 
                               + DummyTaskProtected.class);
        }
        
        assertTaskDefFails(DummyTaskPackage.class,   DummyTaskPackage.class   + " is not public");

        assertTaskDefFails(DummyTaskAbstract.class,  DummyTaskAbstract.class  + " is abstract"); 
        assertTaskDefFails(DummyTaskInterface.class, DummyTaskInterface.class + " is abstract");

        assertTaskDefFails(DummyTaskWithoutDefaultConstructor.class, "No public no-arg constructor in " + DummyTaskWithoutDefaultConstructor.class);
        assertTaskDefFails(DummyTaskWithoutPublicConstructor.class,  "No public no-arg constructor in " + DummyTaskWithoutPublicConstructor.class);
        
        assertTaskDefFails(DummyTaskWithoutExecute.class,       "No public execute() in " + DummyTaskWithoutExecute.class);
        assertTaskDefFails(DummyTaskWithNonPublicExecute.class, "No public execute() in " + DummyTaskWithNonPublicExecute.class);
        
        mbl.addBuildEvent("return type of execute() should be void but was \"int\" in " + DummyTaskWithNonVoidExecute.class, Project.MSG_WARN);
        p.addTaskDefinition("NonVoidExecute", DummyTaskWithNonVoidExecute.class);
        mbl.assertEmpty();
        assertEquals(DummyTaskWithNonVoidExecute.class, p.getTaskDefinitions().get("NonVoidExecute"));
    }
        
    public void testInputHandler() {
        InputHandler ih = p.getInputHandler();
        assertNotNull(ih);
        assertTrue(ih instanceof DefaultInputHandler);
        InputHandler pfih = new PropertyFileInputHandler();
        p.setInputHandler(pfih);
        assertSame(pfih, p.getInputHandler());
    }

    public void testTaskDefinitionContainsKey() {
        assertTrue(p.getTaskDefinitions().containsKey("echo"));
    }

    public void testTaskDefinitionContains() {
        assertTrue(p.getTaskDefinitions().contains(org.apache.tools.ant.taskdefs.Echo.class));
    }

    private class DummyTaskPrivate extends Task {
        public DummyTaskPrivate() {}
        public void execute() {}
    }

    protected class DummyTaskProtected extends Task {
        public DummyTaskProtected() {}
        public void execute() {}
    }

}

class DummyTaskPackage extends Task {
    public DummyTaskPackage() {}
    public void execute() {}
}
