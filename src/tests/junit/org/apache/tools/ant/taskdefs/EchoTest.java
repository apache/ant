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

import java.io.*;

import junit.framework.TestCase;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * Test Java-dependent parts of the Echo task.
 */
public class EchoTest extends TestCase {

    private File removeThis;
    /**
     * Create a new EchoTest.
     * @param name
     */
    public EchoTest(String name) {
        super(name);
    }

    public void testLogBlankEcho() {
        Project p = new Project();
        p.init();
        EchoTestLogger logger = new EchoTestLogger();
        p.addBuildListener(logger);
        Echo echo = new Echo();
        echo.setProject(p);
        echo.setTaskName("testLogBlankEcho");
        echo.execute();
        assertEquals("[testLogBlankEcho] ", logger.lastLoggedMessage );
    }

    public void testLogUTF8Echo() {
        Project p = new Project();
        p.init();
        EchoTestLogger logger = new EchoTestLogger();
        p.addBuildListener(logger);
        Echo echo = new Echo();
        echo.setProject(p);
        echo.setTaskName("testLogUTF8Echo");
        echo.setMessage("\u00e4\u00a9");
        removeThis = new File("abc.txt");
        echo.setFile(removeThis);
        echo.setEncoding("UTF-8");
        echo.execute();
        FileUtils fu = FileUtils.getFileUtils();
        try {
        String x = FileUtils.readFully(new InputStreamReader(new FileInputStream(removeThis), "UTF-8" ));
            assertEquals(x,"\u00e4\u00a9");
        } catch (Exception exc) {

        }
    }

    public void tearDown() {
        if (removeThis != null && removeThis.exists()) {
            if (!removeThis.delete())
            {
                removeThis.deleteOnExit();
            }
        }
    }

    private class EchoTestLogger extends DefaultLogger {
        String lastLoggedMessage;
        
        /**
         * Create a new EchoTestLogger.
         */
        public EchoTestLogger() {
            super();
            this.setMessageOutputLevel(Project.MSG_DEBUG);
            this.setOutputPrintStream(new PrintStream(new ByteArrayOutputStream(256)));
            this.setErrorPrintStream(new PrintStream(new ByteArrayOutputStream(256)));
        }

        /**
         * {@inheritDoc}
         */
        protected void log(String message) {
            this.lastLoggedMessage = message;
        }
        
    }
}
