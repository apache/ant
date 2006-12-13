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
 * JUnit 3 testcases for org.apache.tools.ant.types.PatternSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.</p>
 *
 */

public class PatternSetTest extends TestCase {

    private Project project;

    public PatternSetTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");
    }

    public void testEmptyElementIfIsReference() {
        PatternSet p = new PatternSet();
        p.setIncludes("**/*.java");
        try {
            p.setRefid(new Reference("dummyref"));
            fail("Can add reference to PatternSet with elements from setIncludes");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        p = new PatternSet();
        p.setRefid(new Reference("dummyref"));
        try {
            p.setIncludes("**/*.java");
            fail("Can set includes in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        p = new PatternSet();
        p.setRefid(new Reference("dummyref"));
        try {
            p.setIncludesfile(new File("/a"));
            fail("Can set includesfile in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            p.setExcludes("**/*.java");
            fail("Can set excludes in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            p.setExcludesfile(new File("/a"));
            fail("Can set excludesfile in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            p.createInclude();
            fail("Can add nested include in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            p.createExclude();
            fail("Can add nested exclude in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            p.createIncludesFile();
            fail("Can add nested includesfile in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
        try {
            p.createExcludesFile();
            fail("Can add nested excludesfile in PatternSet that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify nested elements when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {
        PatternSet p = new PatternSet();
        project.addReference("dummy", p);
        p.setRefid(new Reference("dummy"));
        try {
            p.getIncludePatterns(project);
            fail("Can make PatternSet a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            p.getExcludePatterns(project);
            fail("Can make PatternSet a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        PatternSet p1 = new PatternSet();
        project.addReference("dummy1", p1);
        p1.setRefid(new Reference("dummy2"));
        PatternSet p2 = new PatternSet();
        project.addReference("dummy2", p2);
        p2.setRefid(new Reference("dummy3"));
        PatternSet p3 = new PatternSet();
        project.addReference("dummy3", p3);
        p3.setRefid(new Reference("dummy1"));
        try {
            p1.getIncludePatterns(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }
        try {
            p1.getExcludePatterns(project);
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3
        // (which holds patterns "include" and "exclude")
        p1 = new PatternSet();
        project.addReference("dummy1", p1);
        p1.setRefid(new Reference("dummy2"));
        p2 = new PatternSet();
        project.addReference("dummy2", p2);
        p2.setRefid(new Reference("dummy3"));
        p3 = new PatternSet();
        project.addReference("dummy3", p3);
        p3.setIncludes("include");
        p3.createExclude().setName("exclude");
        String[] i = p1.getIncludePatterns(project);
        assertEquals("One include pattern buried deep inside a nested patternset structure",
                     1, i.length);
        assertEquals("include", i[0]);
        i = p3.getExcludePatterns(project);
        assertEquals("One exclude pattern buried deep inside a nested patternset structure",
                     1, i.length);
        assertEquals("exclude", i[0]);
    }

    public void testNestedPatternset() {
        PatternSet p = new PatternSet();
        p.setIncludes("**/*.java");

        PatternSet nested = new PatternSet();
        nested.setExcludes("**/*.class");

        p.addConfiguredPatternset(nested);

        String[] excludes = p.getExcludePatterns(project);
        String[] includes = p.getIncludePatterns(project);

        assertEquals("Includes","**/*.java", includes[0]);
        assertEquals("Excludes","**/*.class", excludes[0]);
    }
}
