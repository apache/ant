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

package org.apache.tools.ant.taskdefs;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

/**
 */
public class EchoTest extends BuildFileTest {

    public EchoTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/echo.xml");
    }

    public void tearDown() {
        executeTarget("clean");
    }

    // Output an empty String
    public void test1() {
        expectLog("test1", "");
    }
    
    public void testLogBlankEcho() {
        EchoTestLogger logger = new EchoTestLogger();
        getProject().addBuildListener(logger);
        getProject().executeTarget("test1");
        assertEquals("     [echo] ", logger.lastLoggedMessage );
    }
 
    // Output 'OUTPUT OF ECHO'
    public void test2() {
        expectLog("test2", "OUTPUT OF ECHO");
    }

    public void test3() {
        expectLog("test3", "\n"+
                              "    This \n"+
                              "    is\n"+
                              "    a \n"+
                              "    multiline\n"+
                              "    message\n"+
                              "    ");
    }

    public void testFile() throws Exception {
        executeTarget("testFile");
    }

    public void testAppend() throws Exception {
        executeTarget("testAppend");
    }

    public void testEmptyEncoding() throws Exception {
        executeTarget("testEmptyEncoding");
    }

    public void testUTF16Encoding() throws Exception {
        executeTarget("testUTF16Encoding");
    }
    public void testUTF8Encoding() throws Exception {
        executeTarget("testUTF8Encoding");
    }
    
    private class EchoTestLogger extends DefaultLogger {
        String lastLoggedMessage;
    
        
        /**
         * 
         */
        public EchoTestLogger() {
            super();
            this.setMessageOutputLevel(Project.MSG_DEBUG);
            this.setOutputPrintStream(new PrintStream(new ByteArrayOutputStream(256)));
            this.setErrorPrintStream(new PrintStream(new ByteArrayOutputStream(256)));
        }
        /* 
         * @param message
         */
        protected void log(String message) {
            this.lastLoggedMessage = message;
        }
        
    }
}
