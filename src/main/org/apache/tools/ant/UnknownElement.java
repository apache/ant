/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant;

import java.util.Vector;

/**
 * Wrapper class that holds all information necessary to create a task
 * that did not exist when Ant started.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class UnknownElement extends Task {

    private String elementName;
    private Task realTask;
    private Vector children = new Vector();
    
    public UnknownElement (String elementName) {
        this.elementName = elementName;
    }
    
    /**
     * return the corresponding XML tag.
     */
    public String getTag() {
        return elementName;
    }

    public void maybeConfigure() throws BuildException {
        realTask = makeTask(this, wrapper);

        wrapper.setProxy(realTask);
        realTask.setRuntimeConfigurableWrapper(wrapper);

        handleChildren(realTask, wrapper);

        realTask.maybeConfigure();
        target.replaceTask(this, realTask);
    }

    /**
     * Called when the real task has been configured for the first time.
     */
    public void execute() {
        if (realTask == null) {
            // plain impossible to get here, maybeConfigure should 
            // have thrown an exception.
            throw new BuildException("Could not create task of type: "
                                     + elementName, location);
        }
        realTask.execute();
    }

    public void addChild(UnknownElement child) {
        children.addElement(child);
    }

    protected void handleChildren(Object parent, 
                                  RuntimeConfigurable parentWrapper) 
        throws BuildException {

        if (parent instanceof TaskAdapter) {
            parent = ((TaskAdapter) parent).getProxy();
        }

        Class parentClass = parent.getClass();
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parentClass);
        
        for (int i=0; i<children.size(); i++) {
            RuntimeConfigurable childWrapper = parentWrapper.getChild(i);
            UnknownElement child = (UnknownElement) children.elementAt(i);
            Object realChild = null;
            if (parent instanceof TaskContainer) {
                realChild = makeTask(child, childWrapper);
                ((TaskContainer) parent).addTask((Task) realChild);
            } else {
                realChild = ih.createElement(project, parent, child.getTag());
            }

            childWrapper.setProxy(realChild);
            if (realChild instanceof Task) {
                ((Task) realChild).setRuntimeConfigurableWrapper(childWrapper);
            }
            child.handleChildren(realChild, childWrapper);
            if (realChild instanceof Task) {
                ((Task) realChild).maybeConfigure();
            }
        }
    }

    /**
     * Create a named task and configure it up to the init() stage.
     */
    protected Task makeTask(UnknownElement ue, RuntimeConfigurable w) {
        Task task = project.createTask(ue.getTag());
        if (task == null) {
            log("Could not create task of type: " + elementName + " Common solutions" +
                " are adding the task to defaults.properties and executing bin/bootstrap",
                Project.MSG_DEBUG);
            throw new BuildException("Could not create task of type: " + elementName +
                                     ". Common solutions are to use taskdef to declare" +
                                     " your task, or, if this is an optional task," +
                                     " to put the optional.jar in the lib directory of" +
                                     " your ant installation (ANT_HOME).", location);
        }

        task.setLocation(getLocation());
        String id = w.getAttributes().getValue("id");
        if (id != null) {
            project.addReference(id, task);
        }
        // UnknownElement always has an associated target
        task.setOwningTarget(target);

        task.init();
        return task;
    }

    /**
     * Get the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
        return realTask == null ? super.getTaskName() : realTask.getTaskName();
    }

}// UnknownElement
