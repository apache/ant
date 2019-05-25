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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * JUnit 4 testcases for org.apache.tools.ant.types.PatternSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.</p>
 */

public class PatternSetTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Project project;

    private PatternSet p;

    @Before
    public void setUp() {
        project = new Project();
        project.setBasedir(".");
        p = new PatternSet();
    }

    @Test
    public void testEmptyElementSetIncludesThenRefid() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setIncludes("**/*.java");
        p.setRefid(new Reference(project, "dummyref"));
    }

    @Test
    public void testEmptyElementSetRefidThenIncludes() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.setIncludes("**/*.java");
    }

    @Test
    public void testEmptyElementSetRefidThenIncludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setRefid(new Reference(project, "dummyref"));
            p.setIncludesfile(new File("/a"));
    }

    @Test
    public void testEmptyElementSetRefidThenExclude() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.setExcludes("**/*.java");
    }

    @Test
    public void testEmptyElementSetRefidThenExcludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.setExcludesfile(new File("/a"));
    }

    @Test
    public void testEmptyElementSetRefidThenAddInclude() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.createInclude();
    }

    @Test
    public void testEmptyElementSetRefidThenAddExclude() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.createExclude();
    }

    @Test
    public void testEmptyElementSetRefidThenAddIncludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.createIncludesFile();
    }

    @Test
    public void testEmptyElementSetRefidThenAddExcludesfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify nested elements when using refid");
        p.setRefid(new Reference(project, "dummyref"));
        p.createExcludesFile();
    }

    @Test
    public void testCircularReferenceCheckIncludePaterns() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", p);
        p.setRefid(new Reference(project, "dummy"));
        p.getIncludePatterns(project);
    }

    @Test
    public void testCircularReferenceCheckExcludePatterns() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", p);
        p.setRefid(new Reference(project, "dummy"));
        p.getExcludePatterns(project);
    }

    @Test
    public void testLoopReferenceCheckIncludePaterns() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // dummy --> dummyA --> dummyB --> dummy
        project.addReference("dummy", p);
        p.setRefid(new Reference(project, "dummyA"));
        PatternSet pa = new PatternSet();
        project.addReference("dummyA", pa);
        pa.setRefid(new Reference(project, "dummyB"));
        PatternSet pb = new PatternSet();
        project.addReference("dummyB", pb);
        pb.setRefid(new Reference(project, "dummy"));
        p.getIncludePatterns(project);
    }

    @Test
    public void testLoopReferenceCheckExcludePaterns() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        // dummy --> dummyA --> dummyB --> dummy
        project.addReference("dummy", p);
        p.setRefid(new Reference(project, "dummyA"));
        PatternSet pa = new PatternSet();
        project.addReference("dummyA", pa);
        pa.setRefid(new Reference(project, "dummyB"));
        PatternSet pb = new PatternSet();
        project.addReference("dummyB", pb);
        pb.setRefid(new Reference(project, "dummy"));
        p.getExcludePatterns(project);
    }

    @Test
    public void testLoopReferenceCheck() {
        // dummy --> dummyA --> dummyB
        // (which holds patterns "include" and "exclude")
        project.addReference("dummy", p);
        p.setRefid(new Reference(project, "dummyA"));
        PatternSet pa = new PatternSet();
        project.addReference("dummyA", pa);
        pa.setRefid(new Reference(project, "dummyB"));
        PatternSet pb = new PatternSet();
        project.addReference("dummyB", pb);
        pb.setIncludes("include");
        pb.createExclude().setName("exclude");
        String[] i = p.getIncludePatterns(project);
        assertEquals("One include pattern buried deep inside a nested patternset structure",
                     1, i.length);
        assertEquals("include", i[0]);
        i = pb.getExcludePatterns(project);
        assertEquals("One exclude pattern buried deep inside a nested patternset structure",
                     1, i.length);
        assertEquals("exclude", i[0]);
    }

    @Test
    public void testNestedPatternset() {
        p.setIncludes("**/*.java");

        PatternSet nested = new PatternSet();
        nested.setExcludes("**/*.class");

        p.addConfiguredPatternset(nested);

        String[] excludes = p.getExcludePatterns(project);
        String[] includes = p.getIncludePatterns(project);

        assertEquals("Includes", "**/*.java", includes[0]);
        assertEquals("Excludes", "**/*.class", excludes[0]);
    }

    @Test
    public void testEncodingOfIncludesFile() throws IOException {
        File testFile = testFolder.newFile("ant.pattern");
        Charset cs = StandardCharsets.UTF_16LE;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(testFile), cs)) {
            w.write("\u00e4\n");
        }
        PatternSet.PatternFileNameEntry ne =
                (PatternSet.PatternFileNameEntry) p.createIncludesFile();
        ne.setName(testFile.getAbsolutePath());
        ne.setEncoding(cs.name());
        assertArrayEquals(new String[] {"\u00e4"}, p.getIncludePatterns(project));
    }
}
