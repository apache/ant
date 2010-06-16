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
import java.io.FileReader;
import java.io.FileWriter;
import junit.framework.TestCase;
import org.apache.tools.ant.util.FileUtils;

public class PropertyFileCLITest extends TestCase {

    public void testPropertyResolution() throws Exception {
        FileUtils fu = FileUtils.getFileUtils();
        File props = fu.createTempFile("propertyfilecli", ".properties",
                                       null, true, true);
        File build = fu.createTempFile("propertyfilecli", ".xml", null, true,
                                       true);
        File log = fu.createTempFile("propertyfilecli", ".log", null, true,
                                     true);
        FileWriter fw = null;
        FileReader fr = null;
        try {
            fw = new FileWriter(props);
            fw.write("w=world\nmessage=Hello, ${w}\n");
            fw.close();
            fw = new FileWriter(build);
            fw.write("<project><echo>${message}</echo></project>");
            fw.close();
            fw = null;
            Main m = new NoExitMain();
            m.startAnt(new String[] {
                    "-propertyfile", props.getAbsolutePath(),
                    "-f", build.getAbsolutePath(),
                    "-l", log.getAbsolutePath()
                }, null, null);
            String l = FileUtils.safeReadFully(fr = new FileReader(log));
            assertTrue("expected log to contain 'Hello, world' but was " + l,
                       l.indexOf("Hello, world") > -1);
        } finally {
            FileUtils.close(fw);
            FileUtils.close(fr);
        }
    }

    private static class NoExitMain extends Main {
        protected void exit(int exitCode) {
        }
    }
}
