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
package org.apache.tools.ant.taskdefs.optional.script;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.JavaVersion;
import org.apache.tools.ant.types.FileSet;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests the examples of the &lt;scriptdef&gt; task.
 *
 * @since Ant 1.6
 */
public class ScriptDefTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/script/scriptdef.xml");
    }

    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
        // get the fileset and its basedir
        Project p = buildRule.getProject();
        FileSet fileset = p.getReference("testfileset");
        File baseDir = fileset.getDir(p);
        String log = buildRule.getLog();
        assertThat("Expecting attribute value printed", log,
                containsString("Attribute attr1 = test"));
        assertThat("Expecting nested element value printed", log,
                containsString("Fileset basedir = " + baseDir.getAbsolutePath()));
    }

    /**
     * Expected failure due to lacking language attribute
     */
    @Test
    public void testNoLang() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("requires a language attribute");
        buildRule.executeTarget("nolang");
    }

    /**
     * Expected failure due to lacking name attribute
     */
    @Test
    public void testNoName() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("scriptdef requires a name attribute");
        buildRule.executeTarget("noname");
    }

    @Test
    public void testNestedByClassName() {
        buildRule.executeTarget("nestedbyclassname");
        // get the fileset and its basedir
        Project p = buildRule.getProject();
        FileSet fileset = p.getReference("testfileset");
        File baseDir = fileset.getDir(p);
        String log = buildRule.getLog();
        assertThat("Expecting attribute value to be printed", log,
                containsString("Attribute attr1 = test"));
        assertThat("Expecting nested element value to be printed", log,
                containsString("Fileset basedir = " + baseDir.getAbsolutePath()));
    }

    @Test
    public void testNoElement() {
        buildRule.executeTarget("noelement");
        assertEquals("Attribute attr1 = test", buildRule.getOutput().trim());
    }

    @Test
    public void testException() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("TypeError");
        buildRule.executeTarget("exception");
    }

    @Test
    public void testDoubleDef() {
        buildRule.executeTarget("doubledef");
        String log = buildRule.getLog();
        assertThat("Task1 did not execute", log, containsString("Task1"));
        assertThat("Task2 did not execute", log, containsString("Task2"));
    }

    /**
     * Expected failure due to duplicate attribute definition
     */
    @Test
    public void testDoubleAttribute() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("attr1 attribute more than once");
        buildRule.executeTarget("doubleAttributeDef");
    }

    @Test
    public void testProperty() {
        buildRule.executeTarget("property");
        assertThat("Expecting property in attribute value replaced",
                buildRule.getLog(), containsString("Attribute value = test"));
    }

    @Test
    public void testUseSrcAndEncoding() {
        final String readerEncoding = "UTF-8";
        buildRule.getProject().setProperty("useSrcAndEncoding.reader.encoding", readerEncoding);
        buildRule.executeTarget("useSrcAndEncoding");
    }

    @Test
    public void testUseSrcAndEncodingFailure() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("expected <eacute [\u00e9]> but was <eacute [\u00c3\u00a9]>");
        final String readerEncoding = "ISO-8859-1";
        buildRule.getProject().setProperty("useSrcAndEncoding.reader.encoding", readerEncoding);
        buildRule.executeTarget("useSrcAndEncoding");
    }

    @Test
    public void testUseCompiled() {
        final JavaVersion atMostJava14 = new JavaVersion();
        atMostJava14.setAtMost("14");
        // skip execution since this compilation timing based test consistently fails starting Java 15 (where we use
        // Graal libraries for Javascript engine)
        Assume.assumeTrue("Skipping test execution since Java version is greater than Java 14", atMostJava14.eval());
        final long duration;
        {
            long start = System.nanoTime();
            buildRule.executeTarget("useCompiled");
            duration = System.nanoTime() - start;
        }

        final long notCompiledDuration;
        {
            long start = System.nanoTime();
            buildRule.executeTarget("useNotCompiled");
            notCompiledDuration = System.nanoTime() - start;
        }

        assertTrue(
            String.format(
                "Compiled scripts should run faster (%d ns) than not compiled (%d ns) scripts.",
                duration, notCompiledDuration
	        ), 
            duration < notCompiledDuration
        );
    }
}
