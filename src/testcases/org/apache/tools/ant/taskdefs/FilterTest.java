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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.tools.ant.BuildFileTest;

/**
 * @author Nico Seessle <nico@seessle.de> 
 */
public class FilterTest extends BuildFileTest { 
      
    public FilterTest(String name) { 
        super(name);
    }    
    
    public void setUp() { 
        configureProject("src/etc/testcases/taskdefs/filter.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }
    
    public void test1() { 
        expectBuildException("test1", "required argument missing");
    }

    public void test2() { 
        expectBuildException("test2", "required argument missing");
    }

    public void test3() { 
        expectBuildException("test3", "required argument missing");
    }
    
    public void test4() { 
        executeTarget("test4");
    }
    
    public void test5() {
        executeTarget("test5");
        assertEquals("2000",
                     getFilteredFile("5", "filtered.tmp"));
    }
    

    public void test6() {
        executeTarget("test6");
        assertEquals("2000",
                     getFilteredFile("6", "taskdefs.tmp/filter1.txt"));
    }

    public void test7() {
        executeTarget("test7");
        assertEquals("<%@ include file=\"root/some/include.jsp\"%>",
                     getFilteredFile("7", "filtered.tmp"));
    }

    public void test8() {
        executeTarget("test8");
        assertEquals("<%@ include file=\"root/some/include.jsp\"%>",
                     getFilteredFile("8", "taskdefs.tmp/filter2.txt"));
    }
    
    public void test9() {
        executeTarget("test9");
        assertEquals("included",
                    getFilteredFile("9", "taskdefs.tmp/filter3.txt"));
    }
        
    private String getFilteredFile(String testNumber, String filteredFile) {
    
        String line = null;
        File f = new File(getProjectDir(), filteredFile);
        if (!f.exists()) {
            fail("filter test"+testNumber+" failed");
        } else {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(f));
            } catch (FileNotFoundException fnfe) {
                fail("filter test"+testNumber+" failed, filtered file: " + f.toString() + " not found");
            }
            try {
                line = in.readLine();
                in.close();
            } catch (IOException ioe) {
                fail("filter test"+testNumber+" failed.  IOException while reading filtered file: " + ioe);
            }
        }
        f.delete();
        return line;
    }
}
