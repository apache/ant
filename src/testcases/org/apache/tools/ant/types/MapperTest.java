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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.*;
import org.apache.tools.ant.BuildFileTest;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.File;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.Mapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
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
