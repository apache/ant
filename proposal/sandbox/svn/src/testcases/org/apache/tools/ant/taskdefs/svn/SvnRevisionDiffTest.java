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
public class SvnRevisionDiffTest extends BuildFileTest {

    public SvnRevisionDiffTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/revisiondiff.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testDiff() throws IOException {
        String log = executeTargetAndReadLogFully("diff");
        assertAttributesNoURL(log);
        assertAdded(log);
        assertModified(log);
        assertDeleted(log);
    }

    public void testDiffUrl() throws IOException {
        String log = executeTargetAndReadLogFully("diff-using-url");
        assertAttributesWithURL(log);
        assertAdded(log);
        assertModified(log);
        assertDeleted(log);
    }

    private String executeTargetAndReadLogFully(String target) 
        throws IOException {
        executeTarget(target);
        FileReader r = new FileReader(getProject()
                                      .resolveFile("tmpdir/diff.xml"));
        try {
            return FileUtils.readFully(r);
        } finally {
            r.close();
        }
    }

    private static final void assertAttributes(String log) {
        int start = log.indexOf("<revisiondiff");
        Assert.assertTrue(start > -1);
        int end = log.indexOf(">", start);
        Assert.assertTrue(end > -1);
        Assert.assertTrue(log.indexOf("start=\"152904\"", start) > -1);
        Assert.assertTrue(log.indexOf("start=\"152904\"") < end);
        Assert.assertTrue(log.indexOf("end=\"153682\"", start) > -1);
        Assert.assertTrue(log.indexOf("end=\"153682\"") < end);
    }

    private static final void assertAttributesNoURL(String log) {
        assertAttributes(log);
        Assert.assertEquals(-1, log.indexOf("svnurl="));
    }

    private static final void assertAttributesWithURL(String log) {
        assertAttributes(log);
        int start = log.indexOf("<revisiondiff");
        int end = log.indexOf(">", start);
        Assert.assertTrue(log.indexOf("svnurl=\"http://svn.apache.org/repos/"
                                      + "asf/jakarta/bcel/trunk\"", start)
                          > -1);
        Assert.assertTrue(log.indexOf("svnurl=\"http://svn.apache.org/repos/"
                                      + "asf/jakarta/bcel/trunk\"", start)
                          < end);
    }

    private static final void assertAdded(String log) {
        int name = log.indexOf("<![CDATA[src/java/org/apache/bcel/classfile/"
                               + "ElementValuePair.java]]>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>added</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>added</action>", name) 
                          < pathAfterName);
    }

    private static final void assertModified(String log) {
        int name = log.indexOf("<name><![CDATA[xdocs/stylesheets/project."
                               + "xml]]></name>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>modified</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>modified</action>", name) 
                          < pathAfterName);
    }

    private static final void assertDeleted(String log) {
        int name = log.indexOf("<name><![CDATA[lib/CCK.jar]]></name>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>deleted</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>deleted</action>", name) 
                          < pathAfterName);
    }
}
