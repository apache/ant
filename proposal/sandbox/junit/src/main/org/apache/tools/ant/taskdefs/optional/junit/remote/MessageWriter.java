/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.tools.ant.taskdefs.optional.junit.TestRunListener;

/**
 * A wrapper that sends string messages to a given stream.
 *
 * <i>
 * This code is based on the code from Erich Gamma made for the
 * JUnit plugin for Eclipse. {@link http://www.eclipse.org} and is merged
 * with code originating from Ant 1.4.x.
 * </i>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class MessageWriter implements MessageIds {

    private PrintWriter pw;

    public MessageWriter(OutputStream out) {
        this.pw = new PrintWriter(out, true);
    }

    protected void finalize(){
        close();
    }

    public void close() {
        if (pw != null){
            pw.close();
            pw = null;
        }
    }

    public void sendMessage(String msg) {
        pw.println(msg);
    }

// --------  notifier helper methods

    public void notifyTestRunStarted(int testCount) {
        sendMessage(MessageIds.TEST_COUNT + testCount);
    }

    public void notifyTestRunEnded(long elapsedTime) {
        sendMessage(MessageIds.TEST_ELAPSED_TIME + elapsedTime);
    }

    public void notifyTestRunStopped(long elapsedTime) {
        sendMessage(MessageIds.TEST_STOPPED + elapsedTime);
    }

    public void notifyTestStarted(String testName) {
        sendMessage(MessageIds.TEST_START + testName);
    }

    public void notifyTestEnded(String testName) {
        sendMessage(MessageIds.TEST_END + testName);
    }

    public void notifyTestFailed(int status, String testName, String trace) {
        if (status == TestRunListener.STATUS_FAILURE) {
            sendMessage(MessageIds.TEST_FAILED + testName);
        } else {
            sendMessage(MessageIds.TEST_ERROR + testName);
        }
        sendMessage(MessageIds.TRACE_START + new String(Base64.encode(trace.getBytes())) + MessageIds.TRACE_END);
    }

    public void notifyStdOutLine(String testname, String line) {
        sendMessage(MessageIds.STDOUT_START);
        sendMessage(line);
        sendMessage(MessageIds.STDOUT_END);
    }

    public void notifyStdErrLine(String testname, String line) {
        sendMessage(MessageIds.STDERR_START);
        sendMessage(line);
        sendMessage(MessageIds.STDERR_END);
    }

    public void notifySystemProperties() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(System.getProperties());
            oos.close();
            String msg = new String(Base64.encode(out.toByteArray()));
            sendMessage(MessageIds.PROPS_START + msg + MessageIds.PROPS_END);
        } catch (IOException e){
            // ignore
            e.printStackTrace();
        }
    }

}
