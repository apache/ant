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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
     * Holds the namespace of the element.
     */
    private String namespace;

    /**
     * Holds the namespace qname of the element.
     */
    private String qname;

    /**
     * The real object after it has been loaded.
     */
    private Object realThing;

    /**
     * List of child elements (UnknownElements).
     */
    private List/*<UnknownElement>*/ children = null;

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

    /** Return the namespace of the XML element associated with this component.
     *
     * @return Namespace URI used in the xmlns declaration.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Set the namespace of the XML element associated with this component.
     * This method is typically called by the XML processor.
     * If the namespace is "ant:current", the component helper
     * is used to get the current antlib uri.
     *
     * @param namespace URI used in the xmlns declaration.
     */
    public void setNamespace(String namespace) {
        if (namespace.equals(ProjectHelper.ANT_CURRENT_URI)) {
            ComponentHelper helper = ComponentHelper.getComponentHelper(
                getProject());
            namespace = helper.getCurrentAntlibUri();
        }
        this.namespace = namespace;
    }

    /** Return the qname of the XML element associated with this component.
     *
     * @return namespace Qname used in the element declaration.
     */
    public String getQName() {
        return qname;
    }

    /** Set the namespace qname of the XML element.
     * This method is typically called by the XML processor.
     *
     * @param qname the qualified name of the element
     */
    public void setQName(String qname) {
        this.qname = qname;
    }


    /**
     * Get the RuntimeConfigurable instance for this UnknownElement, containing
     * the configuration information.
     *
     * @return the configuration info.
     */
    public RuntimeConfigurable getWrapper() {
        return super.getWrapper();
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

        configure(makeObject(this, getWrapper()));
    }

    /**
     * Configure the given object from this UnknownElement
     *
     * @param realObject the real object this UnknownElement is representing.
     *
     */
    public void configure(Object realObject) {
        realThing = realObject;

        getWrapper().setProxy(realThing);
        Task task = null;
        if (realThing instanceof Task) {
            task = (Task) realThing;

            task.setRuntimeConfigurableWrapper(getWrapper());

            // For Script to work. Ugly
            // The reference is replaced by RuntimeConfigurable
            this.getOwningTarget().replaceChild(this, (Task) realThing);
        }

        handleChildren(realThing, getWrapper());

        // configure attributes of the object and it's children. If it is
        // a task container, defer the configuration till the task container
        // attempts to use the task

        if (task != null) {
            task.maybeConfigure();
        } else {
            getWrapper().maybeConfigure(getProject());
        }
    }

    /**
     * Handles output sent to System.out by this task or its real task.
     *
     * @param output The output to log. Should not be <code>null</code>.
     */
    protected void handleOutput(String output) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleOutput(output);
        } else {
            super.handleOutput(output);
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
     * @param output The output to log. Should not be <code>null</code>.
     */
    protected void handleFlush(String output) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleFlush(output);
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Handles error output sent to System.err by this task or its real task.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     */
    protected void handleErrorOutput(String output) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleErrorOutput(output);
        } else {
            super.handleErrorOutput(output);
        }
    }


    /**
     * Handles error output sent to System.err by this task or its real task.
     *
     * @param output The error output to log. Should not be <code>null</code>.
     */
    protected void handleErrorFlush(String output) {
        if (realThing instanceof Task) {
            ((Task) realThing).handleErrorOutput(output);
        } else {
            super.handleErrorOutput(output);
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
        if (children == null) {
            children = new ArrayList();
        }
        children.add(child);
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
        if (parent instanceof TypeAdapter) {
            parent = ((TypeAdapter) parent).getProxy();
        }

        Class parentClass = parent.getClass();
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parentClass);


        if (children != null) {
            Iterator it = children.iterator();
            for (int i = 0; it.hasNext(); i++) {
                RuntimeConfigurable childWrapper = parentWrapper.getChild(i);
                UnknownElement child = (UnknownElement) it.next();
                if (!handleChild(ih, parent, child,
                                 childWrapper)) {
                    if (!(parent instanceof TaskContainer)) {
                        ih.throwNotSupported(getProject(), parent,
                                             child.getTag());
                    } else {
                        // a task container - anything could happen - just add the
                        // child to the container
                        TaskContainer container = (TaskContainer) parent;
                        container.addTask(child);
                    }
                }
            }
        }
    }

    /**
     * @return the component name - uses ProjectHelper#genComponentName()
     */
    protected String getComponentName() {
        return ProjectHelper.genComponentName(getNamespace(), getTag());
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
        ComponentHelper helper = ComponentHelper.getComponentHelper(
            getProject());
        String name = ue.getComponentName();
        Object o = helper.createComponent(ue, ue.getNamespace(), name);
        if (o == null) {
            throw getNotFoundException("task or type", name);
        }
        if (o instanceof Task) {
            Task task = (Task) o;
            task.setOwningTarget(getOwningTarget());
            task.init();
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
        return realThing == null
            || !(realThing instanceof Task) ? super.getTaskName()
                                            : ((Task) realThing).getTaskName();
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

    /**
     * Return the configured object
     *
     * @return the real thing whatever it is
     *
     * @since ant 1.6
     */
    public Object getRealThing() {
        return realThing;
    }
    /**
     * Try to create a nested element of <code>parent</code> for the
     * given tag.
     *
     * @return whether the creation has been successful
     */
    private boolean handleChild(IntrospectionHelper ih,
                                Object parent, UnknownElement child,
                                RuntimeConfigurable childWrapper) {
        // backwards compatibility - element names of nested
        // elements have been all lower-case in Ant, except for
        // TaskContainers
        String childName = child.getTag().toLowerCase(Locale.US);
        if (ih.supportsNestedElement(childName)) {
            IntrospectionHelper.Creator creator =
                ih.getElementCreator(getProject(), parent, childName);
            creator.setPolyType(childWrapper.getPolyType());
            Object realChild = creator.create();
            childWrapper.setCreator(creator);
            childWrapper.setProxy(realChild);
            if (realChild instanceof Task) {
                Task childTask = (Task) realChild;
                childTask.setRuntimeConfigurableWrapper(childWrapper);
                childTask.setTaskName(childName);
                childTask.setTaskType(childName);
                childTask.setLocation(child.getLocation());
            }
            child.handleChildren(realChild, childWrapper);
            return true;
        }
        return false;
    }

    /**
     * like contents equals, but ignores project
     * @param obj the object to check against
     * @return true if this unknownelement has the same contents the other
     */
    public boolean similar(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!getClass().getName().equals(obj.getClass().getName())) {
            return false;
        }
        UnknownElement other = (UnknownElement) obj;
        // Are the names the same ?
        if (!equalsString(elementName, other.elementName)) {
            return false;
        }
        if (!namespace.equals(other.namespace)) {
            return false;
        }
        if (!qname.equals(other.qname)) {
            return false;
        }
        // Are attributes the same ?
        if (!getWrapper().getAttributeMap().equals(
                other.getWrapper().getAttributeMap())) {
            return false;
        }
        // Is the text the same?
        //   Need to use equals on the string and not
        //   on the stringbuffer as equals on the string buffer
        //   does not compare the contents.
        if (!getWrapper().getText().toString().equals(
                other.getWrapper().getText().toString())) {
            return false;
        }
        // Are the sub elements the same ?
        if (children == null || children.size() == 0) {
            return other.children == null || other.children.size() == 0;
        }
        if (other.children == null) {
            return false;
        }
        if (children.size() != other.children.size()) {
            return false;
        }
        for (int i = 0; i < children.size(); ++i) {
            UnknownElement child = (UnknownElement) children.get(i);
            if (!child.similar(other.children.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean equalsString(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
