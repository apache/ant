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
package org.apache.tools.ant.gui.core;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.gui.event.*;
import org.apache.tools.ant.gui.acs.ACSProjectElement;
import org.apache.tools.ant.gui.acs.ACSTargetElement;
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
    /** The project manager. */
    private ProjectManager _projectManager = new ProjectManager();
    /** Thing that keeps track of the current selection state. */
    private SelectionManager _selectionManager = new SelectionManager();

    /** Application actions. */
    private ActionManager _actions = 
        new ActionManager(_eventBus, new ResourceManager(
            "org.apache.tools.ant.gui.resources.action"));

    /** Parent frame used in various operations. XXX what do we do 
     *  in the applet context. */
    private Frame _parentFrame = null;

    /** 
     * Constructor of apps that don't have a graphical
     * component (e.g. web based).
     *  
     */
    public AppContext() {
        this(null);
    }

    /** 
     * Standard constructor.
     * 
     * @param parent Parent frame. XXX may go away.
     */
    public AppContext(Frame parent) {
        _parentFrame = parent;
        BuildEventForwarder handler = new BuildEventForwarder(this);
        _projectManager.addBuildListener(handler);
        _eventBus.addMember(EventBus.MONITORING, _selectionManager);
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
     * Get the project manager.
     * 
     * @return Project manager.
     */
    public ProjectManager getProjectManager() {
        return _projectManager;
    }

    /** 
     * Get the selection manager.
     * 
     * @return Selection manager.
     */
    public SelectionManager getSelectionManager() {
        return _selectionManager;
    }

    /** 
     * Determine if debug mode is turned on.
     * 
     * @return True if in debug mode, false otherwise.
     */
    public boolean isDebugOn() {
        return _resources.getBoolean("debug");
    }

}


