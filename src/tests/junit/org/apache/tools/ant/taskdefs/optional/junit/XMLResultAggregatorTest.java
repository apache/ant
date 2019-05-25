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

package org.apache.tools.ant.taskdefs.optional.junit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.Permission;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.junit.Test;

public class XMLResultAggregatorTest {

    @Test
    public void testFrames() throws Exception {
        // For now, skip this test on JDK 6 (and below); see below for why:
        try {
            Class.forName("java.nio.file.Files");
        } catch (ClassNotFoundException x) {
            assumeNoException("Skip test on JDK 6 and below", x);
        }
        final File d = new File(System.getProperty("java.io.tmpdir"), "XMLResultAggregatorTest");
        if (d.exists()) {
            new Delete() {
                { removeDir(d); }
            }; // is there no utility method for this?
        }
        assertTrue(d.getAbsolutePath(), d.mkdir());
        File xml = new File(d, "x.xml");
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(xml))) {
            pw.println("<testsuite errors='0' failures='0' name='my.UnitTest' tests='1'>");
            pw.println(" <testcase classname='my.UnitTest' name='testSomething'/>");
            pw.println("</testsuite>");
            pw.flush();
        }
        XMLResultAggregator task = new XMLResultAggregator();
        task.setTodir(d);
        Project project = new Project();
        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        project.addBuildListener(logger);
        project.init();
        task.setProject(project);
        AggregateTransformer report = task.createReport();
        report.setTodir(d);
        FileSet fs = new FileSet();
        fs.setFile(xml);
        task.addFileSet(fs);
        /* getResourceAsStream override unnecessary on JDK 7.
         * Ought to work around JAXP #6723276 in JDK 6, but causes a TypeCheckError in FunctionCall for reasons TBD:
        Thread.currentThread().setContextClassLoader(new ClassLoader(ClassLoader.getSystemClassLoader().getParent()) {
            public InputStream getResourceAsStream(String name) {
                if (name.startsWith("META-INF/services/")) {
                    return new ByteArrayInputStream(new byte[0]);
                }
                return super.getResourceAsStream(name);
            }
        });
        */
        // Use the JRE's Xerces, not lib/optional/xerces.jar:
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader().getParent());
        // Tickle #51668:
        System.setSecurityManager(new SecurityManager() {
            public void checkPermission(Permission perm) {
            }
        });
        task.execute();
        assertTrue(new File(d, "index.html").isFile());
    }

}
