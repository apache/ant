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

package org.apache.tools.ant.taskdefs.email;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

public class MessageTest extends TestCase {
    private File f = new File(System.getProperty("java.io.tmpdir"),
                              "message.txt");
    /**
     * test for bugzilla 48932
     */
    public void testPrintStreamDoesNotGetClosed() {
        Message ms = new Message();
        Project p = new Project();
        ms.setProject(p);
        ms.addText("hi, this is an email");
        FileOutputStream fis = null;
        try {
            fis = new FileOutputStream(f);
            ms.print(new PrintStream(fis));
            fis.write(120);
        } catch (IOException ioe) {
            fail("we should not have issues writing after having called Message.print");
        } finally {
            FileUtils.close(fis);
        }

    }

    public void tearDown() {
        if (f.exists()) {
            FileUtils fu = FileUtils.getFileUtils();
            fu.tryHardToDelete(f);
        }
    }

}
