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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.Vector;
import java.util.Properties;

import org.apache.tools.ant.taskdefs.optional.junit.TestRunListener;

/**
 * Read and dispatch messages received via an input stream.
 * The inputstream should be the connection to the remote client.
 * <p>
 * All messages are dispatched to the registered listeners.
 * </p>
 * <i>
 * This code is based on the code from Erich Gamma made for the
 * JUnit plugin for Eclipse. {@link http://www.eclipse.org} and is merged
 * with code originating from Ant 1.4.x.
 * </i>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class MessageReader {

    /** the set of registered listeners */
    private Vector listeners = new Vector();

    // communication states with client
    private boolean inReadTrace = false;
    private boolean inFailedMessage = false;
    private String failedTest;
    private String failedMessage;
    private String failedTrace;
    private int failureKind;
    private long elapsedTime;
    private Properties sysprops;

    public MessageReader() {
    }

    /**
     * Add a new listener.
     * @param listener a listener that will receive events from the client.
     */
    public void addListener(TestRunListener listener) {
        listeners.addElement(listener);
    }

    public void removeListener(TestRunListener listener) {
        listeners.removeElement(listener);
    }

    /**
     * Read a complete stream from a client, it will only return
     * once the connection is stopped. You'd better not reuse
     * an instance of this class since there are instance variables used
     * to keep track of the client state.
     * @param in the inputstream to the client.
     */
    public void process(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF8"));
        String line;
        while ((line = reader.readLine()) != null) {
            processMessage(line);
        }
    }

    /**
     * Process a message from the client and dispatch the
     * appropriate message to the listeners.
     */
    protected void processMessage(String message) {
        if (message == null){
            return;
        }

        String arg = message.substring(MessageIds.MSG_HEADER_LENGTH);
        if (message.startsWith(MessageIds.TRACE_START)) {
            failedTrace = arg.substring(0, arg.indexOf(MessageIds.TRACE_END));
            failedTrace = new String(Base64.decode(failedTrace.getBytes()));
            notifyTestFailed(failureKind, failedTest, failedTrace);
            return;
        }

        if (message.startsWith(MessageIds.TEST_COUNT)) {
            int count = Integer.parseInt(arg);
            notifyTestSuiteStarted(count);
            return;
        }
        if (message.startsWith(MessageIds.TEST_START)) {
            notifyTestStarted(arg);
            return;
        }
        if (message.startsWith(MessageIds.TEST_END)) {
            notifyTestEnded(arg);
            return;
        }
        if (message.startsWith(MessageIds.TEST_ERROR)) {
            failedTest = arg;
            failureKind = TestRunListener.STATUS_ERROR;
            return;
        }
        if (message.startsWith(MessageIds.TEST_FAILED)) {
            failedTest = arg;
            failureKind = TestRunListener.STATUS_FAILURE;
            return;
        }
        if (message.startsWith(MessageIds.TEST_ELAPSED_TIME)) {
            elapsedTime = Long.parseLong(arg);
            notifyTestSuiteEnded(elapsedTime);
            return;
        }
        if (message.startsWith(MessageIds.TEST_STOPPED)) {
            elapsedTime = Long.parseLong(arg);
            notifyTestSuiteStopped(elapsedTime);
            return;
        }
        if (message.startsWith(MessageIds.PROPS_START)){
            try {
                byte[] bytes = arg.substring(0, arg.indexOf(MessageIds.PROPS_END)).getBytes();
                bytes = Base64.decode(bytes);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                sysprops = (Properties)ois.readObject();
            } catch (Exception e){
                // ignore now
                e.printStackTrace();
            }
            notifyTestSystemProperties(sysprops);
        }
    }

    protected void notifyTestStarted(String testname) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestStarted(testname);
            }
        }
    }

    protected void notifyTestEnded(String testname) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestEnded(testname);
            }
        }
    }

    protected void notifyTestFailed(int kind, String testname, String trace) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestFailed(kind, testname, trace);
            }
        }
    }

    protected void notifyTestSuiteStarted(int count) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestRunStarted(count);
            }
        }
    }

    protected void notifyTestSuiteEnded(long elapsedtime) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestRunEnded(elapsedtime);
            }
        }
    }

    protected void notifyTestSuiteStopped(long elapsedtime) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestRunStopped(elapsedtime);
            }
        }
    }

    protected void notifyTestSystemProperties(Properties props) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((TestRunListener) listeners.elementAt(i)).onTestRunSystemProperties(props);
            }
        }
    }

}
