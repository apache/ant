/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

import java.util.*;
import org.apache.ant.core.model.*;

/**
 * BuildEventSupport is used by classes which which to send 
 * build events to the BuoldListeners
 */
public class BuildEventSupport {
    private List listeners = new ArrayList();
    
    public void addBuildListener(BuildListener listener) {
        listeners.add(listener);
    }
    
    public void removeBuildListener(BuildListener listener) {
        listeners.remove(listener);
    }

    public void forwardEvent(BuildEvent event) {
        for (Iterator i = listeners.iterator(); i.hasNext();) {
            BuildListener listener = (BuildListener)i.next();
            listener.processBuildEvent(event);
        }
    }
    
    public void fireBuildStarted(Object source, BuildElement element) {
        BuildEvent event = new BuildEvent(source, BuildEvent.BUILD_STARTED, element);
        forwardEvent(event);
    }

    public void fireBuildFinished(Object source, BuildElement element, Throwable cause) {
        BuildEvent event = new BuildEvent(source, BuildEvent.BUILD_FINISHED, element, cause);
        forwardEvent(event);
    }

    public void fireTargetStarted(Object source, BuildElement element) {
        BuildEvent event = new BuildEvent(source, BuildEvent.TARGET_STARTED, element);
        forwardEvent(event);
    }

    public void fireTargetFinished(Object source, BuildElement element, Throwable cause) {
        BuildEvent event = new BuildEvent(source, BuildEvent.TARGET_FINISHED, element, cause);
        forwardEvent(event);
    }

    public void fireTaskStarted(Object source, BuildElement element) {
        BuildEvent event = new BuildEvent(source, BuildEvent.TASK_STARTED, element);
        forwardEvent(event);
    }

    public void fireTaskFinished(Object source, BuildElement element, Throwable cause) {
        BuildEvent event = new BuildEvent(source, BuildEvent.TASK_FINISHED, element, cause);
        forwardEvent(event);
    }

    public void fireMessageLogged(Object source, BuildElement element,
                                   String message, int priority) {
        BuildEvent event = new BuildEvent(source,  element, message, priority);
        forwardEvent(event);
    }
}
