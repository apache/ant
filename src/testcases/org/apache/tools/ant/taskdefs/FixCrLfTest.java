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

import junit.framework.AssertionFailedError;

/**
 * @author <a href="mailto:pbwest@powerup.com.au">Peter B. West</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class FixCrLfTest extends TaskdefsTest {

    public FixCrLfTest(String name) {
        super(name);
    }

    public void setUp() { 
        configureProject("src/etc/testcases/taskdefs/fixcrlf/build.xml");
    }
    
    public void tearDown() { 
        executeTarget("cleanup");
    }
    
    public void test1() throws IOException { 
        executeTarget("test1");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk1.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk1.java"));
    }
    
    public void test2() throws IOException { 
        executeTarget("test2");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk2.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk2.java"));
    }
    
    public void test3() throws IOException { 
        executeTarget("test3");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk3.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk3.java"));
    }
    
    public void test4() throws IOException { 
        executeTarget("test4");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk4.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk4.java"));
    }
    
    public void test5() throws IOException { 
        executeTarget("test5");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk5.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk5.java"));
    }
    
    public void test6() throws IOException { 
        executeTarget("test6");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk6.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk6.java"));
    }
    
    public void test7() throws IOException { 
        executeTarget("test7");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk7.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk7.java"));
    }
    
    public void test8() throws IOException {  
        executeTarget("test8");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk8.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk8.java"));
    }
    
    public void test9() throws IOException { 
        executeTarget("test9");
        assertEqualContent(new File("src/etc/testcases/taskdefs/fixcrlf/expected/Junk9.java"),
                           new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk9.java"));
    }
    
    public void testNoOverwrite() throws IOException {
        executeTarget("test1");
        File result = 
            new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk1.java");
        long modTime = result.lastModified();

        /*
         * Sleep for some time to make sure a newer file would get a
         * more recent timestamp according to the file system's
         * granularity (should be > 2s to account for Windows FAT).
         */
        try {
            Thread.currentThread().sleep(5000);
        } catch (InterruptedException ie) {
            fail(ie.getMessage());
        } // end of try-catch

        /* 
         * make sure we get a new Project instance or the target won't get run
         * a second time.
         */
        configureProject("src/etc/testcases/taskdefs/fixcrlf/build.xml");

        executeTarget("test1");
        result = 
            new File("src/etc/testcases/taskdefs/fixcrlf/result/Junk1.java");
        assertEquals(modTime, result.lastModified());
    }

    public void assertEqualContent(File expect, File result) 
        throws AssertionFailedError, IOException {
        if (!result.exists()) {
            fail("Expected file "+result+" doesn\'t exist");
        }

        InputStream inExpect = null;
        InputStream inResult = null;
        try {
            inExpect = new BufferedInputStream(new FileInputStream(expect));
            inResult = new BufferedInputStream(new FileInputStream(result));

            int expectedByte = inExpect.read();
            while (expectedByte != -1) {
                assertEquals(expectedByte, inResult.read());
                expectedByte = inExpect.read();
            }
            assertEquals("End of file", -1, inResult.read());
        } finally {
            if (inResult != null) {
                inResult.close();
            }
            if (inExpect != null) {
                inExpect.close();
            }
        }
    }

}
