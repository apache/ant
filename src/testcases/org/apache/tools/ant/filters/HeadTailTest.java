/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/** JUnit Testcases for TailFilter and HeadFilter
 * @author <a href="mailto:jan@materne.de">Jan Materne</a>
 */
/* I wrote the testcases in one java file because I want also to test the
 * combined behaviour (see end of the class).
*/
public class HeadTailTest extends BuildFileTest {

    public HeadTailTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/filters/head-tail.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testHead() throws IOException {
        executeTarget("testHead");
        File expected = getProject().resolveFile("expected/head-tail.head.test");
        File result = getProject().resolveFile("result/head-tail.head.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testHead: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testHeadLines() throws IOException {
        executeTarget("testHeadLines");
        File expected = getProject().resolveFile("expected/head-tail.headLines.test");
        File result = getProject().resolveFile("result/head-tail.headLines.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testHeadLines: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testHeadSkip() throws IOException {
        executeTarget("testHeadSkip");
        File expected = getProject().resolveFile("expected/head-tail.headSkip.test");
        File result = getProject().resolveFile("result/head-tail.headSkip.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testHeadSkip: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testHeadLinesSkip() throws IOException {
        executeTarget("testHeadLinesSkip");
        File expected = getProject().resolveFile("expected/head-tail.headLinesSkip.test");
        File result = getProject().resolveFile("result/head-tail.headLinesSkip.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testHeadLinesSkip: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testFilterReaderHeadLinesSkip() throws IOException {
        executeTarget("testFilterReaderHeadLinesSkip");
        File expected = getProject().resolveFile(
            "expected/head-tail.headLinesSkip.test");
        File result = getProject().resolveFile(
            "result/head-tail.filterReaderHeadLinesSkip.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testFilterReaderHeadLinesSkip: Result not like expected",
                   fu.contentEquals(expected, result));
    }

    public void testTail() throws IOException {
        executeTarget("testTail");
        File expected = getProject().resolveFile("expected/head-tail.tail.test");
        File result = getProject().resolveFile("result/head-tail.tail.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testTail: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testTailLines() throws IOException {
        executeTarget("testTailLines");
        File expected = getProject().resolveFile("expected/head-tail.tailLines.test");
        File result = getProject().resolveFile("result/head-tail.tailLines.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testTailLines: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testTailSkip() throws IOException {
        executeTarget("testTailSkip");
        File expected = getProject().resolveFile("expected/head-tail.tailSkip.test");
        File result = getProject().resolveFile("result/head-tail.tailSkip.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testTailSkip: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testTailLinesSkip() throws IOException {
        executeTarget("testTailLinesSkip");
        File expected = getProject().resolveFile("expected/head-tail.tailLinesSkip.test");
        File result = getProject().resolveFile("result/head-tail.tailLinesSkip.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testTailLinesSkip: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testFilterReaderTailLinesSkip() throws IOException {
        executeTarget("testFilterReaderTailLinesSkip");
        File expected = getProject().resolveFile(
            "expected/head-tail.tailLinesSkip.test");
        File result = getProject().resolveFile(
            "result/head-tail.filterReaderTailLinesSkip.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testFilterReaderTailLinesSkip: Result not like expected",
                   fu.contentEquals(expected, result));
    }

    public void testHeadTail() throws IOException {
        executeTarget("testHeadTail");
        File expected = getProject().resolveFile("expected/head-tail.headtail.test");
        File result = getProject().resolveFile("result/head-tail.headtail.test");
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("testHeadTail: Result not like expected", fu.contentEquals(expected, result));
    }

}
