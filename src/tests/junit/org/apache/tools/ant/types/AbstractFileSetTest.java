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

package org.apache.tools.ant.types;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Base class for FileSetTest and DirSetTest.
 *
 * <p>This doesn't actually test much, mainly reference handling.
 *
 */

public abstract class AbstractFileSetTest {

    private Project project;

    private AbstractFileSet f;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        project = new Project();
        project.setBasedir(".");
        f = getInstance();
    }

    protected abstract AbstractFileSet getInstance();

    protected final Project getProject() {
        return project;
    }

    @Test
    public final void testCannotSetIncludesThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setIncludes("**/*.java");
        f.setRefid(new Reference(getProject(), "dummyref"));
    }

    @Test
    public final void testCannotAddPatternSetThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        f.createPatternSet();
        f.setRefid(new Reference(getProject(), "dummyref"));
    }

    @Test
    public final void testCannotAddIncludeThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.createInclude();
        f.setRefid(new Reference(getProject(), "dummyref"));
    }

    @Test
    public final void testCannotSetRefidThenIncludes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.setIncludes("**/*.java");
    }

    @Test
    public final void testCannotSetRefidThenIncludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.setIncludesfile(new File("/a"));
    }

    @Test
    public final void testCannotSetRefidThenExcludes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.setExcludes("**/*.java");
    }

    @Test
    public final void testCannotSetRefidThenExcludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.setExcludesfile(new File("/a"));
    }

    @Test
    public final void testCannotSetRefidThenDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.setDir(project.resolveFile("."));
    }

    @Test
    public final void testCannotSetRefidThenAddInclude() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.createInclude();
    }

    @Test
    public final void testCannotSetRefidThenAddExclude() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.createExclude();
    }

    @Test
    public final void testCannotSetRefidThenAddIncludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.createIncludesFile();
    }

    @Test
    public final void testCannotSetRefidThenAddExcludesFile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.createExcludesFile();
    }

    @Test
    public final void testCannotSetRefidThenAddPatternset() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        f.setRefid(new Reference(getProject(), "dummyref"));
        f.createPatternSet();
    }

    @Test
    public void testCircularReferenceCheckGetDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", f);
        f.setRefid(new Reference(getProject(), "dummy"));
        f.getDir(project);
    }

    @Test
    public void testCircularReferenceCheckGetDirScanner() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", f);
        f.setRefid(new Reference(getProject(), "dummy"));
        f.getDirectoryScanner(project);
    }

    @Test
    public void testLoopReferenceCheckGetDir() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // dummy1 --> dummy2 --> dummy3 --> dummy1
        project.addReference("dummy1", f);
        f.setRefid(new Reference(getProject(), "dummy2"));
        AbstractFileSet fa = getInstance();
        project.addReference("dummy2", fa);
        fa.setRefid(new Reference(getProject(), "dummy3"));
        AbstractFileSet fb = getInstance();
        project.addReference("dummy3", fb);
        fb.setRefid(new Reference(getProject(), "dummy1"));
        f.getDir(project);
    }

    @Test
    public void testLoopReferenceCheckGetDirScanner() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // dummy1 --> dummy2 --> dummy3 --> dummy1
        project.addReference("dummy1", f);
        f.setRefid(new Reference(getProject(), "dummy2"));
        AbstractFileSet fa = getInstance();
        project.addReference("dummy2", fa);
        fa.setRefid(new Reference(getProject(), "dummy3"));
        AbstractFileSet fb = getInstance();
        project.addReference("dummy3", fb);
        fb.setRefid(new Reference(getProject(), "dummy1"));
        f.getDirectoryScanner(project);
    }

    @Test
    public void testLoopReferenceCheck() {
        // dummy1 --> dummy2 --> dummy3
        // (which has the Project's basedir as root).
        project.addReference("dummy1", f);
        f.setRefid(new Reference(getProject(), "dummy2"));
        AbstractFileSet fa = getInstance();
        project.addReference("dummy2", fa);
        fa.setRefid(new Reference(getProject(), "dummy3"));
        AbstractFileSet fb = getInstance();
        project.addReference("dummy3", fb);
        fb.setDir(project.resolveFile("."));
        File dir = f.getDir(project);
        assertEquals("Dir is basedir", dir, project.getBaseDir());
    }

    @Test
    public void canCallSetFileTwiceWithSameArgument() {
        f.setFile(new File("/a"));
        f.setFile(new File("/a"));
        // really only asserts no exception is thrown
    }

    @Test
    public void cannotCallSetFileTwiceWithDifferentArguments() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("setFile cannot be called twice with different arguments");
        f.setFile(new File("/a"));
        f.setFile(new File("/b"));
    }
}
