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

package org.apache.tools.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import junit.framework.TestCase;

public class PropertyFileCLITest extends TestCase {

    public void testPropertyResolution() throws Exception {
        File props = File.createTempFile("propertyfilecli", ".properties");
        props.deleteOnExit();
        FileWriter fw = new FileWriter(props);
        fw.write("w=world\nmessage=Hello, ${w}\n");
        fw.close();
        File build = File.createTempFile("propertyfilecli", ".xml");
        build.deleteOnExit();
        fw = new FileWriter(build);
        fw.write("<project><echo>${message}</echo></project>");
        fw.close();
        PrintStream sysOut = System.out;
        StringBuffer sb = new StringBuffer();
        try {
            PrintStream out =
                new PrintStream(new BuildFileTest.AntOutputStream(sb));
            System.setOut(out);
            Main m = new NoExitMain();
            m.startAnt(new String[] {
                    "-propertyfile", props.getAbsolutePath(),
                    "-f", build.getAbsolutePath()
                }, null, null);
        } finally {
            System.setOut(sysOut);
        }
        String log = sb.toString();
        assertTrue("expected log to contain 'Hello, world' but was " + log,
                   log.indexOf("Hello, world") > -1);
    }

    private static class NoExitMain extends Main {
        protected void exit(int exitCode) {
        }
    }
}
