/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
import java.io.IOException;

/**
 * Wrapper class that holds all the information necessary to create a task
 * or data type that did not exist when Ant started, or one which
 * has had its definition updated to use a different implementation class.
 *
 * @author Stefan Bodewig
 */
public class UnknownElement extends Task {

    /**
     * Holds the name of the task/type or nested child element of a
     * task/type that hasn't been defined at parser time or has
     * been redefined since original creation.
     */
    private String elementName;

    /**
     * The real object after it has been loaded.
     */
    private Object realThing;

    /**
     * List of child elements (UnknownElements).
     */
    private Vector children = new Vector();

    /**
     * Creates an UnknownElement for the given element name.
     *
     * @param elementName The name of the unknown element.
     *                    Must not be <code>null</code>.
     */
    public UnknownElement (String elementName) {
        this.elementName = elementName;
    }

    /**
     * Returns the name of the XML element which generated this unknown
     * element.
     *
     * @return the name of the XML element which generated this unknown
     *         element.
     */
    public String getTag() {
        return elementName;
    }

    public RuntimeConfigurable getWrapper() {
        return wrapper;
    }

    /**
     * Creates the real object instance and child elements, then configures
     * the attributes and text of the real object. This unknown element
     * is then replaced with the real object in the containing target's list
     * of children.
     *
     * @exception BuildException if the configuration fails
     */
    public void maybeConfigure() throws BuildException {
        //ProjectComponentHelper helper=ProjectComponentHelper.getProjectComponentHelper();
        //realThing = helper.createProjectComponent( this, getProject(), null,
        //                                           this.getTag());

        realThing = makeObject(this, getWrapper());

        getWrapper().setProxy(realThing);
        if (realThing instanceof Task) {
            Task task = (Task) realThing;

            task.setProject(project);
            task.setRuntimeConfigurableWrapper(getWrapper());
            task.setLocation(this.getLocation());
            // UnknownElement always has an associated target
            task.setOwningTarget(this.getOwningTarget());
            task.init();

            // For Script to work. Ugly
            // The reference is replaced by RuntimeConfigurable
            this.getOwningTarget().replaceChild(this, (Task) realThing);
        }

        handleChildren(realThing, getWrapper());

        // configure attributes of the object and it's children. If it is
        // a task container, defer the configuration till the task container
        // attempts to use the task
        getWrapper().maybeConfigure(getProject());
    }

    /**
     * Handles output sent to System.out by this task or its real task.
     *
     * @param line The line of output to log. Should not be <code>null</code>.
     */
    protected void handleOutput(String line) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleOutput(line);
        } else {
            super.handleOutput(line);
        }
    }

    /**
     * @see Task#handleInput(byte[], int, int)
     * 
     * @since Ant 1.6
     */
    protected int handleInput(byte[] buffer, int offset, int length) 
        throws IOException {
        if (realThing instanceof Task) {
            return ((Task) realThing).handleInput(buffer, offset, length);
        } else {
            return super.handleInput(buffer, offset, length);
        }
            
    }
    /**
     * Handles output sent to System.out by this task or its real task.
     *
     * @param line The line of output to log. Should not be <code>null</code>.
     */
    protected void handleFlush(String line) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleFlush(line);
        } else {
            super.handleFlush(line);
        }
    }

    /**
     * Handles error output sent to System.err by this task or its real task.
     *
     * @param line The error line to log. Should not be <code>null</code>.
     */
    protected void handleErrorOutput(String line) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleErrorOutput(line);
        } else {
            super.handleErrorOutput(line);
        }
    }


    /**
     * Handles error output sent to System.err by this task or its real task.
     *
     * @param line The error line to log. Should not be <code>null</code>.
     */
    protected void handleErrorFlush(String line) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleErrorOutput(line);
        } else {
            super.handleErrorOutput(line);
        }
    }
    
    /**
     * Executes the real object if it's a task. If it's not a task
     * (e.g. a data type) then this method does nothing.
     */
    public void execute() {
        if (realThing == null) {
            // plain impossible to get here, maybeConfigure should
            // have thrown an exception.
            throw new BuildException("Could not create task of type: "
                                     + elementName, getLocation());
        }

        if (realThing instanceof Task) {
            ((Task) realThing).execute();
        }

        // the task will not be reused ( a new init() will be called )
        // Let GC do its job
        realThing = null;
    }

    /**
     * Adds a child element to this element.
     *
     * @param child The child element to add. Must not be <code>null</code>.
     */
    public void addChild(UnknownElement child) {
        children.addElement(child);
    }

    /**
     * Creates child elements, creates children of the children
     * (recursively), and sets attributes of the child elements.
     *
     * @param parent The configured object for the parent.
     *               Must not be <code>null</code>.
     *
     * @param parentWrapper The wrapper containing child wrappers
     *                      to be configured. Must not be <code>null</code>
     *                      if there are any children.
     *
     * @exception BuildException if the children cannot be configured.
     */
    protected void handleChildren(Object parent,
                                  RuntimeConfigurable parentWrapper)
        throws BuildException {
        if (parent instanceof TaskAdapter) {
            parent = ((TaskAdapter) parent).getProxy();
        }

        Class parentClass = parent.getClass();
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parentClass);

        for (int i = 0;  i < children.size(); i++) {
            RuntimeConfigurable childWrapper = parentWrapper.getChild(i);
            UnknownElement child = (UnknownElement) children.elementAt(i);
            Object realChild = null;

            if (!ih.supportsNestedElement(child.getTag()) 
                && parent instanceof TaskContainer) {
                realChild = makeTask(child, childWrapper);

                if (realChild == null) {
                    ih.throwNotSupported(getProject(), parent, child.getTag());
                }

                // XXX DataTypes will be wrapped or treated like normal components
                if (realChild instanceof Task) {
                    Task task = (Task) realChild;
                    ((TaskContainer) parent).addTask(task);
                    task.setLocation(child.getLocation());
                    // UnknownElement always has an associated target
                    task.setOwningTarget(this.getOwningTarget());
                    task.init();
                } else {
                    // should not happen
                    ih.throwNotSupported(getProject(), parent, child.getTag());
                }
            } else {
                realChild 
                    = ih.createElement(getProject(), parent, child.getTag());
            }

            childWrapper.setProxy(realChild);
            if (parent instanceof TaskContainer 
                && realChild instanceof Task) {
                ((Task) realChild).setRuntimeConfigurableWrapper(childWrapper);
            }

            child.handleChildren(realChild, childWrapper);
        }
    }

    /**
     * Creates a named task or data type. If the real object is a task,
     * it is configured up to the init() stage.
     *
     * @param ue The unknown element to create the real object for.
     *           Must not be <code>null</code>.
     * @param w  Ignored in this implementation.
     *
     * @return the task or data type represented by the given unknown element.
     */
    protected Object makeObject(UnknownElement ue, RuntimeConfigurable w) {
        Object o = makeTask(ue, w);
        if (o == null) {
            o = getProject().createDataType(ue.getTag());
        }
        if (o == null) {
            throw getNotFoundException("task or type", ue.getTag());
        }
        return o;
    }

    /**
     * Creates a named task and configures it up to the init() stage.
     *
     * @param ue The UnknownElement to create the real task for.
     *           Must not be <code>null</code>.
     * @param w  Ignored.
     *
     * @return the task specified by the given unknown element, or
     *         <code>null</code> if the task name is not recognised.
     */
    protected Task makeTask(UnknownElement ue, RuntimeConfigurable w) {
        Task task = getProject().createTask(ue.getTag());

        if (task != null) {
            task.setLocation(getLocation());
            // UnknownElement always has an associated target
            task.setOwningTarget(getOwningTarget());
            task.init();
        }
        return task;
    }

    /**
     * Returns a very verbose exception for when a task/data type cannot
     * be found.
     *
     * @param what The kind of thing being created. For example, when
     *             a task name could not be found, this would be
     *             <code>"task"</code>. Should not be <code>null</code>.
     * @param elementName The name of the element which could not be found.
     *                    Should not be <code>null</code>.
     *
     * @return a detailed description of what might have caused the problem.
     */
    protected BuildException getNotFoundException(String what,
                                                  String elementName) {
        String lSep = System.getProperty("line.separator");
        String msg = "Could not create " + what + " of type: " + elementName
            + "." + lSep + lSep
            + "Ant could not find the task or a class this "
            + "task relies upon." + lSep + lSep
            + "This is common and has a number of causes; the usual " + lSep
            + "solutions are to read the manual pages then download and" + lSep
            + "install needed JAR files, or fix the build file: " + lSep
            + " - You have misspelt '" + elementName + "'." + lSep
            + "   Fix: check your spelling." + lSep
            + " - The task needs an external JAR file to execute" + lSep
            + "   and this is not found at the right place in the classpath." + lSep
            + "   Fix: check the documentation for dependencies." + lSep
            + "   Fix: declare the task." + lSep
            + " - The task is an Ant optional task and optional.jar is absent" + lSep
            + "   Fix: look for optional.jar in ANT_HOME/lib, download if needed" + lSep
            + " - The task was not built into optional.jar as dependent"  + lSep
            + "   libraries were not found at build time." + lSep
            + "   Fix: look in the JAR to verify, then rebuild with the needed" + lSep
            + "   libraries, or download a release version from apache.org" + lSep
            + " - The build file was written for a later version of Ant" + lSep
            + "   Fix: upgrade to at least the latest release version of Ant" + lSep
            + " - The task is not an Ant core or optional task " + lSep
            + "   and needs to be declared using <taskdef>." + lSep
            + lSep
            + "Remember that for JAR files to be visible to Ant tasks implemented" + lSep
            + "in ANT_HOME/lib, the files must be in the same directory or on the" + lSep
            + "classpath" + lSep
            + lSep
            + "Please neither file bug reports on this problem, nor email the" + lSep
            + "Ant mailing lists, until all of these causes have been explored," + lSep
            + "as this is not an Ant bug.";


        return new BuildException(msg, getLocation());
    }

    /**
     * Returns the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
        //return elementName;
        return realThing == null || !(realThing instanceof Task) ?
            super.getTaskName() : ((Task) realThing).getTaskName();
    }

    /**
     * Returns the task instance after it has been created and if it is a task.
     *
     * @return a task instance or <code>null</code> if the real object is not
     *         a task.
     */
    public Task getTask() {
        if (realThing instanceof Task) {
            return (Task) realThing;
        }
        return null;
    }

}// UnknownElement
