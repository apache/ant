/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.tools.ant.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class PropertyFileCLITest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testPropertyResolution() throws Exception {
        File props = testFolder.newFile("propertyfilecli.properties");
        try (FileWriter fw = new FileWriter(props)) {
            fw.write("w=world\nmessage=Hello, ${w}\n");
        }

        File build = testFolder.newFile("propertyfilecli.xml");
        try (FileWriter fw = new FileWriter(build)) {
            fw.write("<project><echo>${message}</echo></project>");
        }

        Main m = new NoExitMain();
        File log = testFolder.newFile("propertyfilecli.log");
        m.startAnt(new String[] {
                "-propertyfile", props.getAbsolutePath(),
                "-f", build.getAbsolutePath(),
                "-l", log.getAbsolutePath()
        }, null, null);
        try (FileReader fr = new FileReader(log)) {
            assertThat(FileUtils.safeReadFully(fr), containsString("Hello, world"));
        }
    }

    private static class NoExitMain extends Main {
        protected void exit(int exitCode) {
        }
    }
}
