/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
 * or data type that did not exist when Ant started.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class UnknownElement extends Task {

    /**
     * Holds the name of the task/type or nested child element of a
     * task/type that hasn't been defined at parser time.
     */
    private String elementName;

    /**
     * The real object after it has been loaded.
     */
    private Object realThing;

    /**
     * Childelements, holds UnknownElement instances.
     */
    private Vector children = new Vector();

    public UnknownElement (String elementName) {
        this.elementName = elementName;
    }

    /**
     * return the corresponding XML element name.
     */
    public String getTag() {
        return elementName;
    }

    /**
     * creates the real object instance, creates child elements, configures
     * the attributes of the real object.
     */
    public void maybeConfigure() throws BuildException {
        realThing = makeObject(this, wrapper);

        wrapper.setProxy(realThing);
        if (realThing instanceof Task) {
            ((Task) realThing).setRuntimeConfigurableWrapper(wrapper);
        }

        handleChildren(realThing, wrapper);

        wrapper.maybeConfigure(project);
        if (realThing instanceof Task) {
            target.replaceChild(this, realThing);
        } else {
            target.replaceChild(this, wrapper);
        }
    }

    /**
     * Called when the real task has been configured for the first time.
     */
    public void execute() {
        if (realThing == null) {
            // plain impossible to get here, maybeConfigure should
            // have thrown an exception.
            throw new BuildException("Could not create task of type: "
                                     + elementName, location);
        }

        if (realThing instanceof Task) {
            ((Task) realThing).execute();
        }
    }

    /**
     * Adds a child element to this element.
     */
    public void addChild(UnknownElement child) {
        children.addElement(child);
    }

    /**
     * Creates child elements, creates children of the children, sets
     * attributes of the child elements.
     */
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
                realChild = makeTask(child, childWrapper, false);
                ((TaskContainer) parent).addTask((Task) realChild);
            } else {
                realChild = ih.createElement(project, parent, child.getTag());
            }

            childWrapper.setProxy(realChild);
            if (parent instanceof TaskContainer) {
                ((Task) realChild).setRuntimeConfigurableWrapper(childWrapper);
            }

            child.handleChildren(realChild, childWrapper);

            if (parent instanceof TaskContainer) {
                ((Task) realChild).maybeConfigure();
            }
        }
    }

    /**
     * Creates a named task or data type - if it is a task, configure it up to the init() stage.
     */
    protected Object makeObject(UnknownElement ue, RuntimeConfigurable w) {
        Object o = makeTask(ue, w, true);
        if (o == null) {
            o = project.createDataType(ue.getTag());
        }
        if (o == null) {
            throw getNotFoundException("task or type", ue.getTag());
        }
        return o;
    }

    /**
     * Create a named task and configure it up to the init() stage.
     */
    protected Task makeTask(UnknownElement ue, RuntimeConfigurable w,
                            boolean onTopLevel) {
        Task task = project.createTask(ue.getTag());
        if (task == null && !onTopLevel) {
            throw getNotFoundException("task", ue.getTag());
        }

        if (task != null) {
            task.setLocation(getLocation());
            // UnknownElement always has an associated target
            task.setOwningTarget(target);
            task.init();
        }
        return task;
    }

    protected BuildException getNotFoundException(String what,
                                                  String elementName) {
        String lSep = System.getProperty("line.separator");
        String msg = "Could not create " + what + " of type: " + elementName
            + "." + lSep+ lSep
            + "Ant could not find the task or a class this "
            + "task relies upon." + lSep +lSep
            + "This is common and has a number of causes; the usual " + lSep
            + "solutions are to read the manual pages then download and" + lSep
            + "install needed JAR files, or fix the build file: "+ lSep
            + " - You have misspelt '" + elementName + "'." + lSep
            + "   Fix: check your spelling." +lSep
            + " - The task needs an external JAR file to execute" +lSep
            + "   and this is not found at the right place in the classpath." +lSep
            + "   Fix: check the documentation for dependencies." +lSep
            + " - The task is not an Ant core or optional task " +lSep
            + "   and needs to be declared using <taskdef>." +lSep
            + "   Fix: declare the task." +lSep
            + " - The task is an Ant optional task and optional.jar is absent"+lSep
            + "   Fix: look for optional.jar in ANT_HOME/lib, download if needed" +lSep
            + " - The task was not built into optional.jar as dependent"  +lSep
            + "   libraries were not found at build time." + lSep
            + "   Fix: look in the JAR to verify, then rebuild with the needed" +lSep
            + "   libraries, or download a release version from apache.org" +lSep
            + " - The build file was written for a later version of Ant" +lSep
            + "   Fix: upgrade to at least the latest release version of Ant" +lSep
            + lSep
            + "Remember that for JAR files to be visible to Ant tasks implemented" +lSep
            + "in ANT_HOME/lib, the files must be in the same directory or on the" +lSep
            + "classpath"+ lSep
            + lSep
            + "Please neither file bug reports on this problem, nor email the" +lSep
            + "Ant mailing lists, until all of these causes have been explored," +lSep
            + "as this is not an Ant bug.";


        return new BuildException(msg, location);
    }

    /**
     * Get the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
        return realThing == null || !(realThing instanceof Task) ?
            super.getTaskName() : ((Task) realThing).getTaskName();
    }

    /**
     * Return the task instance after it has been created (and if it is a task.
     */
    public Task getTask() {
        if (realThing != null && realThing instanceof Task) {
            return (Task) realThing;
        }
        return null;
    }

}// UnknownElement
