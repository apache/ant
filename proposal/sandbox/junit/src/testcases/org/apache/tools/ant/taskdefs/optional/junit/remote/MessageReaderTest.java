/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.junit.remote;

import java.io.PrintWriter;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.tools.ant.taskdefs.optional.junit.TestRunListener;
import org.apache.tools.ant.taskdefs.optional.junit.TestRunRecorder;

/**
 * Ensure that the Reader/Writer works fine.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class MessageReaderTest extends TestCase {

    protected ByteArrayOutputStream out;

    protected MessageReader reader;

    protected MessageWriter writer;

    protected TestRunRecorder recorder;

    public MessageReaderTest(String s) {
        super(s);
    }

    protected void setUp() {
        out = new ByteArrayOutputStream();
        writer = new MessageWriter(out);
        reader = new MessageReader();
        recorder = new TestRunRecorder();
        reader.addListener( recorder );
    }

    public void testTestRunStarted() throws Exception {
        writer.notifyTestRunStarted(9999);
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        assertEquals(new Integer(9999), recorder.runStarted.elementAt(0));
    }

    public void testTestStarted() throws Exception {
        writer.notifyTestStarted("xxxxxx");
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        assertEquals("xxxxxx", recorder.testStarted.elementAt(0));
    }

    public void testTestEnded() throws Exception {
        writer.notifyTestEnded("xxxxxx");
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        assertEquals("xxxxxx", recorder.testEnded.elementAt(0));
    }

    public void testTestFailedError() throws Exception {
        writer.notifyTestFailed(TestRunListener.STATUS_ERROR, "xxxxxx", "1\n2\n3\n4\n5");
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        TestRunRecorder.TestFailedInfo info = (TestRunRecorder.TestFailedInfo)recorder.testFailed.elementAt(0);
        assertEquals("xxxxxx", info.testname);
        assertEquals(TestRunListener.STATUS_ERROR, info.status);
        assertEquals("1\n2\n3\n4\n5", info.trace);
    }

    public void testTestFailedFailure() throws Exception {
        writer.notifyTestFailed(TestRunListener.STATUS_FAILURE, "xxxxxx", "1\n2\n3\n4\n5");
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        TestRunRecorder.TestFailedInfo info = (TestRunRecorder.TestFailedInfo)recorder.testFailed.elementAt(0);
        assertEquals("xxxxxx", info.testname);
        assertEquals(TestRunListener.STATUS_FAILURE, info.status);
        assertEquals("1\n2\n3\n4\n5", info.trace);
    }

    public void testTestRunEnded() throws Exception {
        writer.notifyTestRunEnded(999999);
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        assertEquals(new Long(999999), recorder.runEnded.elementAt(0));
    }

    public void testTestRunStopped() throws Exception {
        writer.notifyTestRunStopped(999999);
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        assertEquals(new Long(999999), recorder.runStopped.elementAt(0));
    }

    public void testTestRunSystemProperties() throws Exception {
        writer.notifySystemProperties();
        reader.process( new ByteArrayInputStream(out.toByteArray()) );
        assertEquals(System.getProperties(), recorder.sysprops.elementAt(0));
    }

}
