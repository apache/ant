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
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.gui.event.*;
import java.awt.Frame;
import java.util.*;

/**
 * A container for the state information for the application. Provides
 * a centeralized place to gain access to resources and data.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class AppContext {
    /** Event bus. */
    private EventBus _eventBus = new EventBus();
    /** Application resources. */
    private ResourceManager _resources = new ResourceManager();
    /** Application actions. */
    private ActionManager _actions = new ActionManager(_eventBus);
    /** List of build listeners to register when build starts. */
    private List _buildListeners = new LinkedList();

    /** Parent frame used in various operations. XXX what do we do 
     *  in the applet context. */
    private Frame _parentFrame = null;

    /** The current data model. */
    private ProjectProxy _project = null;

    public AppContext(Frame parent) {
        _parentFrame = parent;
        // Add the build listener for dispatching BuildEvent
        // objects to the EventBus.
        BuildEventForwarder handler = new BuildEventForwarder(this);
        addBuildListener(handler);
    }

	/** 
	 * Get the localized resources.
	 * 
	 * @return Resources.
	 */
    public ResourceManager getResources() {
        return _resources;
    }

	/** 
	 * Get the action manager.
	 * 
	 * @return Action manager.
	 */
    public ActionManager getActions() {
        return _actions;
    }

	/** 
	 * Get the event bus.
	 * 
	 * @return EventBus.
	 */
    public EventBus getEventBus() {
        return _eventBus;
    }

	/** 
	 * Get the parent frame. XXX may change...
	 * 
	 * @return Parent frame.
	 */
    public Frame getParentFrame() {
        return _parentFrame;
    }

	/** 
	 * Get the current project.
	 * 
	 * @return Current project. NUll if  no active project.
	 */
    public ProjectProxy getProject() {
        return _project;
    }


	/** 
	 * Add a build listener.
	 * 
	 * @param l Listener to add.
	 */
    public void addBuildListener(BuildListener l) {
        _buildListeners.add(l);
    }

	/** 
	 * Remove a build listener.
	 * 
	 * @param l Listener to remove.
	 */
    public void removeBuildListener(BuildListener l) {
        _buildListeners.remove(l);
    }

	/** 
	 * Determine if the given BuildListener is registered.
	 * 
	 * @param l Listener to test for.
	 * @return True if listener has been added, false if unknown.
	 */
    public boolean isRegisteredBuildListener(BuildListener l) {
        return _buildListeners.contains(l);
    }

	/** 
	 * Get the set of current build listeners.
	 * 
     * @return Set of current build listeners.
	 */
    public BuildListener[] getBuildListeners() {
        BuildListener[] retval = new BuildListener[_buildListeners.size()];
        _buildListeners.toArray(retval);
        return retval;
    }

	/** 
	 * Set the current project.
	 * 
	 * @param project Next project to operate on. May be null for the "close" 
     * action.

	 */
    public void setProject(ProjectProxy project) {
        if(_project == null || !_project.equals(project)) {
            _project = project;
            getEventBus().postEvent(new NewProjectEvent(this));
        }
    }
}


