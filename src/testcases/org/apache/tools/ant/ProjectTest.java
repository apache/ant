/*
 * Copyright  2000-2004 The Apache Software Foundation
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
 * @author Stefan Bodewig
 * @author Jan Matèrne
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

    // Bug in Ant 1.6/1.7 found by Dominique: there must no multiple
    // targets with the same name in a project.
    public void testDuplicateTargets() {
        BFT bft = new BFT("", "core/duplicate-target.xml");
        bft.expectBuildException("twice", "Duplicate target");
    }

    private class DummyTaskPrivate extends Task {
        public DummyTaskPrivate() {}
        public void execute() {}
    }

    protected class DummyTaskProtected extends Task {
        public DummyTaskProtected() {}
        public void execute() {}
    }

    private class BFT extends org.apache.tools.ant.BuildFileTest {
        BFT(String name, String buildfile) {
            super(name);
            this.buildfile = buildfile;
            setUp();
        }

        // avoid multiple configurations
        boolean isConfigured = false;

        // the buildfile to use
        String buildfile = "";

        public void setUp() {
            if (!isConfigured) {
                configureProject("src/etc/testcases/"+buildfile);
                isConfigured = true;
            }
        }

        public void tearDown() { }

        // call a target
        public void doTarget(String target) {
            if (!isConfigured) setUp();
            executeTarget(target);
        }

        public org.apache.tools.ant.Project getProject() {
            return super.getProject();
        }
    }//class-BFT

}

class DummyTaskPackage extends Task {
    public DummyTaskPackage() {}
    public void execute() {}
}