/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.tools.ant.taskdefs.PreSetDef;

/**
 * Wrapper class that holds all the information necessary to create a task
 * or data type that did not exist when Ant started, or one which
 * has had its definition updated to use a different implementation class.
 *
 */
public class UnknownElement extends Task {

    /**
     * Holds the name of the task/type or nested child element of a
     * task/type that hasn't been defined at parser time or has
     * been redefined since original creation.
     */
    private final String elementName;

    /**
     * Holds the namespace of the element.
     */
    private String namespace = "";

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
    private List<UnknownElement> children = null;

    /** Specifies if a predefined definition has been done */
    private boolean presetDefed = false;

    /**
     * Creates an UnknownElement for the given element name.
     *
     * @param elementName The name of the unknown element.
     *                    Must not be <code>null</code>.
     */
    public UnknownElement(String elementName) {
        this.elementName = elementName;
    }

    /**
     * @return the list of nested UnknownElements for this UnknownElement.
     */
    public List<UnknownElement> getChildren() {
        return children;
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

    /**
     * Return the namespace of the XML element associated with this component.
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
        this.namespace = namespace == null ? "" : namespace;
    }

    /**
     * Return the qname of the XML element associated with this component.
     *
     * @return namespace Qname used in the element declaration.
     */
    public String getQName() {
        return qname;
    }

    /**
     * Set the namespace qname of the XML element.
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
        final Object copy = realThing;
        if (copy != null) {
            return;
        }
        configure(makeObject(this, getWrapper()));
    }

    /**
     * Configure the given object from this UnknownElement
     *
     * @param realObject the real object this UnknownElement is representing.
     *
     */
    public void configure(Object realObject) {
        if (realObject == null) {
            return;
        }
        realThing = realObject;

        getWrapper().setProxy(realObject);
        Task task = null;
        if (realObject instanceof Task) {
            task = (Task) realObject;

            task.setRuntimeConfigurableWrapper(getWrapper());

            // For Script example that modifies id'ed tasks in other
            // targets to work. *very* Ugly
            // The reference is replaced by RuntimeConfigurable
            if (getWrapper().getId() != null) {
                this.getOwningTarget().replaceChild(this, (Task) realObject);
            }
       }


        // configure attributes of the object and it's children. If it is
        // a task container, defer the configuration till the task container
        // attempts to use the task

        if (task != null) {
            task.maybeConfigure();
        } else {
            getWrapper().maybeConfigure(getProject());
        }

        handleChildren(realObject, getWrapper());
    }

    /**
     * Handles output sent to System.out by this task or its real task.
     *
     * @param output The output to log. Should not be <code>null</code>.
     */
    protected void handleOutput(String output) {
        final Object copy = realThing;
        if (copy instanceof Task) {
            ((Task) copy).handleOutput(output);
        } else {
            super.handleOutput(output);
        }
    }

    /**
     * Delegate to realThing if present and if it as task.
     * @see Task#handleInput(byte[], int, int)
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @since Ant 1.6
     */
    protected int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        final Object copy = realThing;
        if (copy instanceof Task) {
            return ((Task) copy).handleInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }

    /**
     * Handles output sent to System.out by this task or its real task.
     *
     * @param output The output to log. Should not be <code>null</code>.
     */
    protected void handleFlush(String output) {
        final Object copy = realThing;
        if (copy instanceof Task) {
            ((Task) copy).handleFlush(output);
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
        final Object copy = realThing;
        if (copy instanceof Task) {
            ((Task) copy).handleErrorOutput(output);
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
        final Object copy = realThing;
        if (copy instanceof Task) {
            ((Task) copy).handleErrorFlush(output);
        } else {
            super.handleErrorFlush(output);
        }
    }

    /**
     * Executes the real object if it's a task. If it's not a task
     * (e.g. a data type) then this method does nothing.
     */
    public void execute() {
        final Object copy = realThing;
        if (copy == null) {
            // Got here if the runtimeconfigurable is not enabled.
            return;
        }
        try {
            if (copy instanceof Task) {
                ((Task) copy).execute();
            }
        } finally {
            // Finished executing the task
            // null it (unless it has an ID) to allow
            // GC do its job
            // If this UE is used again, a new "realthing" will be made
            if (getWrapper().getId() == null) {
                realThing = null;
                getWrapper().setProxy(null);
            }
        }
    }

    /**
     * Adds a child element to this element.
     *
     * @param child The child element to add. Must not be <code>null</code>.
     */
    public void addChild(UnknownElement child) {
        if (children == null) {
            children = new ArrayList<>();
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
    protected void handleChildren(
        Object parent,
        RuntimeConfigurable parentWrapper)
        throws BuildException {
        if (parent instanceof TypeAdapter) {
            parent = ((TypeAdapter) parent).getProxy();
        }

        String parentUri = getNamespace();
        Class<?> parentClass = parent.getClass();
        IntrospectionHelper ih = IntrospectionHelper.getHelper(getProject(), parentClass);

        if (children != null) {
            Iterator<UnknownElement> it = children.iterator();
            for (int i = 0; it.hasNext(); i++) {
                RuntimeConfigurable childWrapper = parentWrapper.getChild(i);
                UnknownElement child = it.next();
                try {
                    if (!childWrapper.isEnabled(child)) {
                        if (ih.supportsNestedElement(
                                parentUri, ProjectHelper.genComponentName(
                                    child.getNamespace(), child.getTag()))) {
                            continue;
                        }
                        // fall tru and fail in handlechild (unsupported element)
                    }
                    if (!handleChild(
                            parentUri, ih, parent, child, childWrapper)) {
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
                } catch (UnsupportedElementException ex) {
                    throw new BuildException(
                        parentWrapper.getElementTag()
                        + " doesn't support the nested \"" + ex.getElement()
                        + "\" element.", ex);
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
     * This is used then the realobject of the UE is a PreSetDefinition.
     * This is also used when a presetdef is used on a presetdef
     * The attributes, elements and text are applied to this
     * UE.
     *
     * @param u an UnknownElement containing the attributes, elements and text
     */
    public void applyPreSet(UnknownElement u) {
        if (presetDefed) {
            return;
        }
        // Do the runtime
        getWrapper().applyPreSet(u.getWrapper());
        if (u.children != null) {
            List<UnknownElement> newChildren = new ArrayList<>(u.children);
            if (children != null) {
                newChildren.addAll(children);
            }
            children = newChildren;
        }
        presetDefed = true;
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
        if (!w.isEnabled(ue)) {
            return null;
        }
        ComponentHelper helper = ComponentHelper.getComponentHelper(
            getProject());
        String name = ue.getComponentName();
        Object o = helper.createComponent(ue, ue.getNamespace(), name);
        if (o == null) {
            throw getNotFoundException("task or type", name);
        }
        if (o instanceof PreSetDef.PreSetDefinition) {
            PreSetDef.PreSetDefinition def = (PreSetDef.PreSetDefinition) o;
            o = def.createObject(ue.getProject());
            if (o == null) {
                throw getNotFoundException(
                    "preset " + name,
                    def.getPreSets().getComponentName());
            }
            ue.applyPreSet(def.getPreSets());
            if (o instanceof Task) {
                Task task = (Task) o;
                task.setTaskType(ue.getTaskType());
                task.setTaskName(ue.getTaskName());
                task.init();
            }
        }
        if (o instanceof UnknownElement) {
            o = ((UnknownElement) o).makeObject((UnknownElement) o, w);
        }
        if (o instanceof Task) {
            ((Task) o).setOwningTarget(getOwningTarget());
        }
        if (o instanceof ProjectComponent) {
            ((ProjectComponent) o).setLocation(getLocation());
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
     * @param name The name of the element which could not be found.
     *             Should not be <code>null</code>.
     *
     * @return a detailed description of what might have caused the problem.
     */
    protected BuildException getNotFoundException(String what,
                                                  String name) {
        ComponentHelper helper = ComponentHelper.getComponentHelper(getProject());
        String msg = helper.diagnoseCreationFailure(name, what);
        return new BuildException(msg, getLocation());
    }

    /**
     * Returns the name to use in logging messages.
     *
     * @return the name to use in logging messages.
     */
    public String getTaskName() {
        final Object copy = realThing;
        return !(copy instanceof Task) ? super.getTaskName()
            : ((Task) copy).getTaskName();
    }

    /**
     * Returns the task instance after it has been created and if it is a task.
     *
     * @return a task instance or <code>null</code> if the real object is not
     *         a task.
     */
    public Task getTask() {
        final Object copy = realThing;
        if (copy instanceof Task) {
            return (Task) copy;
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
     * Set the configured object
     * @param realThing the configured object
     * @since ant 1.7
     */
    public void setRealThing(Object realThing) {
        this.realThing = realThing;
    }

    /**
     * Try to create a nested element of <code>parent</code> for the
     * given tag.
     *
     * @return whether the creation has been successful
     */
    private boolean handleChild(
        String parentUri,
        IntrospectionHelper ih,
        Object parent, UnknownElement child,
        RuntimeConfigurable childWrapper) {
        String childName = ProjectHelper.genComponentName(
            child.getNamespace(), child.getTag());
        if (ih.supportsNestedElement(parentUri, childName, getProject(),
                                     parent)) {
            IntrospectionHelper.Creator creator = null;
            try {
                creator = ih.getElementCreator(getProject(), parentUri,
                                               parent, childName, child);
            } catch (UnsupportedElementException use) {
                if (!ih.isDynamic()) {
                    throw use;
                }
                // can't trust supportsNestedElement for dynamic elements
                return false;
            }
            creator.setPolyType(childWrapper.getPolyType());
            Object realChild = creator.create();
            if (realChild instanceof PreSetDef.PreSetDefinition) {
                PreSetDef.PreSetDefinition def =
                    (PreSetDef.PreSetDefinition) realChild;
                realChild = creator.getRealObject();
                child.applyPreSet(def.getPreSets());
            }
            childWrapper.setCreator(creator);
            childWrapper.setProxy(realChild);
            if (realChild instanceof Task) {
                Task childTask = (Task) realChild;
                childTask.setRuntimeConfigurableWrapper(childWrapper);
                childTask.setTaskName(childName);
                childTask.setTaskType(childName);
            }
            if (realChild instanceof ProjectComponent) {
                ((ProjectComponent) realChild).setLocation(child.getLocation());
            }
            childWrapper.maybeConfigure(getProject());
            child.handleChildren(realChild, childWrapper);
            creator.store();
            return true;
        }
        return false;
    }

    /**
     * like contents equals, but ignores project
     * @param obj the object to check against
     * @return true if this UnknownElement has the same contents the other
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
        if (!Objects.equals(elementName, other.elementName)) {
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
        final int childrenSize = children == null ? 0 : children.size();
        if (childrenSize == 0) {
            return other.children == null || other.children.isEmpty();
        }
        if (other.children == null) {
            return false;
        }
        if (childrenSize != other.children.size()) {
            return false;
        }
        for (int i = 0; i < childrenSize; ++i) {
            // children cannot be null childrenSize would have been 0
            UnknownElement child = children.get(i); //NOSONAR
            if (!child.similar(other.children.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make a copy of the unknown element and set it in the new project.
     * @param newProject the project to create the UE in.
     * @return the copied UE.
     */
    public UnknownElement copy(Project newProject) {
        UnknownElement ret = new UnknownElement(getTag());
        ret.setNamespace(getNamespace());
        ret.setProject(newProject);
        ret.setQName(getQName());
        ret.setTaskType(getTaskType());
        ret.setTaskName(getTaskName());
        ret.setLocation(getLocation());
        if (getOwningTarget() == null) {
            Target t = new Target();
            t.setProject(getProject());
            ret.setOwningTarget(t);
        } else {
            ret.setOwningTarget(getOwningTarget());
        }
        RuntimeConfigurable copyRC = new RuntimeConfigurable(
            ret, getTaskName());
        copyRC.setPolyType(getWrapper().getPolyType());
        Map<String, Object> m = getWrapper().getAttributeMap();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            copyRC.setAttribute(entry.getKey(), (String) entry.getValue());
        }
        copyRC.addText(getWrapper().getText().toString());

        for (RuntimeConfigurable r : Collections.list(getWrapper().getChildren())) {
            UnknownElement ueChild = (UnknownElement) r.getProxy();
            UnknownElement copyChild = ueChild.copy(newProject);
            copyRC.addChild(copyChild.getWrapper());
            ret.addChild(copyChild);
        }
        return ret;
    }
}
