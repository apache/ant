/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
 * @author Nico Seessle <nico@seessle.de> 
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @version $Revision$
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
        expectBuildException("test4", "target doesn't exist");
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
