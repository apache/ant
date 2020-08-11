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

package org.apache.tools.ant.taskdefs.optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests the EchoProperties task.
 *
 * @created   17-Jan-2002
 * @since     Ant 1.5
 */
public class EchoPropertiesTest {

    private static final String GOOD_OUTFILE = "test.properties";
    private static final String GOOD_OUTFILE_XML = "test.xml";
    private static final String PREFIX_OUTFILE = "test-prefix.properties";
    private static final String TEST_VALUE = "isSet";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/echoproperties.xml");
        buildRule.getProject().setProperty("test.property", TEST_VALUE);
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void testEchoToLog() {
        buildRule.executeTarget("testEchoToLog");
        assertThat(buildRule.getLog(), containsString("test.property=" + TEST_VALUE));
    }

    @Test
    public void testEchoWithEmptyPrefixToLog() {
        buildRule.executeTarget("testEchoWithEmptyPrefixToLog");
        assertThat(buildRule.getLog(), containsString("test.property=" + TEST_VALUE));
    }

    @Test
    public void testReadBadFile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("srcfile is a directory!");
        buildRule.executeTarget("testReadBadFile");
    }

    @Test
    public void testReadBadFileNoFail() {
        buildRule.executeTarget("testReadBadFileNoFail");
        assertThat(buildRule.getLog(), containsString("srcfile is a directory!"));
    }

    @Test
    public void testEchoToBadFile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("destfile is a directory!");
        buildRule.executeTarget("testEchoToBadFile");
    }

    @Test
    public void testEchoToBadFileNoFail() {
        buildRule.executeTarget("testEchoToBadFileNoFail");
        assertThat(buildRule.getLog(), containsString("destfile is a directory!"));
    }

    @Test
    public void testEchoToGoodFile() throws Exception {
        buildRule.executeTarget("testEchoToGoodFile");
        assertGoodFile();
    }

    @Test
    public void testEchoToGoodFileXml() throws Exception {
        buildRule.executeTarget("testEchoToGoodFileXml");

        // read in the file
        File f = new File(buildRule.getProject().getBaseDir(), GOOD_OUTFILE_XML);
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            assertTrue("did not encounter set property in generated file.", br.lines().anyMatch(line
                    -> line.contains("<property name=\"test.property\" value=\"" + TEST_VALUE + "\" />")));
        }
    }

    @Test
    public void testEchoToGoodFileFail() throws Exception {
        buildRule.executeTarget("testEchoToGoodFileFail");
        assertGoodFile();
    }

    @Test
    public void testEchoToGoodFileNoFail() throws Exception {
        buildRule.executeTarget("testEchoToGoodFileNoFail");
        assertGoodFile();
    }

    @Test
    public void testEchoPrefix() throws Exception {
        testEchoPrefixVarious("testEchoPrefix");
    }

    @Test
    public void testEchoPrefixAsPropertyset() throws Exception {
        testEchoPrefixVarious("testEchoPrefixAsPropertyset");
    }

    @Test
    public void testEchoPrefixAsNegatedPropertyset() throws Exception {
        testEchoPrefixVarious("testEchoPrefixAsNegatedPropertyset");
    }

    @Test
    public void testEchoPrefixAsDoublyNegatedPropertyset() throws Exception {
        testEchoPrefixVarious("testEchoPrefixAsDoublyNegatedPropertyset");
    }

    @Test
    public void testWithPrefixAndRegex() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Please specify either prefix or regex, but not both");
        buildRule.executeTarget("testWithPrefixAndRegex");
    }

    @Test
    public void testWithEmptyPrefixAndRegex() {
        buildRule.executeTarget("testEchoWithEmptyPrefixToLog");
        assertThat(buildRule.getLog(), containsString("test.property=" + TEST_VALUE));
    }

    @Test
    public void testWithRegex() {
        assumeTrue("Test skipped because no regexp matcher is present.",
                RegexpMatcherFactory.regexpMatcherPresent(buildRule.getProject()));
        buildRule.executeTarget("testWithRegex");
        // the following line has been changed from checking ant.home to ant.version
        // so the test will still work when run outside of an Ant script
        assertThat(buildRule.getFullLog(), containsString(MagicNames.ANT_VERSION + "="));
    }

    @Test
    public void testLocalPropertyset() {
        buildRule.executeTarget("testEchoLocalPropertyset");
        assertThat(buildRule.getLog(), containsString("loc=foo"));
    }

    private void testEchoPrefixVarious(String target) throws Exception {
        buildRule.executeTarget(target);
        Properties props = loadPropFile(PREFIX_OUTFILE);
        assertEquals("prefix didn't include 'a.set' property",
            "true", props.getProperty("a.set"));
        assertNull("prefix failed to filter out property 'b.set'",
            props.getProperty("b.set"));
    }

    protected Properties loadPropFile(String relativeFilename)
            throws IOException {
        assertNotNull("Null property file name", relativeFilename);
        File f = new File(buildRule.getProject().getBaseDir(), relativeFilename);
        assertTrue("Did not create " + f.getAbsolutePath(), f.exists());
        Properties props = new Properties();
        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            props.load(in);
        }
        return props;
    }

    protected void assertGoodFile() throws Exception {
        Properties props = loadPropFile(GOOD_OUTFILE);
        props.list(System.out);
        assertEquals("test property not found ",
                     TEST_VALUE, props.getProperty("test.property"));
    }

}
