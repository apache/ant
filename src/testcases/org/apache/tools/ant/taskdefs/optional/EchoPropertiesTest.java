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

package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;

import java.io.*;
import java.util.Properties;

/**
 * Tests the EchoProperties task.
 *
 * @author    Matt Albrecht <a href="mailto:groboclown@users.sourceforge.net">groboclown@users.sourceforge.net</a>
 * @created   17-Jan-2002
 * @since     Ant 1.5
 */
public class EchoPropertiesTest extends BuildFileTest {

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/";
    private static final String GOOD_OUTFILE = "test.properties";
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
    
    
    public void testEchoToBadFile() {
        expectBuildExceptionContaining( "testEchoToBadFile",
            "outfile is not writeable",
            "Destfile "+toAbsolute(BAD_OUTFILE)+" could not be written to." );
    }
    
    
    public void testEchoToBadFileFail() {
        expectBuildExceptionContaining( "testEchoToBadFileFail",
            "outfile is not writeable",
            "Destfile "+toAbsolute(BAD_OUTFILE)+" could not be written to." );
    }
    
    
    public void testEchoToBadFileNoFail() {
        expectLog( "testEchoToBadFileNoFail",
            "Destfile "+toAbsolute(BAD_OUTFILE)+" could not be written to." );
    }
    
    
    public void testEchoToGoodFile() throws Exception {
        executeTarget( "testEchoToGoodFile" );
        assertGoodFile();
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
        executeTarget( "testEchoPrefix" );
        Properties props=loadPropFile(PREFIX_OUTFILE);
//        props.list(System.out);
        assertEquals("prefix didn't include 'a.set' property","true",props.getProperty("a.set"));
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

