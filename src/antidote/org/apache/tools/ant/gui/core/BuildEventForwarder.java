/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui.core;

import org.apache.tools.ant.gui.event.*;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildEvent;

/**
 * BuildListener for forwarding events to the EventBus.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class BuildEventForwarder implements BuildListener {

    /** Application context. */
    private AppContext _context = null;

    public BuildEventForwarder(AppContext context) {
        _context = context;
    }

    /**
     *  Fired before any targets are started.
     */
    public void buildStarted(BuildEvent event){
        postEvent(event, BuildEventType.BUILD_STARTED);
        // We doubly post this event.
        _context.getEventBus().postEvent(
            new BuildStartedEvent(_context, event));
    }

    /**
     *  Fired after the last target has finished. This event
     *  will still be thrown if an error occured during the build.
     *
     *  @see BuildEvent#getException()
     */
    public void buildFinished(BuildEvent event) {
        postEvent(event, BuildEventType.BUILD_FINISHED);
        // We doubly post this event.
        _context.getEventBus().postEvent(
            new BuildFinishedEvent(_context, event));
    }

    /**
     *  Fired when a target is started.
     *
     *  @see BuildEvent#getTarget()
     */
    public void targetStarted(BuildEvent event) {
        postEvent(event, BuildEventType.TARGET_STARTED);
    }

    /**
     *  Fired when a target has finished. This event will
     *  still be thrown if an error occured during the build.
     *
     *  @see BuildEvent#getException()
     */
    public void targetFinished(BuildEvent event) {
        postEvent(event, BuildEventType.TARGET_FINISHED);
    }

    /**
     *  Fired when a task is started.
     *
     *  @see BuildEvent#getTask()
     */
    public void taskStarted(BuildEvent event) {
        postEvent(event, BuildEventType.TASK_STARTED);
    }

    /**
     *  Fired when a task has finished. This event will still
     *  be throw if an error occured during the build.
     *
     *  @see BuildEvent#getException()
     */
    public void taskFinished(BuildEvent event) {
        postEvent(event, BuildEventType.TASK_FINISHED);
    }

    /**
     *  Fired whenever a message is logged.
     *
     *  @see BuildEvent#getMessage()
     *  @see BuildEvent#getPriority()
     */
    public void messageLogged(BuildEvent event) {
        postEvent(event, BuildEventType.MESSAGE_LOGGED);
    }

	/** 
	 * Forward the event.
	 * 
	 * @param event Event to forward.
	 * @param type Description of how the event came in.
	 */
    private void postEvent(BuildEvent event, BuildEventType type) {
        _context.getEventBus().postEvent(
            new AntBuildEvent(_context, event, type));
    }


}
