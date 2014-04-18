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

package org.apache.tools.ant.taskdefs.optional;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the EchoProperties task.
 *
 * @created   17-Jan-2002
 * @since     Ant 1.5
 */
public class EchoPropertiesTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";
    private static final String GOOD_OUTFILE = "test.properties";
    private static final String GOOD_OUTFILE_XML = "test.xml";
    private static final String PREFIX_OUTFILE = "test-prefix.properties";
    private static final String TEST_VALUE = "isSet";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();


    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "echoproperties.xml");
        buildRule.getProject().setProperty("test.property", TEST_VALUE);
    }


    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }


    @Test
    public void testEchoToLog() {
    	buildRule.executeTarget("testEchoToLog");
    	assertContains("test.property=" + TEST_VALUE, buildRule.getLog());
    }

    @Test
    public void testEchoWithEmptyPrefixToLog() {
    	buildRule.executeTarget("testEchoWithEmptyPrefixToLog");
    	assertContains("test.property="+TEST_VALUE, buildRule.getLog());
    }


    @Test
    public void testReadBadFile() {
    	try {
    		buildRule.executeTarget("testReadBadFile");
    		fail("BuildException should have been thrown on bad file");
    	}
    	catch(BuildException ex) {
    		assertContains("srcfile is a directory","srcfile is a directory!", ex.getMessage());
    	}
    }

    @Test
    public void testReadBadFileNoFail() {
        buildRule.executeTarget("testReadBadFileNoFail");
        assertContains("srcfile is a directory!", buildRule.getLog());
    }


    @Test
    public void testEchoToBadFile() {
    	try {
    		buildRule.executeTarget("testEchoToBadFile");
            fail("BuildException should have been thrown on destination file being a directory");
    	} catch(BuildException ex) {
    		assertContains("destfile is a directory", "destfile is a directory!", ex.getMessage());
    	}
    }


    @Test
    public void testEchoToBadFileNoFail() {
    	buildRule.executeTarget("testEchoToBadFileNoFail");
    	assertContains("destfile is a directory!", buildRule.getLog());
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
        File f = createRelativeFile(GOOD_OUTFILE_XML);
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        try {
            String read = null;
            while ((read = br.readLine()) != null) {
                if (read.indexOf("<property name=\"test.property\" value=\""+TEST_VALUE+"\" />") >= 0) {
                    // found the property we set - it's good.
                    return;
                }
            }
            fail("did not encounter set property in generated file.");
        } finally {
            try {
                fr.close();
            } catch(IOException e) {}
            try {
                br.close();
            } catch(IOException e) {}
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
    public void testWithPrefixAndRegex() throws Exception {
    	try {
    		buildRule.executeTarget("testWithPrefixAndRegex");
    		fail("BuildException should have been thrown on Prefix and RegEx beng set");
    	} catch (BuildException ex) {
    		assertEquals("The target must fail with prefix and regex attributes set", "Please specify either prefix or regex, but not both", ex.getMessage());
    	}
    }

    @Test
    public void testWithEmptyPrefixAndRegex() throws Exception {
    	buildRule.executeTarget("testEchoWithEmptyPrefixToLog");
    	assertContains("test.property="+TEST_VALUE, buildRule.getLog());
    }

    @Test
    public void testWithRegex() throws Exception {
        assumeTrue("Test skipped because no regexp matcher is present.", RegexpMatcherFactory.regexpMatcherPresent(buildRule.getProject()));
        buildRule.executeTarget("testWithRegex");
        // the following line has been changed from checking ant.home to ant.version so the test will still work when run outside of an ant script
        assertContains("ant.version=", buildRule.getFullLog());
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
        File f = createRelativeFile(relativeFilename);
        Properties props=new Properties();
        InputStream in=null;
        try  {
            in=new BufferedInputStream(new FileInputStream(f));
            props.load(in);
        } finally {
            if(in!=null) {
                try { in.close(); } catch(IOException e) {}
            }
        }
        return props;
    }

    protected void assertGoodFile() throws Exception {
        File f = createRelativeFile(GOOD_OUTFILE);
        assertTrue("Did not create "+f.getAbsolutePath(),
            f.exists());
        Properties props=loadPropFile(GOOD_OUTFILE);
        props.list(System.out);
        assertEquals("test property not found ",
                     TEST_VALUE, props.getProperty("test.property"));
    }


    protected String toAbsolute(String filename) {
        return createRelativeFile(filename).getAbsolutePath();
    }


    protected File createRelativeFile(String filename) {
        if (filename.equals(".")) {
            return buildRule.getProject().getBaseDir();
        }
        // else
        return new File(buildRule.getProject().getBaseDir(), filename);
    }
}

