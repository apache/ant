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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.io.PrintWriter;
import junit.framework.TestCase;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

public class XMLResultAggregatorTest extends TestCase {

    public XMLResultAggregatorTest(String name) {
        super(name);
    }

    public void testFrames() throws Exception {
        final File d = new File(System.getProperty("java.io.tmpdir"), "XMLResultAggregatorTest");
        new Delete() {{removeDir(d);}}; // is there no utility method for this?
        assertTrue(d.getAbsolutePath(), d.mkdir());
        File xml = new File(d, "x.xml");
        PrintWriter pw = new PrintWriter(xml);
        try {
            pw.println("<testsuite errors='0' failures='0' name='my.UnitTest' tests='1'>");
            pw.println(" <testcase classname='my.UnitTest' name='testSomething'/>");
            pw.println("</testsuite>");
            pw.flush();
        } finally {
            pw.close();
        }
        XMLResultAggregator task = new XMLResultAggregator();
        task.setTodir(d);
        AggregateTransformer report = task.createReport();
        report.setTodir(d);
        FileSet fs = new FileSet();
        fs.setFile(xml);
        task.addFileSet(fs);
        Project project = new Project();
        project.init();
        task.setProject(project);
        task.execute();
        assertTrue(new File(d, "index.html").isFile());
    }

}
