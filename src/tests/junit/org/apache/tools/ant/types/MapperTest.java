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

import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.ChainedMapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FlatFileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.MergingMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * JUnit testcases for org.apache.tools.ant.types.Mapper.
 *
 */

public class MapperTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Project project;

    private Mapper m;

    @Before
    public void setUp() {
        project = new Project();
        project.setBasedir(".");
        m = new Mapper(project);
    }

    @Test
    public void testEmptyElementIfIsReference1() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        m.setFrom("*.java");
        m.setRefid(new Reference(project, "dummy"));
    }

    @Test
    public void testEmptyElementIfIsReference2() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        m.setRefid(new Reference(project, "dummy"));
        m.setFrom("*.java");
    }

    @Test
    public void testEmptyElementIfIsReference3() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        m.setRefid(new Reference(project, "dummy"));
        m.setTo("*.java");
    }

    @Test
    public void testEmptyElementIfIsReference4() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify more than one attribute when using refid");
        m.setRefid(new Reference(project, "dummy"));
        Mapper.MapperType mt = new Mapper.MapperType();
        mt.setValue("glob");
        m.setType(mt);
    }

    @Test
    public void testCircularReferenceCheck1() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", m);
        m.setRefid(new Reference(project, "dummy"));
        m.getImplementation();
    }

    @Test
    public void testCircularReferenceCheck2() {
        // dummy --> dummy2 --> dummy3 --> dummy
        thrown.expect(BuildException.class);
        thrown.expectMessage("This data type contains a circular reference.");
        project.addReference("dummy", m);
        m.setRefid(new Reference(project, "dummy2"));
        Mapper m2 = new Mapper(project);
        project.addReference("dummy2", m2);
        m2.setRefid(new Reference(project, "dummy3"));
        Mapper m3 = new Mapper(project);
        project.addReference("dummy3", m3);
        m3.setRefid(new Reference(project, "dummy"));
        m.getImplementation();
    }

    @Test
    public void testCircularReferenceCheck3() {
        // dummy --> dummy2 --> dummy3
        // (which holds a glob mapper from "*.java" to "*.class"
        project.addReference("dummy", m);
        m.setRefid(new Reference(project, "dummy2"));
        Mapper m2 = new Mapper(project);
        project.addReference("dummy2", m2);
        m2.setRefid(new Reference(project, "dummy3"));
        Mapper m3 = new Mapper(project);
        project.addReference("dummy3", m3);
        Mapper.MapperType mt = new Mapper.MapperType();
        mt.setValue("glob");
        m3.setType(mt);
        m3.setFrom("*.java");
        m3.setTo("*.class");
        FileNameMapper fmm = m.getImplementation();
        assertThat("should be glob", fmm, instanceOf(GlobPatternMapper.class));
        String[] result = fmm.mapFileName("a.java");
        assertEquals("a.java should match", 1, result.length);
        assertEquals("a.class", result[0]);
    }

    @Test
    public void testNested() {
        Mapper mapper1 = new Mapper(project);
        Mapper.MapperType mt = new Mapper.MapperType();
        mt.setValue("glob");
        mapper1.setType(mt);
        mapper1.setFrom("from*");
        mapper1.setTo("to*");

        //mix element types
        FileNameMapper mapper2 = new FlatFileNameMapper();
        FileNameMapper mapper3 = new MergingMapper();
        mapper3.setTo("mergefile");

        // m is implicit composite
        m.addConfiguredMapper(mapper1);
        m.add(mapper2);
        m.add(mapper3);

        FileNameMapper fileNameMapper = m.getImplementation();
        String[] targets = fileNameMapper.mapFileName("fromfilename");
        assertNotNull("no filenames mapped", targets);
        assertEquals("wrong number of filenames mapped", 3, targets.length);
        List<String> list = Arrays.asList(targets);
        assertThat("cannot find expected target \"tofilename\"", list, hasItem("tofilename"));
        assertThat("cannot find expected target \"fromfilename\"", list, hasItem("fromfilename"));
        assertThat("cannot find expected target \"mergefile\"", list, hasItem("mergefile"));
    }

    /**
     * <pre>
     * a --> b --> c --- def
     *              \-- ghi
     * </pre>
     */
    @Test
    public void testChained() {
        FileNameMapper mapperAB = new GlobPatternMapper();
        mapperAB.setFrom("a");
        mapperAB.setTo("b");

        FileNameMapper mapperBC = new GlobPatternMapper();
        mapperBC.setFrom("b");
        mapperBC.setTo("c");

        FileNameMapper mapperDEF = new GlobPatternMapper();
        mapperDEF.setFrom("c");
        mapperDEF.setTo("def");

        FileNameMapper mapperGHI = new GlobPatternMapper();
        mapperGHI.setFrom("c");
        mapperGHI.setTo("ghi");

        // m is implicit composite
        m.add(mapperDEF);
        m.add(mapperGHI);

        Mapper chained = new Mapper(project);
        chained.setClassname(ChainedMapper.class.getName());
        chained.add(mapperAB);
        chained.add(mapperBC);
        chained.addConfiguredMapper(m);

        FileNameMapper fileNameMapper = chained.getImplementation();
        String[] targets = fileNameMapper.mapFileName("a");
        assertNotNull("no filenames mapped", targets);
        assertEquals("wrong number of filenames mapped", 2, targets.length);
        List<String> list = Arrays.asList(targets);
        assertThat("cannot find expected target \"def\"", list, hasItem("def"));
        assertThat("cannot find expected target \"ghi\"", list, hasItem("ghi"));

        targets = fileNameMapper.mapFileName("z");
        assertNull(targets);
    }

    @Test
    public void testCopyTaskWithTwoFilesets() {
        buildRule.configureProject("src/etc/testcases/types/mapper.xml");
        buildRule.executeTarget("test1");
    }

}
