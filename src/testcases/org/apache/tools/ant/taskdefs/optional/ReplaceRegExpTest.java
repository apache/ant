/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "Ant" and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * JUnit Testcase for the optional replaceregexp task.
 *
 * @author Stefan Bodewig 
 * @version $Revision$
 */
public class ReplaceRegExpTest extends BuildFileTest {
    private static final String PROJECT_PATH = "src/etc/testcases/taskdefs/optional";
    public ReplaceRegExpTest(String name) {
        super(name);
    }
    
    public void setUp() { 
        configureProject(PROJECT_PATH + "/replaceregexp.xml");
    }
    
    public void tearDown() { 
        executeTarget("cleanup");
    }
    
    public void testReplace() throws IOException {
        Properties original = new Properties();
        FileInputStream propsFile = null;
        try {
            propsFile = new FileInputStream(PROJECT_PATH + "/replaceregexp.properties");
            original.load(propsFile);
        } finally {
            if (propsFile != null) {
                propsFile.close();
                propsFile = null;
            }
        }

        assertEquals("Def", original.get("OldAbc"));

        executeTarget("testReplace");

        Properties after = new Properties();
        try {
            propsFile = new FileInputStream(PROJECT_PATH + "/test.properties");
            after.load(propsFile);
        } finally {
            if (propsFile != null) {
                propsFile.close();
                propsFile = null;
            }
        }

        assertNull(after.get("OldAbc"));
        assertEquals("AbcDef", after.get("NewProp"));
    }
    // inspired by bug 22541
    public void testDirectoryDateDoesNotChange() {
        executeTarget("touchDirectory");
        File myFile = new File(PROJECT_PATH + "/" + getProject().getProperty("tmpregexp"));
        long timeStampBefore = myFile.lastModified();
        executeTarget("testDirectoryDateDoesNotChange");
        long timeStampAfter = myFile.lastModified();
        assertEquals("directory date should not change",
            timeStampBefore, timeStampAfter);
    }
    public void testDontAddNewline1() throws IOException {
        executeTarget("testDontAddNewline1");
        assertTrue("Files match",
                   FileUtils.newFileUtils()
                   .contentEquals(new File(PROJECT_PATH + "/test.properties"),
                                  new File(PROJECT_PATH + "/replaceregexp2.result.properties")));
    }

    public void testDontAddNewline2() throws IOException {
        executeTarget("testDontAddNewline2");
        assertTrue("Files match",
                   FileUtils.newFileUtils()
                   .contentEquals(new File(PROJECT_PATH + "/test.properties"),
                                  new File(PROJECT_PATH + "/replaceregexp2.result.properties")));
    }

}// ReplaceRegExpTest
