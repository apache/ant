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

package org.apache.tools.ant.taskdefs.email;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MessageTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * test for bugzilla 48932
     */
    @Test
    public void testPrintStreamDoesNotGetClosed() throws IOException {
        Message ms = new Message();
        Project p = new Project();
        ms.setProject(p);
        ms.addText("hi, this is an email");
        try (FileOutputStream fos = new FileOutputStream(testFolder.newFile("message.txt"))) {
            ms.print(new PrintStream(fos));
            fos.write(120);
        }
    }
}
