/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs;

import java.io.*;
import org.apache.tools.ant.*;

/**
 * Tests CVSLogin task.
 *
 * @author <a href="mailto:jeff@custommonkey.org">Jeff Martin</a>
 */
public class CVSPassTest extends TaskdefsTest { 
    private final String EOL = System.getProperty("line.separator");
    private final String JAKARTA_URL =
        ":pserver:anoncvs@jakarta.apache.org:/home/cvspublic Ay=0=h<Z";
    private final String XML_URL =
        ":pserver:anoncvs@xml.apache.org:/home/cvspublic Ay=0=h<Z";
    private final String TIGRIS_URL =
        ":pserver:guest@cvs.tigris.org:/cvs AIbdZ,";
    
    
    public CVSPassTest(String name) { 
        super(name);
    }

    public void setUp() { 
        configureProject("src/etc/testcases/taskdefs/cvspass.xml");
    }

    public void testNoCVSRoot() { 
        try{
            executeTarget("test1");
            fail("BuildException not thrown");
        }catch(BuildException e){
            assertEquals("cvsroot is required", e.getMessage());
        }
    }

    public void testNoPassword() { 
        try{
            executeTarget("test2");
            fail("BuildException not thrown");
        }catch(BuildException e){
            assertEquals("password is required", e.getMessage());
        }
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testPassFile() throws Exception { 
        executeTarget("test3");
        File f = new File(getProjectDir(), "testpassfile.tmp");

        assertTrue( "Passfile "+f+" not created", f.exists());

        assertEquals(JAKARTA_URL+EOL, readFile(f));

    }

    public void testPassFileDuplicateEntry() throws Exception { 
        executeTarget("test4");
        File f = new File(getProjectDir(), "testpassfile.tmp");

        assertTrue( "Passfile "+f+" not created", f.exists());

        assertEquals(
            JAKARTA_URL+ EOL+
            TIGRIS_URL+ EOL,
            readFile(f));
    }

    public void testPassFileMultipleEntry() throws Exception { 
        executeTarget("test5");
        File f = new File(getProjectDir(), "testpassfile.tmp");

        assertTrue( "Passfile "+f+" not created", f.exists());

        assertEquals(
            JAKARTA_URL+ EOL+
            XML_URL+ EOL+
            TIGRIS_URL+ EOL,
            readFile(f));
    }

    private String readFile(File f) throws Exception {
        BufferedReader reader = null; 
        
        try {
            reader = new BufferedReader(new FileReader(f));

            StringBuffer buf = new StringBuffer();
            String line=null;
            while((line=reader.readLine())!=null){
                buf.append(line + EOL);
            }
            return buf.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
