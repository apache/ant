/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.util.WeakishReference;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import java.util.Vector;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Component creation and configuration.
 *
 * The class is based around handing component
 * definitions in an AntTypeTable.
 *
 * The old task/type methods have been kept
 * for backward compatibly.
 * Project will just delegate its calls to this class.
 *
 * A very simple hook mechnism is provided that allows users to plug
 * in custom code. It is also possible to replace the default behavior
 * ( for example in an app embeding ant )
 *
 * @author Costin Manolache
 * @author Peter Reilly
 * @since Ant1.6
 */
public class ComponentHelper  {
    /** Map from compoennt name to anttypedefinition */
    private AntTypeTable antTypeTable;

    /** Map of tasks generated from antTypeTable */
    private Hashtable taskClassDefinitions = new Hashtable();
    /** flag to rebuild taskClassDefinitions */
    private boolean rebuildTaskClassDefinitions = true;

    /** Map of types generated from antTypeTable */
    private Hashtable typeClassDefinitions = new Hashtable();
    /** flag to rebuild typeClassDefinitions */
    private boolean rebuildTypeClassDefinitions = true;

    /**
     * Map from task names to vectors of created tasks
     * (String to Vector of Task). This is used to invalidate tasks if
     * the task definition changes.
     */
    private Hashtable createdTasks = new Hashtable();

    private ComponentHelper next;
    private Project project;

    /**
     * find a project component for a specific project, creating
     * it if it does not exist
     * @param project the project
     * @return the project component for a specific project
     */
    public static ComponentHelper getComponentHelper(Project project) {
        // Singleton for now, it may change ( per/classloader )
        ComponentHelper ph = (ComponentHelper) project.getReference(
            "ant.ComponentHelper");
        if (ph != null) {
            return ph;
        }
        ph = new ComponentHelper();
        ph.setProject(project);

        project.addReference("ant.ComponentHelper", ph);
        return ph;
    }

    /**
     * Creates a new ComponentHelper instance.
     */
    protected ComponentHelper() {
    }

    /**
     * Set the next chained component helper
     *
     * @param next the next chained component helper
     */
    public void setNext(ComponentHelper next) {
        this.next = next;
    }

    /**
     * Get the next chained component helper
     *
     * @return the next chained component helper
     */
    public ComponentHelper getNext() {
        return next;
    }

    /**
     * Sets the project for this component helper
     *
     * @param project the project for this helper
     */
    public void setProject(Project project) {
        this.project = project;
        antTypeTable = new AntTypeTable(project);
    }

    /**
     * Used with creating child projects. Each child
     * project inherites the component definitions
     * from its parent.
     * @param helper the component helper of the parent project
     */
    public void initSubProject(ComponentHelper helper) {
        // add the types of the parent project
        AntTypeTable typeTable = helper.antTypeTable;
        for (Iterator i = typeTable.values().iterator(); i.hasNext();) {
            AntTypeDefinition def = (AntTypeDefinition) i.next();
            antTypeTable.put(def.getName(), def);
        }
    }

    /** Factory method to create the components.
     *
     * This should be called by UnknownElement.
     *
     * @param ue The component helper has access via ue to the entire XML tree.
     * @param ns Namespace. Also available as ue.getNamespace()
     * @param taskName The element name. Also available as ue.getTag()
     * @return the created component
     * @throws BuildException if an error occuries
     */
    public Object createComponent(UnknownElement ue,
                                  String ns,
                                  String taskName)
        throws BuildException {
        Object component = createComponent(taskName);
        if (component == null) {
            return null;
        }

        if (component instanceof Task) {
            Task task = (Task) component;
            task.setTaskType(taskName);
            task.setTaskName(taskName);
            addCreatedTask(taskName, task);
        }

        return component;
    }

    /**
     * Create an object for a component.
     *
     * @param componentName the name of the component, if
     *                      the component is in a namespace, the
     *                      name is prefixed withe the namespace uri and ":"
     * @return the class if found or null if not.
     */
    public Object createComponent(String componentName) {
        return antTypeTable.create(componentName);
    }

    /**
     * Return the class of the component name.
     *
     * @param componentName the name of the component, if
     *                      the component is in a namespace, the
     *                      name is prefixed withe the namespace uri and ":"
     * @return the class if found or null if not.
     */
    public Class getComponentClass(String componentName) {
        return antTypeTable.getExposedClass(componentName);
    }

    /**
     * Return the antTypeDefinition for a componentName
     * @param componentName the name of the component
     * @return the ant definition or null if not present
     */
    public AntTypeDefinition getDefinition(String componentName) {
        return antTypeTable.getDefinition(componentName);
    }

    /**
     * Initialization code - implementing the original ant component
     * loading from /org/apache/tools/ant/taskdefs/default.properties
     * and .../types/default.properties
     */
    public void initDefaultDefinitions() {
        initTasks();
        initTypes();
    }

    /**
     * Adds a new task definition to the project.
     * Attempting to override an existing definition with an
     * equivalent one (i.e. with the same classname) results in
     * a verbose log message. Attempting to override an existing definition
     * with a different one results in a warning log message and
     * invalidates any tasks which have already been created with the
     * old definition.
     *
     * @param taskName The name of the task to add.
     *                 Must not be <code>null</code>.
     * @param taskClass The full name of the class implementing the task.
     *                  Must not be <code>null</code>.
     *
     * @exception BuildException if the class is unsuitable for being an Ant
     *                           task. An error level message is logged before
     *                           this exception is thrown.
     *
     * @see #checkTaskClass(Class)
     */
    public void addTaskDefinition(String taskName, Class taskClass) {
        checkTaskClass(taskClass);
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(taskName);
        def.setClassLoader(taskClass.getClassLoader());
        def.setClass(taskClass);
        def.setAdapterClass(TaskAdapter.class);
        def.setClassName(taskClass.getName());
        def.setAdaptToClass(Task.class);
        updateDataTypeDefinition(def);
    }

    /**
     * Checks whether or not a class is suitable for serving as Ant task.
     * Ant task implementation classes must be public, concrete, and have
     * a no-arg constructor.
     *
     * @param taskClass The class to be checked.
     *                  Must not be <code>null</code>.
     *
     * @exception BuildException if the class is unsuitable for being an Ant
     *                           task. An error level message is logged before
     *                           this exception is thrown.
     */
    public void checkTaskClass(final Class taskClass) throws BuildException {
        if (!Modifier.isPublic(taskClass.getModifiers())) {
            final String message = taskClass + " is not public";
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        if (Modifier.isAbstract(taskClass.getModifiers())) {
            final String message = taskClass + " is abstract";
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        try {
            taskClass.getConstructor(null);
            // don't have to check for public, since
            // getConstructor finds public constructors only.
        } catch (NoSuchMethodException e) {
            final String message = "No public no-arg constructor in "
                    + taskClass;
            project.log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        if (!Task.class.isAssignableFrom(taskClass)) {
            TaskAdapter.checkTaskClass(taskClass, project);
        }
    }

    /**
     * Returns the current task definition hashtable. The returned hashtable is
     * "live" and so should not be modified.
     *
     * @return a map of from task name to implementing class
     *         (String to Class).
     */
    public Hashtable getTaskDefinitions() {
        synchronized (taskClassDefinitions) {
            synchronized (antTypeTable) {
                if (rebuildTaskClassDefinitions) {
                    taskClassDefinitions.clear();
                    for (Iterator i = antTypeTable.keySet().iterator();
                         i.hasNext();) {
                        String name = (String) i.next();
                        Class clazz =
                            (Class) antTypeTable.getExposedClass(name);
                        if (clazz == null) {
                            continue;
                        }
                        if (Task.class.isAssignableFrom(clazz)) {
                            taskClassDefinitions.put(
                                name, antTypeTable.getTypeClass(name));
                        }
                    }
                    rebuildTaskClassDefinitions = false;
                }
            }
        }
        return taskClassDefinitions;
    }


    /**
     * Returns the current type definition hashtable. The returned hashtable is
     * "live" and so should not be modified.
     *
     * @return a map of from type name to implementing class
     *         (String to Class).
     */
    public Hashtable getDataTypeDefinitions() {
        synchronized (typeClassDefinitions) {
            synchronized (antTypeTable) {
                if (rebuildTypeClassDefinitions) {
                    typeClassDefinitions.clear();
                    for (Iterator i = antTypeTable.keySet().iterator();
                         i.hasNext();) {
                        String name = (String) i.next();
                        Class clazz =
                            (Class) antTypeTable.getExposedClass(name);
                        if (clazz == null) {
                            continue;
                        }
                        if (!(Task.class.isAssignableFrom(clazz))) {
                            typeClassDefinitions.put(
                                name, antTypeTable.getTypeClass(name));
                        }
                    }
                    rebuildTypeClassDefinitions = false;
                }
            }
        }
        return typeClassDefinitions;
    }

    /**
     * Adds a new datatype definition.
     * Attempting to override an existing definition with an
     * equivalent one (i.e. with the same classname) results in
     * a verbose log message. Attempting to override an existing definition
     * with a different one results in a warning log message, but the
     * definition is changed.
     *
     * @param typeName The name of the datatype.
     *                 Must not be <code>null</code>.
     * @param typeClass The full name of the class implementing the datatype.
     *                  Must not be <code>null</code>.
     */
    public void addDataTypeDefinition(String typeName, Class typeClass) {
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(typeName);
        def.setClass(typeClass);
        updateDataTypeDefinition(def);
        String msg = " +User datatype: " + typeName + "     "
                + typeClass.getName();
        project.log(msg, Project.MSG_DEBUG);
    }

    /**
     * Describe <code>addDataTypeDefinition</code> method here.
     *
     * @param def an <code>AntTypeDefinition</code> value
     */
    public void addDataTypeDefinition(AntTypeDefinition def) {
        updateDataTypeDefinition(def);
    }

    /**
     * Returns the current datatype definition hashtable. The returned
     * hashtable is "live" and so should not be modified.
     *
     * @return a map of from datatype name to implementing class
     *         (String to Class).
     */
    public Hashtable getAntTypeTable() {
        return antTypeTable;
    }

    /**
     * Creates a new instance of a task, adding it to a list of
     * created tasks for later invalidation. This causes all tasks
     * to be remembered until the containing project is removed
     *
     *  Called from Project.createTask(), which can be called by tasks.
     *  The method should be deprecated, as it doesn't support ns and libs.
     *
     * @param taskType The name of the task to create an instance of.
     *                 Must not be <code>null</code>.
     *
     * @return an instance of the specified task, or <code>null</code> if
     *         the task name is not recognised.
     *
     * @exception BuildException if the task name is recognised but task
     *                           creation fails.
     */
    public Task createTask(String taskType) throws BuildException {
        Task task = createNewTask(taskType);
        if (task == null && taskType.equals("property")) {
            // quick fix for Ant.java use of property before
            // initializeing the project
            addTaskDefinition("property",
                              org.apache.tools.ant.taskdefs.Property.class);
            task = createNewTask(taskType);
        }

        if (task != null) {
            addCreatedTask(taskType, task);
        }
        return task;
    }

    /**
     * Creates a new instance of a task. This task is not
     * cached in the createdTasks list.
     * @since ant1.6
     * @param taskType The name of the task to create an instance of.
     *                 Must not be <code>null</code>.
     *
     * @return an instance of the specified task, or <code>null</code> if
     *         the task name is not recognised.
     *
     * @exception BuildException if the task name is recognised but task
     *                           creation fails.
     */
    private Task createNewTask(String taskType) throws BuildException {
        Class c = antTypeTable.getExposedClass(taskType);
        if (c == null) {
            return null;
        }

        if (!(Task.class.isAssignableFrom(c))) {
            return null;
        }
        Task task = (Task) antTypeTable.create(taskType);
        if (task == null) {
            return null;
        }
        task.setTaskType(taskType);

        // set default value, can be changed by the user
        task.setTaskName(taskType);

        String msg = "   +Task: " + taskType;
        project.log (msg, Project.MSG_DEBUG);
        return task;
    }

    /**
     * Keeps a record of all tasks that have been created so that they
     * can be invalidated if a new task definition overrides the current one.
     *
     * @param type The name of the type of task which has been created.
     *             Must not be <code>null</code>.
     *
     * @param task The freshly created task instance.
     *             Must not be <code>null</code>.
     */
    private void addCreatedTask(String type, Task task) {
        synchronized (createdTasks) {
            Vector v = (Vector) createdTasks.get(type);
            if (v == null) {
                v = new Vector();
                createdTasks.put(type, v);
            }
            v.addElement(WeakishReference.createReference(task));
        }
    }

    /**
     * Mark tasks as invalid which no longer are of the correct type
     * for a given taskname.
     *
     * @param type The name of the type of task to invalidate.
     *             Must not be <code>null</code>.
     */
    private void invalidateCreatedTasks(String type) {
        synchronized (createdTasks) {
            Vector v = (Vector) createdTasks.get(type);
            if (v != null) {
                Enumeration taskEnum = v.elements();
                while (taskEnum.hasMoreElements()) {
                    WeakishReference ref =
                            (WeakishReference) taskEnum.nextElement();
                    Task t = (Task) ref.get();
                    //being a weak ref, it may be null by this point
                    if (t != null) {
                        t.markInvalid();
                    }
                }
                v.removeAllElements();
                createdTasks.remove(type);
            }
        }
    }

    /**
     * Creates a new instance of a data type.
     *
     * @param typeName The name of the data type to create an instance of.
     *                 Must not be <code>null</code>.
     *
     * @return an instance of the specified data type, or <code>null</code> if
     *         the data type name is not recognised.
     *
     * @exception BuildException if the data type name is recognised but
     *                           instance creation fails.
     */
    public Object createDataType(String typeName) throws BuildException {
        return antTypeTable.create(typeName);
    }

    /**
     * Returns a description of the type of the given element.
     * <p>
     * This is useful for logging purposes.
     *
     * @param element The element to describe.
     *                Must not be <code>null</code>.
     *
     * @return a description of the element type
     *
     * @since Ant 1.6
     */
    public String getElementName(Object element) {
        //  PR: I do not know what to do if the object class
        //      has multiple defines
        //      but this is for logging only...
        Class elementClass = element.getClass();
        for (Iterator i = antTypeTable.values().iterator(); i.hasNext();) {
            AntTypeDefinition def = (AntTypeDefinition) i.next();
            if (elementClass == def.getExposedClass(project)) {
                return "The <" + def.getName() + "> type";
            }
        }
        return "Class " + elementClass.getName();
    }


    /**
     * check if definition is a valid definition - it
     * may be a definition of an optional task that
     * does not exist
     * @param def the definition to test
     * @return true if exposed type of definition is present
     */
    private boolean validDefinition(AntTypeDefinition def) {
        if (def.getTypeClass(project) == null
            || def.getExposedClass(project) == null) {
            return false;
        }
        return true;
    }

    /**
     * check if two definitions are the same
     * @param def  the new definition
     * @param old the old definition
     * @return true if the two definitions are the same
     */
    private boolean sameDefinition(
        AntTypeDefinition def, AntTypeDefinition old) {
        if (!validDefinition(def) || !validDefinition(old)) {
            return validDefinition(def) == validDefinition(old);
        }

        if (!(old.getTypeClass(project).equals(def.getTypeClass(project)))) {
            return false;
        }
        if (!(old.getExposedClass(project).equals(
                  def.getExposedClass(project)))) {
            return false;
        }
        return true;
    }


    /**
     * update the component definition table with a new or
     * modified definition.
     * @param def the definition to update or insert
     */
    private void updateDataTypeDefinition(AntTypeDefinition def) {
        String name = def.getName();
        synchronized (antTypeTable) {
            rebuildTaskClassDefinitions = true;
            rebuildTypeClassDefinitions = true;
            AntTypeDefinition old = antTypeTable.getDefinition(name);
            if (old != null) {
                if (sameDefinition(def, old)) {
                    return;
                }
                Class oldClass = antTypeTable.getExposedClass(name);
                if (oldClass != null && Task.class.isAssignableFrom(oldClass)) {
                    int logLevel = Project.MSG_WARN;
                    if (def.getClassName().equals(old.getClassName())
                        && def.getClassLoader() == old.getClassLoader()) {
                        logLevel = Project.MSG_VERBOSE;
                    }
                    project.log(
                        "Trying to override old definition of task "
                        + name, logLevel);
                    invalidateCreatedTasks(name);
                } else {
                    project.log(
                        "Trying to override old definition of datatype "
                        + name, Project.MSG_WARN);
                }
            }
            project.log(" +Datatype " + name + " " + def.getClassName(),
                        Project.MSG_DEBUG);
            antTypeTable.put(name, def);
        }
    }

    /**
     * load ant's tasks
     */
    private void initTasks() {
        ClassLoader classLoader = null;
        if (project.getCoreLoader() != null
            && !("only".equals(project.getProperty("build.sysclasspath")))) {
            classLoader = project.getCoreLoader();
        }
        String dataDefs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        InputStream in = null;
        try {
            Properties props = new Properties();
            in = this.getClass().getResourceAsStream(dataDefs);
            if (in == null) {
                throw new BuildException("Can't load default task list");
            }
            props.load(in);

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String name = (String) enum.nextElement();
                String className = props.getProperty(name);
                AntTypeDefinition def = new AntTypeDefinition();
                def.setName(name);
                def.setClassName(className);
                def.setClassLoader(classLoader);
                def.setAdaptToClass(Task.class);
                def.setAdapterClass(TaskAdapter.class);
                antTypeTable.put(name, def);
            }
        } catch (IOException ex) {
            throw new BuildException("Can't load default type list");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                    // Ignore
                }
            }
        }
    }

    /**
     * load ant's datatypes
     */
    private void initTypes() {
        ClassLoader classLoader = null;
        if (project.getCoreLoader() != null
            && !("only".equals(project.getProperty("build.sysclasspath")))) {
            classLoader = project.getCoreLoader();
        }
        String dataDefs = "/org/apache/tools/ant/types/defaults.properties";

        InputStream in = null;
        try {
            Properties props = new Properties();
            in = this.getClass().getResourceAsStream(dataDefs);
            if (in == null) {
                throw new BuildException("Can't load default datatype list");
            }
            props.load(in);

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String name = (String) enum.nextElement();
                String className = props.getProperty(name);
                AntTypeDefinition def = new AntTypeDefinition();
                def.setName(name);
                def.setClassName(className);
                def.setClassLoader(classLoader);
                antTypeTable.put(name, def);
            }
        } catch (IOException ex) {
            throw new BuildException("Can't load default type list");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * map that contains the component definitions
     */
    private static class AntTypeTable extends Hashtable {
        private Project project;

        public AntTypeTable(Project project) {
            this.project = project;
        }

        public AntTypeDefinition getDefinition(String key) {
            AntTypeDefinition ret = (AntTypeDefinition) super.get(key);
            return ret;
        }

        /** Equivalent to getTypeType */
        public Object get(Object key) {
            return getTypeClass((String) key);
        }

        public Object create(String name) {
            AntTypeDefinition def = getDefinition(name);
            if (def == null) {
                return null;
            }
            return def.create(project);
        }

        public Class getTypeClass(String name) {
            AntTypeDefinition def = getDefinition(name);
            if (def == null) {
                return null;
            }
            return def.getTypeClass(project);
        }

        public Class getExposedClass(String name) {
            AntTypeDefinition def = getDefinition(name);
            if (def == null) {
                return null;
            }
            return def.getExposedClass(project);
        }

        public boolean contains(Object clazz) {
            for (Iterator i = values().iterator(); i.hasNext();) {
                AntTypeDefinition def = (AntTypeDefinition) i.next();
                Class c = def.getExposedClass(project);
                if (c == clazz) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsValue(Object value) {
            return contains(value);
        }
    }

}
