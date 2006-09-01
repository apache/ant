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

package org.apache.tools.ant.types;

import java.io.File;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Base class for FileSetTest and DirSetTest.
 *
 * <p>This doesn't actually test much, mainly reference handling.
 *
 */

public abstract class AbstractFileSetTest extends TestCase {

    private Project project;

    public AbstractFileSetTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");
    }

    protected abstract AbstractFileSet getInstance();

    protected final Project getProject() {
        return project;
    }

    public final void testEmptyElementIfIsReference() {
        AbstractFileSet f = getInstance();
        f.setIncludes("**/*.java");
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to "
                 + f.getDataTypeName()
                 + " with elements from setIncludes");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }

        f = getInstance();
        f.createPatternSet();
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to "
                 + f.getDataTypeName()
                 + " with nested patternset element.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when "
                         + "using refid", be.getMessage());
        }

        f = getInstance();
        f.createInclude();
        try {
            f.setRefid(new Reference("dummyref"));
            fail("Can add reference to "
                 + f.getDataTypeName()
                 + " with nested include element.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }

        f = getInstance();
        f.setRefid(new Reference("dummyref"));
        try {
            f.setIncludes("**/*.java");
            fail("Can set includes in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.setIncludesfile(new File("/a"));
            fail("Can set includesfile in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.setExcludes("**/*.java");
            fail("Can set excludes in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.setExcludesfile(new File("/a"));
            fail("Can set excludesfile in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.setDir(project.resolveFile("."));
            fail("Can set dir in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute "
                         + "when using refid", be.getMessage());
        }
        try {
            f.createInclude();
            fail("Can add nested include in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using "
                         + "refid", be.getMessage());
        }
        try {
            f.createExclude();
            fail("Can add nested exclude in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using "
                         + "refid", be.getMessage());
        }
        try {
            f.createIncludesFile();
            fail("Can add nested includesfile in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using "
                         + "refid", be.getMessage());
        }
        try {
            f.createExcludesFile();
            fail("Can add nested excludesfile in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using "
                         + "refid", be.getMessage());
        }
        try {
            f.createPatternSet();
            fail("Can add nested patternset in "
                 + f.getDataTypeName()
                 + " that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using "
                         + "refid", be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {
        AbstractFileSet f = getInstance();
        project.addReference("dummy", f);
        f.setRefid(new Reference("dummy"));
        try {
            f.getDir(project);
            fail("Can make " + f.getDataTypeName()
                 + " a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f.getDirectoryScanner(project);
            fail("Can make " + f.getDataTypeName()
                 + " a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        AbstractFileSet f1 = getInstance();
        project.addReference("dummy1", f1);
        f1.setRefid(new Reference("dummy2"));
        AbstractFileSet f2 = getInstance();
        project.addReference("dummy2", f2);
        f2.setRefid(new Reference("dummy3"));
        AbstractFileSet f3 = getInstance();
        project.addReference("dummy3", f3);
        f3.setRefid(new Reference("dummy1"));
        try {
            f1.getDir(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            f1.getDirectoryScanner(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3
        // (which has the Project's basedir as root).
        f1 = getInstance();
        project.addReference("dummy1", f1);
        f1.setRefid(new Reference("dummy2"));
        f2 = getInstance();
        project.addReference("dummy2", f2);
        f2.setRefid(new Reference("dummy3"));
        f3 = getInstance();
        project.addReference("dummy3", f3);
        f3.setDir(project.resolveFile("."));
        File dir = f1.getDir(project);
        assertEquals("Dir is basedir", dir, project.getBaseDir());
    }
}
