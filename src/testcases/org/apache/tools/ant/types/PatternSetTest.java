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

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.io.File;

/**
 * JUnit 3 testcases for org.apache.tools.ant.types.PatternSet.
 *
 * <p>This doesn't actually test much, mainly reference handling.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
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
