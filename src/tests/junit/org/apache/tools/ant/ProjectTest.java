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

package org.apache.tools.ant;

import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.File;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Very limited test class for Project. Waiting to be extended.
 *
 */
public class ProjectTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private Project p;
    private String root;
    private MockBuildListener mbl;

    @Before
    public void setUp() {
        p = new Project();
        p.init();
        root = new File(File.separator).getAbsolutePath().toUpperCase();
        mbl = new MockBuildListener(p);
    }

    @Test
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
    @Test
    public void testResolveFile() {
        if (Os.isFamily("netware") || Os.isFamily("dos")) {
            assertEqualsIgnoreDriveCase(localize(File.separator),
                p.resolveFile("/", null).getPath());
            assertEqualsIgnoreDriveCase(localize(File.separator),
                p.resolveFile("\\", null).getPath());
            /*
             * throw in drive letters
             */
            String driveSpec = "C:";
            String driveSpecLower = "c:";
            
            assertEqualsIgnoreDriveCase(driveSpecLower + "\\",
                         p.resolveFile(driveSpec + "/", null).getPath());
            assertEqualsIgnoreDriveCase(driveSpecLower + "\\",
                         p.resolveFile(driveSpec + "\\", null).getPath());
            assertEqualsIgnoreDriveCase(driveSpecLower + "\\",
                         p.resolveFile(driveSpecLower + "/", null).getPath());
            assertEqualsIgnoreDriveCase(driveSpecLower + "\\",
                         p.resolveFile(driveSpecLower + "\\", null).getPath());
            /*
             * promised to eliminate consecutive slashes after drive letter.
             */
            assertEqualsIgnoreDriveCase(driveSpec + "\\",
                         p.resolveFile(driveSpec + "/////", null).getPath());
            assertEqualsIgnoreDriveCase(driveSpec + "\\",
                         p.resolveFile(driveSpec + "\\\\\\\\\\\\", null).getPath());
        } else {
            /*
             * Start with simple absolute file names.
             */
            assertEquals(File.separator,
                         p.resolveFile("/", null).getPath());
            assertEquals(File.separator,
                         p.resolveFile("\\", null).getPath());
            /*
             * drive letters are not used, just to be considered as normal
             * part of a name
             */
            String driveSpec = "C:";
            String udir = System.getProperty("user.dir") + File.separatorChar;
            assertEquals(udir + driveSpec,
                         p.resolveFile(driveSpec + "/", null).getPath());
            assertEquals(udir + driveSpec,
                         p.resolveFile(driveSpec + "\\", null).getPath());
            String driveSpecLower = "c:";
            assertEquals(udir + driveSpecLower,
                         p.resolveFile(driveSpecLower + "/", null).getPath());
            assertEquals(udir + driveSpecLower,
                         p.resolveFile(driveSpecLower + "\\", null).getPath());
        }
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

    /**
     * convenience method
     * the drive letter is in lower case under cygwin
     * calling this method allows tests where FileUtils.normalize
     * is called via resolveFile to pass under cygwin
     */
    private void assertEqualsIgnoreDriveCase(String s1, String s2) {
        if ((Os.isFamily("dos") || Os.isFamily("netware"))
            && s1.length() >= 1 && s2.length() >= 1) {
            StringBuffer sb1 = new StringBuffer(s1);
            StringBuffer sb2 = new StringBuffer(s2);
            sb1.setCharAt(0, Character.toUpperCase(s1.charAt(0)));
            sb2.setCharAt(0, Character.toUpperCase(s2.charAt(0)));
            assertEquals(sb1.toString(), sb2.toString());
        } else {
            assertEquals(s1, s2);
        }
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

    @Test
    public void testAddTaskDefinition() {
        p.addBuildListener(mbl);

        p.addTaskDefinition("Ok", DummyTaskOk.class);
        assertEquals(DummyTaskOk.class, p.getTaskDefinitions().get("Ok"));
        p.addTaskDefinition("OkNonTask", DummyTaskOkNonTask.class);
        assertEquals(DummyTaskOkNonTask.class, p.getTaskDefinitions().get("OkNonTask"));
        mbl.assertEmpty();

        assertTaskDefFails(DummyTaskPrivate.class,   DummyTaskPrivate.class   + " is not public");

        assertTaskDefFails(DummyTaskProtected.class,
                           DummyTaskProtected.class + " is not public");

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

    @Test
    public void testInputHandler() {
        InputHandler ih = p.getInputHandler();
        assertNotNull(ih);
        assertTrue(ih instanceof DefaultInputHandler);
        InputHandler pfih = new PropertyFileInputHandler();
        p.setInputHandler(pfih);
        assertSame(pfih, p.getInputHandler());
    }

    @Test
    public void testTaskDefinitionContainsKey() {
        assertTrue(p.getTaskDefinitions().containsKey("echo"));
    }

    @Test
    public void testTaskDefinitionContains() {
        assertTrue(p.getTaskDefinitions().contains(org.apache.tools.ant.taskdefs.Echo.class));
    }

    @Test
    public void testDuplicateTargets() {
        // fail, because buildfile contains two targets with the same name
        try {
            buildRule.configureProject("src/etc/testcases/core/duplicate-target.xml");
            fail("Should throw BuildException about duplicate target");
        } catch (BuildException ex) {
            assertEquals("specific message",
                         "Duplicate target 'twice'",
                         ex.getMessage());
        }
    }

    @Test
    public void testDuplicateTargetsImport() {
        // overriding target from imported buildfile is allowed
        buildRule.configureProject("src/etc/testcases/core/duplicate-target2.xml");
        buildRule.executeTarget("once");
        assertContains("once from buildfile", buildRule.getLog());
    }

    @Test
    public void testOutputDuringMessageLoggedIsSwallowed()
        throws InterruptedException {
        final String FOO = "foo", BAR = "bar";
        p.addBuildListener(new BuildListener() {
                public void buildStarted(BuildEvent event) {}
                public void buildFinished(BuildEvent event) {}
                public void targetStarted(BuildEvent event) {}
                public void targetFinished(BuildEvent event) {}
                public void taskStarted(BuildEvent event) {}
                public void taskFinished(BuildEvent event) {}
                public void messageLogged(final BuildEvent actual) {
                    assertEquals(FOO, actual.getMessage());
                    // each of the following lines would cause an
                    // infinite loop if the message wasn't swallowed
                    System.err.println(BAR);
                    System.out.println(BAR);
                    p.log(BAR, Project.MSG_INFO);
                }
            });
        final boolean[] done = new boolean[] {false};
        Thread t = new Thread() {
                public void run() {
                    p.log(FOO, Project.MSG_INFO);
                    done[0] = true;
                }
            };
        t.start();
        t.join(2000);
        assertTrue("Expected logging thread to finish successfully", done[0]);
    }

    /**
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=47623">
     *     https://issues.apache.org/bugzilla/show_bug.cgi?id=47623</a>
     */
    @Test
    public void testNullThrowableMessageLog() {
        p.log(new Task() {}, null, new Throwable(), Project.MSG_ERR);
        // be content if no exception has been thrown
    }

    private class DummyTaskPrivate extends Task {
        public DummyTaskPrivate() {}
        public void execute() {}
    }

    protected class DummyTaskProtected extends Task {
        public DummyTaskProtected() {}
        public void execute() {}
    }


    class DummyTaskPackage extends Task {
        public DummyTaskPackage() {}
        public void execute() {}
    }

}