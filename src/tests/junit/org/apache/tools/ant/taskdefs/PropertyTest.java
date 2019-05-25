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

package org.apache.tools.ant.taskdefs;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeNoException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 */
public class PropertyTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/property.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void test1() {
        // should get no output at all
        buildRule.executeTarget("test1");
        assertEquals("System output should have been empty", "", buildRule.getOutput());
        assertEquals("System error should have been empty", "", buildRule.getError());
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        assertThat(buildRule.getLog(), containsString("testprop1=aa, testprop3=xxyy, testprop4=aazz"));
    }

    /**
     * Fail due to circular definition
     */
    @Test
    public void test3() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("was circularly defined");
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        assertThat(buildRule.getLog(), containsString("http.url is http://localhost:999"));
    }

    @Test
    public void test5() {
        String baseDir = buildRule.getProject().getProperty(MagicNames.PROJECT_BASEDIR);
        String uri = FileUtils.getFileUtils().toURI(baseDir + "/property3.properties");
        buildRule.getProject().setNewProperty("test5.url", uri);

        buildRule.executeTarget("test5");
        assertThat(buildRule.getLog(), containsString("http.url is http://localhost:999"));
    }

    @Test
    public void testPrefixSuccess() {
        buildRule.executeTarget("prefix.success");
        assertEquals("80", buildRule.getProject().getProperty("server1.http.port"));
    }

    /**
     * Fail due to prefix allowed only non-resource/file load
     */
    @Test
    public void testPrefixFailure() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Prefix is only valid");
        buildRule.executeTarget("prefix.fail");
    }

    /**
     * Fail due to circular definition
     */
    @Test
    public void testCircularReference() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("was circularly defined");
        buildRule.executeTarget("testCircularReference");
    }

    @Test
    public void testThisIsNotACircularReference() {
        buildRule.executeTarget("thisIsNotACircularReference");
        assertThat(buildRule.getLog(), containsString("b is A/A/A"));
    }

    @Test
    public void testXmlProperty() {
        try {
            Class.forName("java.lang.Iterable");
        } catch (ClassNotFoundException e) {
            assumeNoException("XML Loading only on Java 5+", e);
        }
        buildRule.executeTarget("testXmlProperty");
        assertEquals("ONE", buildRule.getProject().getProperty("xml.one"));
        assertEquals("TWO", buildRule.getProject().getProperty("xml.two"));
    }

    @Test
    public void testRuntime() {
        // should get no output at all
        buildRule.executeTarget("testRuntime");
        assertEquals(Runtime.getRuntime().availableProcessors(),
                     Integer.parseInt(buildRule.getProject().getProperty("testruntime.availableProcessors")));
        assertEquals(Runtime.getRuntime().maxMemory(),
                     Long.parseLong(buildRule.getProject().getProperty("testruntime.maxMemory")));
        assertNotNull(buildRule.getProject().getProperty("testruntime.freeMemory"));
        assertNotNull(buildRule.getProject().getProperty("testruntime.totalMemory"));
    }

}
