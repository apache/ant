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

import org.apache.tools.ant.util.LazyHashtable;
import org.apache.tools.ant.util.WeakishReference;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Component creation and configuration.
 *
 * This is cut&paste from Project.java of everything related to
 * task/type management. Project will just delegate.
 *
 * A very simple hook mechnism is provided that allows users to plug
 * in custom code. It is also possible to replace the default behavior
 * ( for example in an app embeding ant )
 *
 * @author Costin Manolache
 * @since Ant1.6
 */
public class ComponentHelper  {
    /** Map from data type names to implementing classes (String to Class). */
    private Hashtable dataClassDefinitions;
    /** Map from task names to implementing classes (String to Class). */
    private Hashtable taskClassDefinitions;
    /**
     * Map from task names to vectors of created tasks
     * (String to Vector of Task). This is used to invalidate tasks if
     * the task definition changes.
     */
    private Hashtable createdTasks = new Hashtable();


    protected ComponentHelper next;
    protected Project project;

    /**
     */
    public static ComponentHelper getComponentHelper(Project project) {
        // Singleton for now, it may change ( per/classloader )
        ComponentHelper ph=(ComponentHelper)project.getReference( "ant.ComponentHelper" );
        if( ph!=null ) return ph;
        ph=new ComponentHelper();
        ph.setProject( project );

        project.addReference( "ant.ComponentHelper",ph );
        return ph;
    }

    protected ComponentHelper() {
    }

    public void setNext( ComponentHelper next ) {
        this.next=next;
    }

    public ComponentHelper getNext() {
        return next;
    }

    public void setProject(Project project) {
        this.project = project;
        dataClassDefinitions= new AntTaskTable(project, false);
        taskClassDefinitions= new AntTaskTable(project, true);
    }

    /** Factory method to create the components.
     * 
     * This should be called by UnknownElement.
     * 
     * @param ue The component helper has access via ue to the entire XML tree.
     * @param ns Namespace. Also available as ue.getNamespace()
     * @param taskName The element name. Also available as ue.getTag()
     * @return
     * @throws BuildException
     */ 
    public Object createComponent( UnknownElement ue,
                                   String ns,
                                   String taskName )
            throws BuildException
    {
        Object component=null;

        // System.out.println("Fallback to project default " + taskName );
        // Can't create component. Default is to use the old methods in project.

        // This policy is taken from 1.5 ProjectHelper. In future the difference between
        // task and type should disapear.
        if( project.getDataTypeDefinitions().get(taskName) != null ) {
            // This is the original policy in ProjectHelper. The 1.5 version of UnkwnonwElement
            // used to try first to create a task, and if it failed tried a type. In 1.6 the diff
            // should disapear.
            component = this.createDataType(taskName);
            if( component!=null ) return component;
        }

        // from UnkwnonwElement.createTask. The 'top level' case is removed, we're
        // allways lazy
        component = this.createTask(taskName);

        return component;
    }

    /**
     * get the class of a particular component
     */
    public Class getComponentClass(String componentName) {
        Class elementClass =
            (Class) getTaskDefinitions().get(componentName);
        if (elementClass != null) {
            if (! (Task.class.isAssignableFrom(elementClass))) {
                elementClass = TaskAdapter.class;
            }
            return elementClass;
        }
        return (Class) getDataTypeDefinitions().get(componentName);
    }
    
    /**
     * create a named component
     */
    public Object createComponent(String componentName)
        throws BuildException
    {
        Object obj = createTask(componentName);
        if (obj == null) {
            obj = createDataType(componentName);
        }
        if (obj == null) {
            return obj;
        }
        project.setProjectReference(obj);
        if (obj instanceof Task) {
            ((Task)obj).init(); // Needed here ??
        }
        return obj;
    }

    /** Initialization code - implementing the original ant component
     * loading from /org/apache/tools/ant/taskdefs/default.properties 
     * and .../types/default.properties
     * 
     * @throws BuildException
     */ 
    public void initDefaultDefinitions() throws BuildException {
        String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        try {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream(defs);
            if (in == null) {
                throw new BuildException("Can't load default task list");
            }
            props.load(in);
            in.close();
            ((AntTaskTable)taskClassDefinitions).addDefinitions( props );


        } catch (IOException ioe) {
            throw new BuildException("Can't load default task list");
        }

        String dataDefs = "/org/apache/tools/ant/types/defaults.properties";

        try {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream(dataDefs);
            if (in == null) {
                throw new BuildException("Can't load default datatype list");
            }
            props.load(in);
            in.close();

            ((AntTaskTable)dataClassDefinitions).addDefinitions(props);


        } catch (IOException ioe) {
            throw new BuildException("Can't load default datatype list");
        }
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
    public void addTaskDefinition(String taskName, Class taskClass)   
            throws BuildException {
        Class old = (Class) taskClassDefinitions.get(taskName);
        if (null != old) {
            if (old.equals(taskClass)) {
//                project.log("Ignoring override for task " + taskName
//                        + ", it is already defined by the same class.",
//                        Project.MSG_VERBOSE);
                return;
            } else {
                int logLevel = Project.MSG_WARN;
                if (old.getName().equals(taskClass.getName())) {
                    ClassLoader oldLoader = old.getClassLoader();
                    ClassLoader newLoader = taskClass.getClassLoader();
                    // system classloader on older JDKs can be null
                    if (oldLoader != null
                            && newLoader != null
                            && oldLoader instanceof AntClassLoader
                            && newLoader instanceof AntClassLoader
                            && ((AntClassLoader) oldLoader).getClasspath()
                            .equals(((AntClassLoader) newLoader).getClasspath())
                    ) {
                        // same classname loaded from the same
                        // classpath components
                        logLevel = Project.MSG_VERBOSE;
                    }
                }

                project.log("Trying to override old definition of task " + taskName,
                        logLevel);
                invalidateCreatedTasks(taskName);
            }
        }

        String msg = " +User task: " + taskName + "     " + taskClass.getName();
        project.log(msg, Project.MSG_DEBUG);
        checkTaskClass(taskClass);
        taskClassDefinitions.put(taskName, taskClass);
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
        return taskClassDefinitions;
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
        synchronized(dataClassDefinitions) {
            Class old = (Class) dataClassDefinitions.get(typeName);
            if (null != old) {
                if (old.equals(typeClass)) {
//                    project.log("Ignoring override for datatype " + typeName
//                            + ", it is already defined by the same class.",
//                            Project.MSG_VERBOSE);
                    return;
                } else {
                    project.log("Trying to override old definition of datatype "
                            + typeName, Project.MSG_WARN);
                }
            }
            dataClassDefinitions.put(typeName, typeClass);
        }
        String msg = " +User datatype: " + typeName + "     "
                + typeClass.getName();
        project.log(msg, Project.MSG_DEBUG);
    }

    /**
     * Returns the current datatype definition hashtable. The returned
     * hashtable is "live" and so should not be modified.
     *
     * @return a map of from datatype name to implementing class
     *         (String to Class).
     */
    public Hashtable getDataTypeDefinitions() {
        return dataClassDefinitions;
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
        Task task=createNewTask(taskType);
        if(task!=null) {
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
        Class c = (Class) taskClassDefinitions.get(taskType);

        if (c == null) {
            return null;
        }

        try {
            Object o = c.newInstance();
            if ( project != null ) {
                project.setProjectReference( o );
            }
            Task task = null;
            if (o instanceof Task) {
                task = (Task) o;
            } else {
                // "Generic" Bean - use the setter pattern
                // and an Adapter
                TaskAdapter taskA = new TaskAdapter();
                taskA.setProxy(o);
                if ( project != null ) {
                    project.setProjectReference( taskA );
                }
                task = taskA;
            }
            task.setProject( project );
            task.setTaskType(taskType);

            // set default value, can be changed by the user
            task.setTaskName(taskType);

            String msg = "   +Task: " + taskType;
            project.log (msg, Project.MSG_DEBUG);
            return task;
        } catch (Throwable t) {
            System.out.println("task CL=" + c.getClassLoader());
            String msg = "Could not create task of type: "
                    + taskType + " due to " + t;
            throw new BuildException(msg, t);
        }
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
                Enumeration enum = v.elements();
                while (enum.hasMoreElements()) {
                    WeakishReference ref=
                            (WeakishReference) enum.nextElement();
                    Task t = (Task) ref.get();
                    //being a weak ref, it may be null by this point
                    if(t!=null) {
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
        Class c = (Class) dataClassDefinitions.get(typeName);

        if (c == null) {
            return null;
        }

        try {
            java.lang.reflect.Constructor ctor = null;
            boolean noArg = false;
            // DataType can have a "no arg" constructor or take a single
            // Project argument.
            try {
                ctor = c.getConstructor(new Class[0]);
                noArg = true;
            } catch (NoSuchMethodException nse) {
                ctor = c.getConstructor(new Class[] {Project.class});
                noArg = false;
            }

            Object o = null;
            if (noArg) {
                o = ctor.newInstance(new Object[0]);
            } else {
                o = ctor.newInstance(new Object[] {project});
            }
            if ( project != null ) {
                project.setProjectReference( o );
            }
            String msg = "   +DataType: " + typeName;
            project.log(msg, Project.MSG_DEBUG);
            return o;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            String msg = "Could not create datatype of type: "
                    + typeName + " due to " + t;
            throw new BuildException(msg, t);
        } catch (Throwable t) {
            String msg = "Could not create datatype of type: "
                    + typeName + " due to " + t;
            throw new BuildException(msg, t);
        }
    }

    /**
     * Returns a description of the type of the given element, with
     * special handling for instances of tasks and data types.
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
        Hashtable elements = taskClassDefinitions;
        Class elementClass = element.getClass();
        String typeName = "task";
        if (!elements.contains(elementClass)) {
            elements = dataClassDefinitions;
            typeName = "data type";
            if (!elements.contains(elementClass)) {
                elements = null;
            }
        }

        if (elements != null) {
            Enumeration e = elements.keys();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                Class clazz = (Class) elements.get(name);
                if (elementClass.equals(clazz)) {
                    return "The <" + name + "> " + typeName;
                }
            }
        }

        return "Class " + elementClass.getName();
    }


    private static class AntTaskTable extends LazyHashtable {
        Project project;
        Properties props;
        boolean tasks=false;

        public AntTaskTable( Project p, boolean tasks ) {
            this.project=p;
            this.tasks=tasks;
        }

        public void addDefinitions( Properties props ) {
            this.props=props;
        }

        protected void initAll( ) {
            if( initAllDone ) {
                return;
            }
            project.log("InitAll", Project.MSG_DEBUG);
            if( props==null ) {
                return;
            }
            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                Class taskClass=getTask( key );
                if( taskClass!=null ) {
                    // This will call a get() and a put()
                    if( tasks ) {
                        project.addTaskDefinition(key, taskClass);
                    } else {
                        project.addDataTypeDefinition(key, taskClass );
                    }
                }
            }
            initAllDone=true;
        }

        protected Class getTask(String key) {
            if( props==null ) {
                return null; // for tasks loaded before init()
            }
            String value=props.getProperty(key);
            if( value==null) {
                //project.log( "No class name for " + key, Project.MSG_VERBOSE );
                return null;
            }
            try {
                Class taskClass=null;
                if( project.getCoreLoader() != null &&
                    !("only".equals(project.getProperty("build.sysclasspath")))) {
                    try {
                        project.log("Loading with the core loader " + value,
                                Project.MSG_DEBUG);
                        taskClass=project.getCoreLoader().loadClass(value);
                        if( taskClass != null ) {
                            return taskClass;
                        }
                    } catch( Exception ex ) {
                        //ignore
                    }
                }
                taskClass = Class.forName(value);
                return taskClass;
            } catch (NoClassDefFoundError ncdfe) {
                project.log("Could not load a dependent class ("
                        + ncdfe.getMessage() + ") for task " 
                        + key, Project.MSG_DEBUG);
            } catch (ClassNotFoundException cnfe) {
                project.log("Could not load class (" + value
                        + ") for task " + key, Project.MSG_DEBUG);
            }
            return null;
        }

        // Hashtable implementation
        public Object get( Object key ) {
            Object orig=super.get( key );
            if( orig!= null ) {
                return orig;
            }
            if( ! (key instanceof String) ) {
                return null;
            }
            project.log("Get task " + key, Project.MSG_DEBUG );
            Object taskClass=getTask( (String) key);
            if( taskClass != null) {
                super.put( key, taskClass );
            }
            return taskClass;
        }

        public boolean containsKey( Object key ) {
            return get( key ) != null;
        }

    }
}
