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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.ChainedMapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FlatFileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.MergingMapper;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.Mapper.
 *
 */

public class MapperTest extends TestCase {

    private Project project;

    public MapperTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.setBasedir(".");
    }

    public void testEmptyElementIfIsReference() {
        Mapper m = new Mapper(project);
        m.setFrom("*.java");
        try {
            m.setRefid(new Reference("dummyref"));
            fail("Can add reference to Mapper with from attribute set");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        m = new Mapper(project);
        m.setRefid(new Reference("dummyref"));
        try {
            m.setFrom("*.java");
            fail("Can set from in Mapper that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }

        m = new Mapper(project);
        m.setRefid(new Reference("dummyref"));
        try {
            m.setTo("*.java");
            fail("Can set to in Mapper that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
        try {
            Mapper.MapperType mt = new Mapper.MapperType();
            mt.setValue("glob");
            m.setType(mt);
            fail("Can set type in Mapper that is a reference.");
        } catch (BuildException be) {
            assertEquals("You must not specify more than one attribute when using refid",
                         be.getMessage());
        }
    }

    public void testCircularReferenceCheck() {
        Mapper m = new Mapper(project);
        project.addReference("dummy", m);
        m.setRefid(new Reference("dummy"));
        try {
            m.getImplementation();
            fail("Can make Mapper a Reference to itself.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3 --> dummy1
        Mapper m1 = new Mapper(project);
        project.addReference("dummy1", m1);
        m1.setRefid(new Reference("dummy2"));
        Mapper m2 = new Mapper(project);
        project.addReference("dummy2", m2);
        m2.setRefid(new Reference("dummy3"));
        Mapper m3 = new Mapper(project);
        project.addReference("dummy3", m3);
        m3.setRefid(new Reference("dummy1"));
        try {
            m1.getImplementation();
            fail("Can make circular reference.");
        } catch (BuildException be) {
            assertEquals("This data type contains a circular reference.",
                         be.getMessage());
        }

        // dummy1 --> dummy2 --> dummy3
        // (which holds a glob mapper from "*.java" to "*.class"
        m1 = new Mapper(project);
        project.addReference("dummy1", m1);
        m1.setRefid(new Reference("dummy2"));
        m2 = new Mapper(project);
        project.addReference("dummy2", m2);
        m2.setRefid(new Reference("dummy3"));
        m3 = new Mapper(project);
        project.addReference("dummy3", m3);
        Mapper.MapperType mt = new Mapper.MapperType();
        mt.setValue("glob");
        m3.setType(mt);
        m3.setFrom("*.java");
        m3.setTo("*.class");
        FileNameMapper fmm = m1.getImplementation();
        assertTrue("should be glob", fmm instanceof GlobPatternMapper);
        String[] result = fmm.mapFileName("a.java");
        assertEquals("a.java should match", 1, result.length);
        assertEquals("a.class", result[0]);
    }

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

        Mapper container = new Mapper(project);
        container.addConfiguredMapper(mapper1);
        container.add(mapper2);
        container.add(mapper3);

        FileNameMapper fileNameMapper = container.getImplementation();
        String[] targets = fileNameMapper.mapFileName("fromfilename");
        assertNotNull("no filenames mapped", targets);
        assertEquals("wrong number of filenames mapped", 3, targets.length);
        List list = Arrays.asList(targets);
        assertTrue("cannot find expected target \"tofilename\"",
            list.contains("tofilename"));
        assertTrue("cannot find expected target \"fromfilename\"",
            list.contains("fromfilename"));
        assertTrue("cannot find expected target \"mergefile\"",
            list.contains("mergefile"));
    }

    public void testChained() {

        // a --> b --> c --- def
        //               \-- ghi

        FileNameMapper mapperAB = new GlobPatternMapper();
        mapperAB.setFrom("a");
        mapperAB.setTo("b");

        FileNameMapper mapperBC = new GlobPatternMapper();
        mapperBC.setFrom("b");
        mapperBC.setTo("c");

        //implicit composite
        Mapper mapperCX = new Mapper(project);

        FileNameMapper mapperDEF = new GlobPatternMapper();
        mapperDEF.setFrom("c");
        mapperDEF.setTo("def");

        FileNameMapper mapperGHI = new GlobPatternMapper();
        mapperGHI.setFrom("c");
        mapperGHI.setTo("ghi");

        mapperCX.add(mapperDEF);
        mapperCX.add(mapperGHI);

        Mapper chained = new Mapper(project);
        chained.setClassname(ChainedMapper.class.getName());
        chained.add(mapperAB);
        chained.add(mapperBC);
        chained.addConfiguredMapper(mapperCX);

        FileNameMapper fileNameMapper = chained.getImplementation();
        String[] targets = fileNameMapper.mapFileName("a");
        assertNotNull("no filenames mapped", targets);
        assertEquals("wrong number of filenames mapped", 2, targets.length);
        List list = Arrays.asList(targets);
        assertTrue("cannot find expected target \"def\"", list.contains("def"));
        assertTrue("cannot find expected target \"ghi\"", list.contains("ghi"));
    }

    public void testCopyTaskWithTwoFilesets() {
        TaskdefForCopyTest t = new TaskdefForCopyTest("test1");
        try {
            t.setUp();
            t.test1();
        } finally {
            t.tearDown();
        }
    }

    private class TaskdefForCopyTest extends BuildFileTest {
        TaskdefForCopyTest(String name) {
            super(name);
        }

        public void setUp() {
            configureProject("src/etc/testcases/types/mapper.xml");
        }

        public void tearDown() {
            executeTarget("cleanup");
        }

        public void test1() {
            executeTarget("test1");
        }
    }
}
