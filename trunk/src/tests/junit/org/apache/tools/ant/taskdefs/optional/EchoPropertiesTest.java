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

import org.apache.tools.ant.BuildFileTest;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Properties;

/**
 * Tests the EchoProperties task.
 *
 * @created   17-Jan-2002
 * @since     Ant 1.5
 */
public class EchoPropertiesTest extends BuildFileTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";
    private static final String GOOD_OUTFILE = "test.properties";
    private static final String GOOD_OUTFILE_XML = "test.xml";
    private static final String PREFIX_OUTFILE = "test-prefix.properties";
    private static final String TEST_VALUE = "isSet";
    private static final String BAD_OUTFILE = ".";

    public EchoPropertiesTest(String name) {
        super(name);
    }


    public void setUp() {
        configureProject(TASKDEFS_DIR + "echoproperties.xml");
        project.setProperty( "test.property", TEST_VALUE );
    }


    public void tearDown() {
        executeTarget("cleanup");
    }


    public void testEchoToLog() {
        expectLogContaining("testEchoToLog", "test.property="+TEST_VALUE);
    }

    public void testEchoWithEmptyPrefixToLog() {
        expectLogContaining("testEchoWithEmptyPrefixToLog", "test.property="+TEST_VALUE);
    }


    public void testReadBadFile() {
        expectBuildExceptionContaining( "testReadBadFile",
            "srcfile is a directory", "srcfile is a directory!" );
    }


    public void testReadBadFileFail() {
        expectBuildExceptionContaining( "testReadBadFile",
            "srcfile is a directory", "srcfile is a directory!" );
    }


    public void testReadBadFileNoFail() {
        expectLog( "testReadBadFileNoFail", "srcfile is a directory!" );
    }


    public void testEchoToBadFile() {
        expectBuildExceptionContaining( "testEchoToBadFile",
            "destfile is a directory", "destfile is a directory!" );
    }


    public void testEchoToBadFileFail() {
        expectBuildExceptionContaining( "testEchoToBadFileFail",
            "destfile is a directory", "destfile is a directory!" );
    }


    public void testEchoToBadFileNoFail() {
        expectLog( "testEchoToBadFileNoFail", "destfile is a directory!");
    }


    public void testEchoToGoodFile() throws Exception {
        executeTarget( "testEchoToGoodFile" );
        assertGoodFile();
    }


    public void testEchoToGoodFileXml() throws Exception {
        executeTarget( "testEchoToGoodFileXml" );

        // read in the file
        File f = createRelativeFile( GOOD_OUTFILE_XML );
        FileReader fr = new FileReader( f );
        try {
            BufferedReader br = new BufferedReader( fr );
            String read = null;
            while ( (read = br.readLine()) != null) {
                if (read.indexOf("<property name=\"test.property\" value=\""+TEST_VALUE+"\" />") >= 0) {
                    // found the property we set - it's good.
                    return;
                }
            }
            fail( "did not encounter set property in generated file." );
        } finally {
            try {
                fr.close();
            } catch(IOException e) {}
        }
    }


    public void testEchoToGoodFileFail() throws Exception {
        executeTarget( "testEchoToGoodFileFail" );
        assertGoodFile();
    }


    public void testEchoToGoodFileNoFail() throws Exception {
        executeTarget( "testEchoToGoodFileNoFail" );
        assertGoodFile();
    }


    public void testEchoPrefix() throws Exception {
        testEchoPrefixVarious("testEchoPrefix");
    }

    public void testEchoPrefixAsPropertyset() throws Exception {
        testEchoPrefixVarious("testEchoPrefixAsPropertyset");
    }

    public void testEchoPrefixAsNegatedPropertyset() throws Exception {
        testEchoPrefixVarious("testEchoPrefixAsNegatedPropertyset");
    }

    public void testEchoPrefixAsDoublyNegatedPropertyset() throws Exception {
        testEchoPrefixVarious("testEchoPrefixAsDoublyNegatedPropertyset");
    }

    public void testWithPrefixAndRegex() throws Exception {
        expectSpecificBuildException("testWithPrefixAndRegex",
                "The target must fail with prefix and regex attributes set",
                "Please specify either prefix or regex, but not both");
    }

    public void testWithEmptyPrefixAndRegex() throws Exception {
        expectLogContaining("testEchoWithEmptyPrefixToLog", "test.property="+TEST_VALUE);
    }

    public void testWithRegex() throws Exception {
        executeTarget("testWithRegex");
        assertDebuglogContaining("ant.home=");
    }

    private void testEchoPrefixVarious(String target) throws Exception {
        executeTarget(target);
        Properties props = loadPropFile(PREFIX_OUTFILE);
        assertEquals("prefix didn't include 'a.set' property",
            "true", props.getProperty("a.set"));
        assertNull("prefix failed to filter out property 'b.set'",
            props.getProperty("b.set"));
    }

    protected Properties loadPropFile(String relativeFilename)
            throws IOException {
        File f = createRelativeFile( relativeFilename );
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
        File f = createRelativeFile( GOOD_OUTFILE );
        assertTrue(
            "Did not create "+f.getAbsolutePath(),
            f.exists() );
        Properties props=loadPropFile(GOOD_OUTFILE);
        props.list(System.out);
        assertEquals("test property not found ",
                     TEST_VALUE, props.getProperty("test.property"));
/*
        // read in the file
        FileReader fr = new FileReader( f );
        try {
            BufferedReader br = new BufferedReader( fr );
            String read = null;
            while ( (read = br.readLine()) != null)
            {
                if (read.indexOf("test.property" + TEST_VALUE) >= 0)
                {
                    // found the property we set - it's good.
                    return;
                }
            }
            fail( "did not encounter set property in generated file." );
        } finally {
            try { fr.close(); } catch(IOException e) {}
        }
*/
    }


    protected String toAbsolute( String filename ) {
        return createRelativeFile( filename ).getAbsolutePath();
    }


    protected File createRelativeFile( String filename ) {
        if (filename.equals( "." )) {
            return getProjectDir();
        }
        // else
        return new File( getProjectDir(), filename );
    }
}

