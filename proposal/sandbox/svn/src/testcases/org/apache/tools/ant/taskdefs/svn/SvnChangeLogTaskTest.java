/*
 * Copyright  2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.svn;

import java.io.IOException;
import java.io.FileReader;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import junit.framework.Assert;

/**
 */
public class SvnChangeLogTaskTest extends BuildFileTest {

    public SvnChangeLogTaskTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/changelog.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testLog() throws IOException {
        String log = executeTargetAndReadLogFully("log");
        assertRev153687(log);
        assertRev152685(log);
    }

    public void testStart() throws IOException {
        String log = executeTargetAndReadLogFully("start");
        assertRev153687(log);
        assertNoRev152685(log);
    }

    public void testStartDate() throws IOException {
        String log = executeTargetAndReadLogFully("startDate");
        assertRev153687(log);
        assertNoRev152685(log);
    }

    public void testEnd() throws IOException {
        String log = executeTargetAndReadLogFully("end");
        assertNoRev153687(log);
        assertRev152685(log);
    }

    public void testEndDate() throws IOException {
        String log = executeTargetAndReadLogFully("endDate");
        assertNoRev153687(log);
        assertRev152685(log);
    }

    private String executeTargetAndReadLogFully(String target) 
        throws IOException {
        executeTarget(target);
        FileReader r = new FileReader(getProject()
                                      .resolveFile("tmpdir/log.xml"));
        try {
            return FileUtils.readFully(r);
        } finally {
            r.close();
        }
    }

    private static final void assertRev153687(String log) {
        int rev = log.indexOf("<revision>153687</revision>");
        Assert.assertTrue(rev > -1);
        int entryBeforeRev = log.lastIndexOf("<entry>", rev);
        int entryAfterRev = log.indexOf("</entry>", rev);

        Assert.assertTrue(entryBeforeRev > -1);
        Assert.assertTrue(entryAfterRev > -1);

        Assert
            .assertTrue(log.lastIndexOf("<author><![CDATA[dbrosius]]></author>",
                                        rev) > entryBeforeRev);
        Assert
            .assertTrue(log.indexOf("<name><![CDATA[/jakarta/bcel/trunk/src"
                                    + "/java/org/apache/bcel/util/BCELifier."
                                    + "java]]></name>", rev) < entryAfterRev);
        Assert
            .assertTrue(log.indexOf("<action>modified</action>", rev) 
                        < entryAfterRev);
        Assert
            .assertTrue(log.indexOf("<message><![CDATA[Update BCELifier to "
                                    + "handle the new method access flags "
                                    + "(ACC_BRIDGE, ACC_VARARGS)]]></message>",
                                    rev)
                        < entryAfterRev);
    }

    private static final void assertRev152685(String log) {
        int rev = log.indexOf("<revision>152685</revision>");
        Assert.assertTrue(rev > -1);
        int entryBeforeRev = log.lastIndexOf("<entry>", rev);
        int entryAfterRev = log.indexOf("</entry>", rev);

        Assert.assertTrue(entryBeforeRev > -1);
        Assert.assertTrue(entryAfterRev > -1);

        Assert
            .assertTrue(log.lastIndexOf("<![CDATA[(no author)]]>", rev) 
                        > entryBeforeRev);
        Assert
            .assertTrue(log.indexOf("<name><![CDATA[/jakarta/bcel/branches]]>"
                                    + "</name>", rev) < entryAfterRev);
        Assert
            .assertTrue(log.indexOf("<action>added</action>", rev) 
                        < entryAfterRev);
        Assert
            .assertTrue(log.indexOf("<message><![CDATA[New repository "
                                    + "initialized by cvs2svn.]]></message>",
                                    rev)
                        < entryAfterRev);
    }

    private static final void assertNoRev153687(String log) {
        int rev = log.indexOf("<revision>153687</revision>");
        Assert.assertEquals(-1, rev);
    }

    private static final void assertNoRev152685(String log) {
        int rev = log.indexOf("<revision>152685</revision>");
        Assert.assertEquals(-1, rev);
    }
}
