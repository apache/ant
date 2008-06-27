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

package org.apache.tools.ant.taskdefs;

import java.io.File;

import junit.framework.AssertionFailedError;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.types.Path;

/**
 */
public class AntTest extends BuildFileTest {

    public AntTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/ant.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "recursive call");
    }

    // target must be specified
    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    // Should fail since a recursion will occur...
    public void test3() {
        expectBuildException("test1", "recursive call");
    }

    public void test4() {
        expectBuildException("test4", "target attribute must not be empty");
    }

    public void test4b() {
        expectBuildException("test4b", "target doesn't exist");
    }

    public void test5() {
        executeTarget("test5");
    }

    public void test6() {
        executeTarget("test6");
    }

    public void testExplicitBasedir1() {
        File dir1 = getProjectDir();
        File dir2 = project.resolveFile("..");
        testBaseDirs("explicitBasedir1",
                     new String[] {dir1.getAbsolutePath(),
                                   dir2.getAbsolutePath()
                     });
    }

    public void testExplicitBasedir2() {
        File dir1 = getProjectDir();
        File dir2 = project.resolveFile("..");
        testBaseDirs("explicitBasedir2",
                     new String[] {dir1.getAbsolutePath(),
                                   dir2.getAbsolutePath()
                     });
    }

    public void testInheritBasedir() {
        String basedir = getProjectDir().getAbsolutePath();
        testBaseDirs("inheritBasedir", new String[] {basedir, basedir});
    }

    public void testDoNotInheritBasedir() {
        File dir1 = getProjectDir();
        File dir2 = project.resolveFile("ant");
        String basedir = getProjectDir().getAbsolutePath();
        testBaseDirs("doNotInheritBasedir",
                     new String[] {dir1.getAbsolutePath(),
                                   dir2.getAbsolutePath()
                     });
    }

    public void testBasedirTripleCall() {
        File dir1 = getProjectDir();
        File dir2 = project.resolveFile("ant");
        testBaseDirs("tripleCall",
                     new String[] {dir1.getAbsolutePath(),
                                   dir2.getAbsolutePath(),
                                   dir1.getAbsolutePath()
                     });
    }

    protected void testBaseDirs(String target, String[] dirs) {
        BasedirChecker bc = new BasedirChecker(dirs);
        project.addBuildListener(bc);
        executeTarget(target);
        AssertionFailedError ae = bc.getError();
        if (ae != null) {
            throw ae;
        }
        project.removeBuildListener(bc);
    }

    public void testReferenceInheritance() {
        Path p = Path.systemClasspath;
        p.setProject(project);
        project.addReference("path", p);
        project.addReference("no-override", p);
        testReference("testInherit", new String[] {"path", "path"},
                      new boolean[] {true, true}, p);
        testReference("testInherit",
                      new String[] {"no-override", "no-override"},
                      new boolean[] {true, false}, p);
        testReference("testInherit",
                      new String[] {"no-override", "no-override"},
                      new boolean[] {false, false}, null);
    }

    public void testReferenceNoInheritance() {
        Path p = Path.systemClasspath;
        p.setProject(project);
        project.addReference("path", p);
        project.addReference("no-override", p);
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

    public void testReferenceRename() {
        Path p = Path.systemClasspath;
        p.setProject(project);
        project.addReference("path", p);
        testReference("testRename", new String[] {"path", "path"},
                      new boolean[] {true, false}, p);
        testReference("testRename", new String[] {"path", "path"},
                      new boolean[] {false, true}, null);
        testReference("testRename", new String[] {"newpath", "newpath"},
                      new boolean[] {false, true}, p);
    }

    public void testInheritPath() {
        executeTarget("testInheritPath");
    }

    protected void testReference(String target, String[] keys,
                                 boolean[] expect, Object value) {
        ReferenceChecker rc = new ReferenceChecker(keys, expect, value);
        project.addBuildListener(rc);
        executeTarget(target);
        AssertionFailedError ae = rc.getError();
        if (ae != null) {
            throw ae;
        }
        project.removeBuildListener(rc);
    }

    public void testLogfilePlacement() {
        File[] logFiles = new File[] {
            getProject().resolveFile("test1.log"),
            getProject().resolveFile("test2.log"),
            getProject().resolveFile("ant/test3.log"),
            getProject().resolveFile("ant/test4.log")
        };
        for (int i=0; i<logFiles.length; i++) {
            assertTrue(logFiles[i].getName()+" doesn\'t exist",
                       !logFiles[i].exists());
        }

        executeTarget("testLogfilePlacement");

        for (int i=0; i<logFiles.length; i++) {
            assertTrue(logFiles[i].getName()+" exists",
                       logFiles[i].exists());
        }
    }

    public void testInputHandlerInheritance() {
        InputHandler ih = new PropertyFileInputHandler();
        getProject().setInputHandler(ih);
        InputHandlerChecker ic = new InputHandlerChecker(ih);
        getProject().addBuildListener(ic);
        executeTarget("tripleCall");
        AssertionFailedError ae = ic.getError();
        if (ae != null) {
            throw ae;
        }
        getProject().removeBuildListener(ic);
    }

    public void testRefId() {
        Path testPath = new Path(project);
        testPath.createPath().setPath(System.getProperty("java.class.path"));
        PropertyChecker pc =
            new PropertyChecker("testprop",
                                new String[] {null,
                                              testPath.toString()});
        project.addBuildListener(pc);
        executeTarget("testRefid");
        AssertionFailedError ae = pc.getError();
        if (ae != null) {
            throw ae;
        }
        project.removeBuildListener(pc);
    }

    public void testUserPropertyWinsInheritAll() {
        getProject().setUserProperty("test", "7");
        expectLogContaining("test-property-override-inheritall-start",
                            "The value of test is 7");
    }

    public void testUserPropertyWinsNoInheritAll() {
        getProject().setUserProperty("test", "7");
        expectLogContaining("test-property-override-no-inheritall-start",
                            "The value of test is 7");
    }

    public void testOverrideWinsInheritAll() {
        expectLogContaining("test-property-override-inheritall-start",
                            "The value of test is 4");
    }

    public void testOverrideWinsNoInheritAll() {
        expectLogContaining("test-property-override-no-inheritall-start",
                            "The value of test is 4");
    }

    public void testPropertySet() {
        executeTarget("test-propertyset");
        assertTrue(getLog().indexOf("test1 is ${test1}") > -1);
        assertTrue(getLog().indexOf("test2 is ${test2}") > -1);
        assertTrue(getLog().indexOf("test1.x is 1") > -1);
    }

    public void testInfiniteLoopViaDepends() {
        expectBuildException("infinite-loop-via-depends", "recursive call");
    }

    public void testMultiSameProperty() {
        expectLog("multi-same-property", "prop is two");
    }

    public void testTopLevelTarget() {
        expectLog("topleveltarget", "Hello world");
    }

    public void testMultiplePropertyFileChildren() {
        PropertyChecker pcBar = new PropertyChecker("bar",
                                                    new String[] {null, "Bar"});
        PropertyChecker pcFoo = new PropertyChecker("foo",
                                                    new String[] {null, "Foo"});
        project.addBuildListener(pcBar);
        project.addBuildListener(pcFoo);
        executeTarget("multiple-property-file-children");
        AssertionFailedError aeBar = pcBar.getError();
        if (aeBar != null) {
            throw aeBar;
        }
        AssertionFailedError aeFoo = pcFoo.getError();
        if (aeFoo != null) {
            throw aeFoo;
        }
        project.removeBuildListener(pcBar);
        project.removeBuildListener(pcFoo);
    }

    public void testBlankTarget() {
        expectBuildException("blank-target", "target name must not be empty");
    }

    public void testMultipleTargets() {
        expectLog("multiple-targets", "tadadctbdbtc");
    }

    public void testMultipleTargets2() {
        expectLog("multiple-targets-2", "dadctb");
    }

    private class BasedirChecker implements BuildListener {
        private String[] expectedBasedirs;
        private int calls = 0;
        private AssertionFailedError error;

        BasedirChecker(String[] dirs) {
            expectedBasedirs = dirs;
        }

        public void buildStarted(BuildEvent event) {}
        public void buildFinished(BuildEvent event) {}
        public void targetFinished(BuildEvent event){}
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().equals("")) {
                return;
            }
            if (error == null) {
                try {
                    assertEquals(expectedBasedirs[calls++],
                                 event.getProject().getBaseDir().getAbsolutePath());
                } catch (AssertionFailedError e) {
                    error = e;
                }
            }
        }

        AssertionFailedError getError() {
            return error;
        }

    }

    private class ReferenceChecker implements BuildListener {
        private String[] keys;
        private boolean[] expectSame;
        private Object value;
        private int calls = 0;
        private AssertionFailedError error;

        ReferenceChecker(String[] keys, boolean[] expectSame, Object value) {
            this.keys = keys;
            this.expectSame = expectSame;
            this.value = value;
        }

        public void buildStarted(BuildEvent event) {}
        public void buildFinished(BuildEvent event) {}
        public void targetFinished(BuildEvent event){}
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().equals("")) {
                return;
            }
            if (error == null) {
                try {
                    String msg =
                        "Call " + calls + " refid=\'" + keys[calls] + "\'";
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
                        Path received = (Path) event.getProject().getReference(keys[calls]);
                        boolean shouldBeEqual = expectSame[calls++];
                        if (received == null) {
                            assertTrue(msg, !shouldBeEqual);
                        } else {
                            String[] l1 = expect.list();
                            String[] l2 = received.list();
                            if (l1.length == l2.length) {
                                for (int i=0; i<l1.length; i++) {
                                    if (!l1[i].equals(l2[i])) {
                                        assertTrue(msg, !shouldBeEqual);
                                    }
                                }
                                assertTrue(msg, shouldBeEqual);
                            } else {
                                assertTrue(msg, !shouldBeEqual);
                            }
                        }
                    }
                } catch (AssertionFailedError e) {
                    error = e;
                }
            }
        }

        AssertionFailedError getError() {
            return error;
        }

    }

    private class InputHandlerChecker implements BuildListener {
        private InputHandler ih;
        private AssertionFailedError error;

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
                } catch (AssertionFailedError e) {
                    error = e;
                }
            }
        }

        AssertionFailedError getError() {
            return error;
        }

    }

    private class PropertyChecker implements BuildListener {
        private String[] expectedValues;
        private String key;
        private int calls = 0;
        private AssertionFailedError error;

        PropertyChecker(String key, String[] values) {
            this.key = key;
            this.expectedValues = values;
        }

        public void buildStarted(BuildEvent event) {}
        public void buildFinished(BuildEvent event) {}
        public void targetFinished(BuildEvent event){}
        public void taskStarted(BuildEvent event) {}
        public void taskFinished(BuildEvent event) {}
        public void messageLogged(BuildEvent event) {}

        public void targetStarted(BuildEvent event) {
            if (event.getTarget().getName().equals("")) {
                return;
            }
            if (calls >= expectedValues.length) {
                error = new AssertionFailedError("Unexpected invocation of"
                                                 + " target "
                                                 + event.getTarget().getName());
            }

            if (error == null) {
                try {
                    assertEquals(expectedValues[calls++],
                                 event.getProject().getProperty(key));
                } catch (AssertionFailedError e) {
                    error = e;
                }
            }
        }

        AssertionFailedError getError() {
            return error;
        }

    }


}
