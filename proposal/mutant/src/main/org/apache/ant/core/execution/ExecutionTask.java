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

package org.apache.ant.core.execution;

import org.apache.ant.core.model.*;
import org.apache.ant.core.support.*;
import java.util.*;
import java.net.URL;

/**
 * An ExecutionTask is the execution time equivalent of the Task
 * object in the Ant project model. Subclasses of ExecutionTask are
 * created by Task writers to implement particular, desired 
 * functionality
 *
 * An ExecutionTask subclass is created for a particular task type. 
 * The data from the task model is introspected into the ExecutionTask
 * which is then executed.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public abstract class ExecutionTask {
    private ExecutionFrame frame = null;
    private Location location = Location.UNKNOWN_LOCATION;
    private BuildEventSupport eventSupport;
    private BuildElement buildElement;

    void setExecutionFrame(ExecutionFrame frame) {
        this.frame = frame;
    }

    /**
     * Get the ExecutionFrame in which this ExecutionTask is being executed.
     * to which this task belongs
     *
     * @return the execution task's ExecutionFrame.
     */
    public ExecutionFrame getExecutionFrame() {
        return frame;
    }

    /**
     * Configure the event support to be used to fire events
     */
    void setBuildEventSupport(BuildEventSupport eventSupport) {
        this.eventSupport = eventSupport;
    }
    
    /**
     * Associate this ExecutionTask with a buildElement in the
     * project model
     */
    void setBuildElement(BuildElement buildElement) {
        this.buildElement = buildElement;
    }
    
    /**
     * Log a message with the default (INFO) priority.
     *
     * @param msg the message to be logged.
     */
    public void log(String msg) {
        log(msg, BuildEvent.MSG_INFO);
    }

    /**
     * Log a mesage with the give priority.
     *
     * @param the message to be logged.
     * @param msgLevel the message priority at which this message is to be logged.
     */
    public void log(String msg, int msgLevel) {
        eventSupport.fireMessageLogged(this, buildElement, msg, msgLevel);
    }

    /**
     * Called by the project to let the task initialize properly. 
     *
     * @throws ExecutionException if someting goes wrong with the build
     */
    public void init() throws ExecutionException {}

    /**
     * Called by the frame to let the task do it's work. 
     *
     * @throws ExecutionException if someting goes wrong with the build
     */
    abstract public void execute() throws ExecutionException;

    /**
     * Returns the file location where this task was defined.
     */
    public Location getLocation() {
        return buildElement.getLocation();
    }
    
}

