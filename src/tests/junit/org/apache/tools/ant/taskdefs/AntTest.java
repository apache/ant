/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.types.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 */
public class AntTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/ant.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    /**
     * Fail due to recursive call
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO assert exception message
    }

    /**
     * Fail due to unspecified target
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO assert exception message
    }

    /**
     * Fail due to recursive call
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO assert exception message
    }

    /**
     * Fail due to empty target name
     */
    @Test(expected = BuildException.class)
    public void test4() {
        buildRule.executeTarget("test4");
        // TODO assert exception message
    }

    /**
     * Fail due to nonexistent target
     */
    @Test(expected = BuildException.class)
    public void test4b() {
        buildRule.executeTarget("test4b");
        // TODO assert exception message
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    @Test
    public void testExplicitBasedir1() {
        File dir1 = buildRule.getProject().getBaseDir();
        File dir2 = buildRule.getProject().resolveFile("..");
        testBaseDirs("explicitBasedir1",
                new String[] {dir1.getAbsolutePath(), dir2.getAbsolutePath()});
    }

    @Test
    public void testExplicitBasedir2() {
        File dir1 = buildRule.getProject().getBaseDir();
        File dir2 = buildRule.getProject().resolveFile("..");
        testBaseDirs("explicitBasedir2",
                new String[] {dir1.getAbsolutePath(), dir2.getAbsolutePath()});
    }

    @Test
    public void testInheritBasedir() {
        String basedir = buildRule.getProject().getBaseDir().getAbsolutePath();
        testBaseDirs("inheritBasedir", new String[] {basedir, basedir});
    }

    @Test
    public void testDoNotInheritBasedir() {
        File dir1 = buildRule.getProject().getBaseDir();
        File dir2 = buildRule.getProject().resolveFile("ant");
        testBaseDirs("doNotInheritBasedir",
                new String[] {dir1.getAbsolutePath(), dir2.getAbsolutePath()});
    }

    @Test
    public void testBasedirTripleCall() {
        File dir1 = buildRule.getProject().getBaseDir();
        File dir2 = buildRule.getProject().resolveFile("ant");
        testBaseDirs("tripleCall",
                new String[] {dir1.getAbsolutePath(), dir2.getAbsolutePath(), dir1.getAbsolutePath()});
    }

    protected void testBaseDirs(String target, String[] dirs) {
        BasedirChecker bc = new BasedirChecker(dirs);
        buildRule.getProject().addBuildListener(bc);
        buildRule.executeTarget(target);
        AssertionError ae = bc.getError();
        if (ae != null) {
            throw ae;
        }
        buildRule.getProject().removeBuildListener(bc);
    }

    @Test
    public void testReferenceInheritance() {
        Path p = Path.systemClasspath;
        p.setProject(buildRule.getProject());
        buildRule.getProject().addReference("path", p);
        buildRule.getProject().addReference("no-override", p);
        testReference("testInherit", new String[] {"path", "path"},
                      new boolean[] {true, true}, p);
        testReference("testInherit",
                      new String[] {"no-override", "no-override"},
                      new boolean[] {true, false}, p);
        testReference("testInherit",
                      new String[] {"no-override", "no-override"},
                      new boolean[] {false, false}, null);
    }

    @Test
    public void testReferenceNoInheritance() {
        Path p = Path.systemClasspath;
        p.setProject(buildRule.getProject());
        buildRule.getProject().addReference("path", p);
        buildRule.getProject().addReference("no-override", p);
        testReference("testNoInherit", new String[] {"path", "path"},
                      new boolean[] {true, false}, p);
        testReference("testNoInherit", new String[] {"path", "path"},
                      new boolean[] {false, true}, null);
        testReference("testInherit",
                      new String[] {"no-override", "no-override"},
                      new boolean[] {true, false}, p);
        testReference("testInherit",
                      new String[] {"no-override", "no-override"},
                      new boolean[] {false, false}, null);
    }

    @Test
    public void testReferenceRename() {
        Path p = Path.systemClasspath;
        p.setProject(buildRule.getProject());
        buildRule.getProject().addReference("path", p);
        testReference("testRename", new String[] {"path", "path"},
                      new boolean[] {true, false}, p);
        testReference("testRename", new String[] {"path", "path"},
                      new boolean[] {false, true}, null);
        testReference("testRename", new String[] {"newpath", "newpath"},
                      new boolean[] {false, true}, p);
    }

    @Test
    public void testInheritPath() {
        buildRule.executeTarget("testInheritPath");
    }

    protected void testReference(String target, String[] keys,
                                 boolean[] expect, Object value) {
        ReferenceChecker rc = new ReferenceChecker(keys, expect, value);
        buildRule.getProject().addBuildListener(rc);
        buildRule.executeTarget(target);
        AssertionError ae = rc.getError();
        if (ae != null) {
            throw ae;
        }
        buildRule.getProject().removeBuildListener(rc);
    }

    @Test
    public void testLogfilePlacement() {
        List<File> logFiles = Arrays.asList(buildRule.getProject().resolveFile("test1.log"),
                buildRule.getProject().resolveFile("test2.log"),
                buildRule.getProject().resolveFile("ant/test3.log"),
                buildRule.getProject().resolveFile("ant/test4.log"));

        logFiles.forEach(logFile -> assertFalse(logFile.getName() + " doesn't exist",
                logFile.exists()));

        buildRule.executeTarget("testLogfilePlacement");

        logFiles.forEach(logFile -> assertTrue(logFile.getName() + " exists", logFile.exists()));
    }

    @Test
    public void testInputHandlerInheritance() {
        InputHandler ih = new PropertyFileInputHandler();
        buildRule.getProject().setInputHandler(ih);
        InputHandlerChecker ic = new InputHandlerChecker(ih);
        buildRule.getProject().addBuildListener(ic);
        buildRule.executeTarget("tripleCall");
        AssertionError ae = ic.getError();
        if (ae != null) {
            throw ae;
        }
        buildRule.getProject().removeBuildListener(ic);
    }

    @Test
    public void testRefId() {
        Path testPath = new Path(buildRule.getProject());
        testPath.createPath().setPath(System.getProperty("java.class.path"));
        PropertyChecker pc = new PropertyChecker("testprop",
                new String[] {null, testPath.toString()});
        buildRule.getProject().addBuildListener(pc);
        buildRule.executeTarget("testRefid");
        AssertionError ae = pc.getError();
        if (ae != null) {
            throw ae;
        }
        buildRule.getProject().removeBuildListener(pc);
    }

    @Test
    public void testUserPropertyWinsInheritAll() {
        buildRule.getProject().setUserProperty("test", "7");
        buildRule.executeTarget("test-property-override-inheritall-start");
        assertThat(buildRule.getLog(), containsString("The value of test is 7"));
    }

    @Test
    public void testUserPropertyWinsNoInheritAll() {
        buildRule.getProject().setUserProperty("test", "7");
        buildRule.executeTarget("test-property-override-no-inheritall-start");
        assertThat(buildRule.getLog(), containsString("The value of test is 7"));
    }

    @Test
    public void testOverrideWinsInheritAll() {
        buildRule.executeTarget("test-property-override-inheritall-start");
        assertThat(buildRule.getLog(), containsString("The value of test is 4"));
    }

    @Test
    public void testOverrideWinsNoInheritAll() {
        buildRule.executeTarget("test-property-override-no-inheritall-start");
        assertThat(buildRule.getLog(), containsString("The value of test is 4"));
    }

    @Test
    public void testPropertySet() {
        buildRule.executeTarget("test-propertyset");
        assertThat(buildRule.getLog(), containsString("test1 is ${test1}"));
        assertThat(buildRule.getLog(), containsString("test2 is ${test2}"));
        assertThat(buildRule.getLog(), containsString("test1.x is 1"));
    }

    /**
     * Fail due to infinite recursion loop
     */
    @Test(expected = BuildException.class)
    public void testInfiniteLoopViaDepends() {
        buildRule.executeTarget("infinite-loop-via-depends");
        // TODO assert exception message
    }

    @Test
    public void testMultiSameProperty() {
        buildRule.executeTarget("multi-same-property");
        assertEquals("prop is two", buildRule.getLog());
    }

    @Test
    public void testTopLevelTarget() {
        buildRule.executeTarget("topleveltarget");
        assertEquals("Hello world", buildRule.getLog());
    }

    @Test
    public void testMultiplePropertyFileChildren() {
        PropertyChecker pcBar = new PropertyChecker("bar",
                                                    new String[] {null, "Bar"});
        PropertyChecker pcFoo = new PropertyChecker("foo",
                                                    new String[] {null, "Foo"});
        buildRule.getProject().addBuildListener(pcBar);
        buildRule.getProject().addBuildListener(pcFoo);
        buildRule.executeTarget("multiple-property-file-children");
        AssertionError aeBar = pcBar.getError();
        if (aeBar != null) {
            throw aeBar;
        }
        AssertionError aeFoo = pcFoo.getError();
        if (aeFoo != null) {
            throw aeFoo;
        }
        buildRule.getProject().removeBuildListener(pcBar);
        buildRule.getProject().removeBuildListener(pcFoo);
    }

    /**
     * Fail due to empty target name
     */
    @Test(expected = BuildException.class)
    public void testBlankTarget() {
        buildRule.executeTarget("blank-target");
        // TODO assert exception message
    }

    @Test
    public void testMultipleTargets() {
        buildRule.executeTarget("multiple-targets");
        assertEquals("tadadctbdbtc", buildRule.getLog());
    }

    @Test
    public void testMultipleTargets2() {
        buildRule.executeTarget("multiple-targets-2");
        assertEquals("dadctb", buildRule.getLog());
    }

    @Test
    public void testAntCoreLib() {
        // Cf. #42263
        buildRule.executeTarget("sub-show-ant.core.lib");
        String realLog = buildRule.getLog();
        assertTrue("found ant.core.lib in: " + realLog,
                realLog.matches(".*(ant[.]jar|ant.classes|build.classes).*"));
    }

    private class BasedirChecker implements BuildListener {
        private String[] expectedBasedirs;
        private int calls = 0;
        private AssertionError error;

        BasedirChecker(String[] dirs) {
            expectedBasedirs = dirs;
        }

        public void buildStarted(BuildEvent event) {
        }

        public void buildFinished(BuildEvent event) {
        }

        public void targetFinished(BuildEvent event) {
        }

        public void taskStarted(BuildEvent event) {
        }

        public void taskFinished(BuildEvent event) {
        }

        public void messageLogged(BuildEvent event) {
        }

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().isEmpty()) {
                return;
            }
            if (error == null) {
                try {
                    assertEquals(expectedBasedirs[calls++],
                                 event.getProject().getBaseDir().getAbsolutePath());
                } catch (AssertionError e) {
                    error = e;
                }
            }
        }

        AssertionError getError() {
            return error;
        }
    }

    private class ReferenceChecker implements BuildListener {
        private String[] keys;
        private boolean[] expectSame;
        private Object value;
        private int calls = 0;
        private AssertionError error;

        ReferenceChecker(String[] keys, boolean[] expectSame, Object value) {
            this.keys = keys;
            this.expectSame = expectSame;
            this.value = value;
        }

        public void buildStarted(BuildEvent event) {
        }

        public void buildFinished(BuildEvent event) {
        }

        public void targetFinished(BuildEvent event) {
        }

        public void taskStarted(BuildEvent event) {
        }

        public void taskFinished(BuildEvent event) {
        }

        public void messageLogged(BuildEvent event) {
        }

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().isEmpty()) {
                return;
            }
            if (error == null) {
                try {
                    String msg = "Call " + calls + " refid='" + keys[calls] + "'";
                    if (value == null) {
                        Object o = event.getProject().getReference(keys[calls]);
                        if (expectSame[calls++]) {
                            assertNull(msg, o);
                        } else {
                            assertNotNull(msg, o);
                        }
                    } else {
                        // a rather convoluted equals() test
                        Path expect = (Path) value;
                        Path received = event.getProject().getReference(keys[calls]);
                        boolean shouldBeEqual = expectSame[calls++];
                        if (received == null) {
                            assertFalse(msg, shouldBeEqual);
                        } else {
                            String[] l1 = expect.list();
                            String[] l2 = received.list();
                            if (l1.length == l2.length) {
                                for (int i = 0; i < l1.length; i++) {
                                    if (!l1[i].equals(l2[i])) {
                                        assertFalse(msg, shouldBeEqual);
                                    }
                                }
                                assertTrue(msg, shouldBeEqual);
                            } else {
                                assertFalse(msg, shouldBeEqual);
                            }
                        }
                    }
                } catch (AssertionError e) {
                    error = e;
                }
            }
        }

        AssertionError getError() {
            return error;
        }
    }

    private class InputHandlerChecker implements BuildListener {
        private InputHandler ih;
        private AssertionError error;

        InputHandlerChecker(InputHandler value) {
            ih = value;
        }

        public void buildStarted(BuildEvent event) {
            check(event);
        }

        public void buildFinished(BuildEvent event) {
            check(event);
        }

        public void targetFinished(BuildEvent event) {
            check(event);
        }

        public void taskStarted(BuildEvent event) {
            check(event);
        }

        public void taskFinished(BuildEvent event) {
            check(event);
        }

        public void messageLogged(BuildEvent event) {
            check(event);
        }

        public void targetStarted(BuildEvent event) {
            check(event);
        }

        private void check(BuildEvent event) {
            if (error == null) {
                try {
                    assertNotNull(event.getProject().getInputHandler());
                    assertSame(ih, event.getProject().getInputHandler());
                } catch (AssertionError e) {
                    error = e;
                }
            }
        }

        AssertionError getError() {
            return error;
        }
    }

    private class PropertyChecker implements BuildListener {
        private String[] expectedValues;
        private String key;
        private int calls = 0;
        private AssertionError error;

        PropertyChecker(String key, String[] values) {
            this.key = key;
            this.expectedValues = values;
        }

        public void buildStarted(BuildEvent event) {
        }

        public void buildFinished(BuildEvent event) {
        }

        public void targetFinished(BuildEvent event) {
        }

        public void taskStarted(BuildEvent event) {
        }

        public void taskFinished(BuildEvent event) {
        }

        public void messageLogged(BuildEvent event) {
        }

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().isEmpty()) {
                return;
            }
            if (calls >= expectedValues.length) {
                error = new AssertionError("Unexpected invocation of target "
                        + event.getTarget().getName());
            }

            if (error == null) {
                try {
                    assertEquals(expectedValues[calls++],
                                 event.getProject().getProperty(key));
                } catch (AssertionError e) {
                    error = e;
                }
            }
        }

        AssertionError getError() {
            return error;
        }
    }

}
