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
package org.apache.ant.antcore.execution;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.apache.ant.common.antlib.Task;
import org.apache.ant.common.event.BuildEvent;
import org.apache.ant.common.event.BuildListener;
import org.apache.ant.common.model.ModelElement;
import org.apache.ant.common.util.DemuxOutputReceiver;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.common.util.AntException;

/**
 * BuildEventSupport is used by classes which which to send build events to
 * the BuildListeners
 *
 * @author Conor MacNeill
 * @created 15 January 2002
 */
public class BuildEventSupport implements DemuxOutputReceiver {
    /**
     * The listeners attached to the object which contains this support
     * object
     */
    private ArrayList listeners = new ArrayList();

    /** Records the latest task to be executed on a thread (Thread to Task). */
    private Map threadTasks = new HashMap();

    /**
     * Gets the listeners of the BuildEventSupport
     *
     * @return the listeners value
     */
    public List getListeners() {
        return (List) listeners.clone();
    }

    /**
     * Add a listener
     *
     * @param listener the listener to be added
     */
    public void addBuildListener(BuildListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     *
     * @param listener the listener to be removed
     */
    public void removeBuildListener(BuildListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fire a build started event
     *
     * @param element the build element with which the event is associated
     */
    public void fireBuildStarted(ModelElement element) {
        BuildEvent event = new BuildEvent(element, BuildEvent.BUILD_STARTED);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.buildStarted(event);
        }
    }

    /**
     * Fir a build finished event
     *
     * @param element the build element with which the event is associated
     * @param cause an exception if there was a failure in the build
     */
    public void fireBuildFinished(ModelElement element,
                                  Throwable cause) {
        BuildEvent event = new BuildEvent(element, BuildEvent.BUILD_FINISHED,
            cause);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.buildFinished(event);
        }
    }

    /**
     * fire a target started event
     *
     * @param element the build element with which the event is associated
     */
    public void fireTargetStarted(ModelElement element) {
        BuildEvent event = new BuildEvent(element, BuildEvent.TARGET_STARTED);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.targetStarted(event);
        }
    }

    /**
     * fire a target finished event
     *
     * @param element the build element with which the event is associated
     * @param cause an exception if there was a failure in the target's task
     */
    public void fireTargetFinished(ModelElement element,
                                   Throwable cause) {
        BuildEvent event = new BuildEvent(element, BuildEvent.TARGET_FINISHED,
            cause);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.targetFinished(event);
        }
    }

    /**
     * fire a task started event
     *
     * @param task the task with which the event is associated
     */
    public void fireTaskStarted(Task task) {
        synchronized (this) {
            threadTasks.put(Thread.currentThread(), task);
        }
        BuildEvent event = new BuildEvent(task, BuildEvent.TASK_STARTED);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.taskStarted(event);
        }
    }

    /**
     * fire a task finished event
     *
     * @param task the task with which the event is associated
     * @param cause an exception if there was a failure in the task
     */
    public void fireTaskFinished(Task task,
                                 Throwable cause) {
        System.out.flush();
        System.err.flush();
        synchronized (this) {
            threadTasks.remove(Thread.currentThread());
        }
        BuildEvent event = new BuildEvent(task, BuildEvent.TASK_FINISHED,
            cause);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.taskFinished(event);
        }
    }

    /**
     * Send a message event
     *
     * @param source the build element with which the event is associated
     * @param message the message to be sent
     * @param priority the priority of the message
     */
    public void fireMessageLogged(Object source,
                                  String message, int priority) {
        BuildEvent event = new BuildEvent(source, message, priority);
        List listeners = getListeners();
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener) i.next();
            listener.messageLogged(event);
        }
    }

    /**
     * Demultiplexes output so that each task receives the appropriate
     * messages. If the current thread is not currently executing a task,
     * the message is logged directly.
     *
     * @param line Message to handle. Should not be <code>null</code>.
     * @param isError Whether the text represents an error (<code>true</code>
     *      ) or information (<code>false</code>).
     */
    public void threadOutput(String line, boolean isError) {
        Task task = (Task) threadTasks.get(Thread.currentThread());
        if (task != null) {
            try {
                if (isError) {
                    task.handleSystemErr(line);
                } else {
                    task.handleSystemOut(line);
                }
                return;
            } catch (AntException e) {
                // ignore just log as a non-task message
            }
        }
        fireMessageLogged(this, line,
            isError ? MessageLevel.MSG_ERR : MessageLevel.MSG_INFO);
    }
}

