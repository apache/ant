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
package org.apache.tools.ant.taskdefs.optional.rjunit.remote;

import java.util.Vector;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class EventDispatcher {

    /** the set of registered listeners */
    private Vector listeners = new Vector();

   /**
     * Add a new listener.
     * @param listener a listener that will receive events from the client.
     */
    public void addListener(TestRunListener listener) {
        listeners.addElement(listener);
    }

    public void removeListener(org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener listener) {
        listeners.removeElement(listener);
    }

   /**
     * Process a message from the client and dispatch the
     * appropriate message to the listeners.
     */
    public void dispatchEvent(TestRunEvent evt) {
       // I hate switch/case but no need to design a complex
       // system for limited events.
        switch (evt.getType()){
            case TestRunEvent.RUN_STARTED:
                fireRunStarted(evt);
                break;
            case TestRunEvent.RUN_ENDED:
                fireRunEnded(evt);
                break;
            case TestRunEvent.RUN_STOPPED:
                fireRunStopped(evt);
                break;
            case TestRunEvent.TEST_STARTED:
                fireTestStarted(evt);
                break;
            case TestRunEvent.TEST_ERROR:
                fireTestError(evt);
                break;
            case TestRunEvent.TEST_FAILURE:
                fireTestFailure(evt);
                break;
            case TestRunEvent.TEST_ENDED:
                fireTestEnded(evt);
                break;
            case TestRunEvent.SUITE_ENDED:
                fireSuiteEnded(evt);
                break;
            case TestRunEvent.SUITE_STARTED:
                fireSuiteStarted(evt);
                break;
            default:
                // should not happen
        }
    }

    protected void fireRunStarted(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onRunStarted(evt);
            }
        }
    }

    protected void fireRunEnded(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onRunEnded(evt);
            }
        }
    }

    protected void fireTestStarted(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onTestStarted(evt);
            }
        }
    }

    protected void fireTestEnded(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onTestEnded(evt);
            }
        }
    }

    protected void fireTestFailure(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onTestFailure(evt);
            }
        }
    }

    protected void fireTestError(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onTestError(evt);
            }
        }
    }

    protected void fireSuiteStarted(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onSuiteStarted(evt);
            }
        }
    }

    protected void fireSuiteEnded(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onSuiteEnded(evt);
            }
        }
    }

    protected void fireRunStopped(TestRunEvent evt) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ((org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunListener) listeners.elementAt(i)).onRunStopped(evt);
            }
        }
    }

}
