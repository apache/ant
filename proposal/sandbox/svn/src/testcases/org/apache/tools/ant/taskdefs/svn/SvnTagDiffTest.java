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
public class SvnTagDiffTest extends BuildFileTest {

    public SvnTagDiffTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/tagdiff.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testDiffWithTwoTags() throws IOException {
        String log = executeTargetAndReadLogFully("diff-with-two-tags");
        assertAttributes(log, "initial", "BCEL_5_0");
        assertAdded1(log);
    }

    public void testDiffWithExplicitTrunk() throws IOException {
        String log = executeTargetAndReadLogFully("diff-with-explicit-trunk");
        assertDiffWithTrunk(log);
    }

    public void testDiffWithImplicitTrunk() throws IOException {
        String log = executeTargetAndReadLogFully("diff-with-implicit-trunk");
        assertDiffWithTrunk(log);
    }

    private static void assertDiffWithTrunk(String log) {
        assertAttributes(log, "BCEL_5_0", "trunk");
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

    private static final void assertAttributes(String log, String tag1,
                                               String tag2) {
        int start = log.indexOf("<tagdiff");
        Assert.assertTrue(start > -1);
        int end = log.indexOf(">", start);
        Assert.assertTrue(end > -1);
        Assert.assertTrue(log.indexOf("tag1=\"" + tag1 + "\"", start) > -1);
        Assert.assertTrue(log.indexOf("tag1=\"" + tag1 + "\"", start) < end);
        Assert.assertTrue(log.indexOf("tag2=\"" + tag2 + "\"", start) > -1);
        Assert.assertTrue(log.indexOf("tag2=\"" + tag2 + "\"", start) < end);
        Assert.assertTrue(log.indexOf("svnurl=\"http://svn.apache.org/repos/"
                                      + "asf/jakarta/bcel/\"", start) > -1);
        Assert.assertTrue(log.indexOf("svnurl=\"http://svn.apache.org/repos/"
                                      + "asf/jakarta/bcel/\"", start) < end);
    }

    private static final void assertAdded(String log) {
        int name = log.indexOf("<![CDATA[LICENSE.txt]]>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>added</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>added</action>", name) 
                          < pathAfterName);
    }

    private static final void assertModified(String log) {
        int name = log.indexOf("<name><![CDATA[src/java/org/apache/bcel/"
                               + "Repository.java]]></name>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>modified</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>modified</action>", name) 
                          < pathAfterName);
    }

    private static final void assertDeleted(String log) {
        int name = log.indexOf("<name><![CDATA[LICENSE]]></name>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>deleted</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>deleted</action>", name) 
                          < pathAfterName);
    }

    private static final void assertAdded1(String log) {
        int name = log.indexOf("<name><![CDATA[src/java/org/apache/bcel/"
                               + "Repository.java]]></name>");
        Assert.assertTrue(name > -1);

        int pathAfterName = log.indexOf("</path>", name);
        Assert.assertTrue(pathAfterName > -1);

        Assert.assertTrue(log.indexOf("<action>added</action>", name) > -1);
        Assert.assertTrue(log.indexOf("<action>added</action>", name) 
                          < pathAfterName);
    }

}
