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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Stack;
import java.lang.reflect.Modifier;


import org.apache.tools.ant.types.FilterSet; 
import org.apache.tools.ant.types.FilterSetCollection; 
import org.apache.tools.ant.util.FileUtils; 
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;

/**
 * Central representation of an Ant project. This class defines an
 * Ant project with all of its targets, tasks and various other 
 * properties. It also provides the mechanism to kick off a build using 
 * a particular target name.
 * <p>
 * This class also encapsulates methods which allow files to be referred
 * to using abstract path names which are translated to native system
 * file paths at runtime.
 *
 * @author duncan@x180.com
 *
 * @version $Revision$
 */

public class Project {

    /** Message priority of "error". */
    public static final int MSG_ERR = 0;
    /** Message priority of "warning". */
    public static final int MSG_WARN = 1;
    /** Message priority of "information". */
    public static final int MSG_INFO = 2;
    /** Message priority of "verbose". */
    public static final int MSG_VERBOSE = 3;
    /** Message priority of "debug". */
    public static final int MSG_DEBUG = 4;

    /** 
     * Constant for the "visiting" state, used when
     * traversing a DFS of target dependencies.
     */
    private static final String VISITING = "VISITING";
    /** 
     * Constant for the "visited" state, used when
     * traversing a DFS of target dependencies.
     */
    private static final String VISITED = "VISITED";

    /** Version constant for Java 1.0 */
    public static final String JAVA_1_0 = JavaEnvUtils.JAVA_1_0;
    /** Version constant for Java 1.1 */
    public static final String JAVA_1_1 = JavaEnvUtils.JAVA_1_1;
    /** Version constant for Java 1.2 */
    public static final String JAVA_1_2 = JavaEnvUtils.JAVA_1_2;
    /** Version constant for Java 1.3 */
    public static final String JAVA_1_3 = JavaEnvUtils.JAVA_1_3;
    /** Version constant for Java 1.4 */
    public static final String JAVA_1_4 = JavaEnvUtils.JAVA_1_4;

    /** Default filter start token. */
    public static final String TOKEN_START = FilterSet.DEFAULT_TOKEN_START;
    /** Default filter end token. */
    public static final String TOKEN_END = FilterSet.DEFAULT_TOKEN_END;

    /** Name of this project. */
    private String name;
    /** Description for this project (if any). */
    private String description;

    /** Project properties map (String to String). */
    private Hashtable properties = new Hashtable();
    /** 
     * Map of "user" properties (as created in the Ant task, for example). 
     * Note that these key/value pairs are also always put into the
     * project properties, so only the project properties need to be queried.
     * Mapping is String to String.
     */
    private Hashtable userProperties = new Hashtable();
    /** Map of references within the project (paths etc) (String to Object). */
    private Hashtable references = new Hashtable();
    
    /** Name of the project's default target. */
    private String defaultTarget;
    /** Map from data type names to implementing classes (String to Class). */
    private Hashtable dataClassDefinitions = new Hashtable();
    /** Map from task names to implementing classes (String to Class). */
    private Hashtable taskClassDefinitions = new Hashtable();
    /** 
     * Map from task names to vectors of created tasks 
     * (String to Vector of Task). This is used to invalidate tasks if
     * the task definition changes.
     */
    private Hashtable createdTasks = new Hashtable();
    /** Map from target names to targets (String to Target). */
    private Hashtable targets = new Hashtable();
    /** Set of global filters. */
    private FilterSet globalFilterSet = new FilterSet();
    /** 
     * Wrapper around globalFilterSet. This collection only ever
     * contains one FilterSet, but the wrapper is needed in order to
     * make it easier to use the FileUtils interface.
     */
    private FilterSetCollection globalFilters 
        = new FilterSetCollection(globalFilterSet);
        
    /** Project base directory. */
    private File baseDir;

    /** List of listeners to notify of build events. */
    private Vector listeners = new Vector();

    /** 
     * The Ant core classloader - may be <code>null</code> if using 
     * parent classloader.
     */    
    private ClassLoader coreLoader = null;

    /** Records the latest task to be executed on a thread (Thread to Task). */ 
    private Hashtable threadTasks = new Hashtable();
    
    /**
     * Called to handle any input requests.
     */
    private InputHandler inputHandler = null;

    /**
     * Sets the input handler
     */
    public void setInputHandler(InputHandler handler) {
        inputHandler = handler;
    }

    /**
     * Retrieves the current input handler.
     */
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /** Instance of a utility class to use for file operations. */
    private FileUtils fileUtils;

    /**
     * Creates a new Ant project.
     */
    public Project() {
        fileUtils = FileUtils.newFileUtils();
        inputHandler = new DefaultInputHandler();
    }
    
    /**
     * Initialises the project.
     *
     * This involves setting the default task definitions and loading the
     * system properties.
     * 
     * @exception BuildException if the default task list cannot be loaded
     */
    public void init() throws BuildException {
        setJavaVersionProperty();
        
        String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        try {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream(defs);
            if (in == null) { 
                throw new BuildException("Can't load default task list");
            }
            props.load(in);
            in.close();

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                String value = props.getProperty(key);
                try {
                    Class taskClass = Class.forName(value);
                    addTaskDefinition(key, taskClass);
                } catch (NoClassDefFoundError ncdfe) {
                    log("Could not load a dependent class (" 
                        + ncdfe.getMessage() + ") for task " + key, MSG_DEBUG); 
                } catch (ClassNotFoundException cnfe) {
                    log("Could not load class (" + value 
                        + ") for task " + key, MSG_DEBUG); 
                }
            }
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

            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                String value = props.getProperty(key);
                try {
                    Class dataClass = Class.forName(value);
                    addDataTypeDefinition(key, dataClass);
                } catch (NoClassDefFoundError ncdfe) {
                    // ignore...
                } catch (ClassNotFoundException cnfe) {
                    // ignore...
                }
            }
        } catch (IOException ioe) {
            throw new BuildException("Can't load default datatype list");
        }

        setSystemProperties();
    }

    /** 
     * Sets the core classloader for the project. If a <code>null</code>
     * classloader is specified, the parent classloader should be used.
     * 
     * @param coreLoader The classloader to use for the project.
     *                   May be <code>null</code>.
     */
    public void setCoreLoader(ClassLoader coreLoader) {
        this.coreLoader = coreLoader;
    }
    
    /** 
     * Returns the core classloader to use for this project.
     * This may be <code>null</code>, indicating that
     * the parent classloader should be used.
     * 
     * @return the core classloader to use for this project.
     *         
     */
    public ClassLoader getCoreLoader() {
        return coreLoader;
    }
    
    /**
     * Adds a build listener to the list. This listener will
     * be notified of build events for this project.
     * 
     * @param listener The listener to add to the list.
     *                 Must not be <code>null</code>.
     */
    public void addBuildListener(BuildListener listener) {
        listeners.addElement(listener);
    }

    /**
     * Removes a build listener from the list. This listener
     * will no longer be notified of build events for this project.
     * 
     * @param listener The listener to remove from the list.
     *                 Should not be <code>null</code>.
     */
    public void removeBuildListener(BuildListener listener) {
        listeners.removeElement(listener);
    }

    /**
     * Returns a list of build listeners for the project. The returned
     * vector is "live" and so should not be modified.
     * 
     * @return a list of build listeners for the project
     */
    public Vector getBuildListeners() {
        return listeners;
    }

    /**
     * Writes a message to the log with the default log level
     * of MSG_INFO
     * @param msg The text to log. Should not be <code>null</code>.
     */
     
    public void log(String msg) {
        log(msg, MSG_INFO);
    }

    /**
     * Writes a project level message to the log with the given log level.
     * @param msg The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log(String msg, int msgLevel) {
        fireMessageLogged(this, msg, msgLevel);
    }

    /**
     * Writes a task level message to the log with the given log level.
     * @param task The task to use in the log. Must not be <code>null</code>.
     * @param msg The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log(Task task, String msg, int msgLevel) {
        fireMessageLogged(task, msg, msgLevel);
    }
    
    /**
     * Writes a target level message to the log with the given log level.
     * @param target The target to use in the log.
     *               Must not be <code>null</code>.
     * @param msg The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log(Target target, String msg, int msgLevel) {
        fireMessageLogged(target, msg, msgLevel);
    }

    /**
     * Returns the set of global filters.
     * 
     * @return the set of global filters
     */
    public FilterSet getGlobalFilterSet() {
        return globalFilterSet;
    }
    
    /**
     * Sets a property. Any existing property of the same name 
     * is overwritten, unless it is a user property. 
     * @param name The name of property to set. 
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public void setProperty(String name, String value) {
        // command line properties take precedence
        if (null != userProperties.get(name)) {
            log("Override ignored for user property " + name, MSG_VERBOSE);
            return;
        }

        if (null != properties.get(name)) {
            log("Overriding previous definition of property " + name, 
                MSG_VERBOSE);
        }

        log("Setting project property: " + name + " -> " +
             value, MSG_DEBUG);
        properties.put(name, value);
    }

    /**
     * Sets a property if no value currently exists. If the property
     * exists already, a message is logged and the method returns with
     * no other effect.
     * 
     * @param name The name of property to set. 
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @since 1.5
     */
    public void setNewProperty(String name, String value) {
        if (null != properties.get(name)) {
            log("Override ignored for property " + name, MSG_VERBOSE);
            return;
        }
        log("Setting project property: " + name + " -> " +
            value, MSG_DEBUG);
        properties.put(name, value);
    }

    /**
     * Sets a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     * @param name The name of property to set. 
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @see #setProperty(String,String)
     */
    public void setUserProperty(String name, String value) {
        log("Setting ro project property: " + name + " -> " +
            value, MSG_DEBUG);
        userProperties.put(name, value);
        properties.put(name, value);
    }
    
    /**
     * Sets a property unless it is already defined as a user property
     * (in which case the method returns silently).
     *
     * @param name The name of the property. 
     *             Must not be <code>null</code>.
     * @param value The property value. Must not be <code>null</code>.
     */
    private void setPropertyInternal(String name, String value) {
        if (null != userProperties.get(name)) {
            return;
        }
        properties.put(name, value);
    }

    /**
     * Returns the value of a property, if it is set.
     * 
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public String getProperty(String name) {
        if (name == null) {
          return null;
        }
        String property = (String) properties.get(name);
        return property;
    }

    /**
     * Replaces ${} style constructions in the given value with the
     * string value of the corresponding data types.
     *
     * @param value The string to be scanned for property references.
     *              May be <code>null</code>.
     * 
     * @return the given string with embedded property names replaced
     *         by values, or <code>null</code> if the given string is
     *         <code>null</code>.
     * 
     * @exception BuildException if the given value has an unclosed 
     *                           property name, e.g. <code>${xxx</code>
     */
    public String replaceProperties(String value)
        throws BuildException { 
        return ProjectHelper.replaceProperties(this, value, properties);
    }

    /**
     * Returns the value of a user property, if it is set.
     * 
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
     public String getUserProperty(String name) {
        if (name == null) {
          return null;
        }
        String property = (String) userProperties.get(name);
        return property;
    }

    /**
     * Returns a copy of the properties table.
     * @return a hashtable containing all properties 
     *         (including user properties).
     */
    public Hashtable getProperties() {
        Hashtable propertiesCopy = new Hashtable();
        
        Enumeration e = properties.keys();
        while (e.hasMoreElements()) {
            Object name = e.nextElement();
            Object value = properties.get(name);
            propertiesCopy.put(name, value);
        }
        
        return propertiesCopy;
    }

    /**
     * Returns a copy of the user property hashtable
     * @return a hashtable containing just the user properties
     */
    public Hashtable getUserProperties() {
        Hashtable propertiesCopy = new Hashtable();
        
        Enumeration e = userProperties.keys();
        while (e.hasMoreElements()) {
            Object name = e.nextElement();
            Object value = properties.get(name);
            propertiesCopy.put(name, value);
        }
        
        return propertiesCopy;
    }

    /**
     * Sets the default target of the project.
     * 
     * @param defaultTarget The name of the default target for this project.
     *                      May be <code>null</code>, indicating that there is
     *                      no default target.
     * 
     * @deprecated use setDefault
     * @see #setDefault(String)
     */
    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    /**
     * Returns the name of the default target of the project.
     * @return name of the default target or 
     *         <code>null</code> if no default has been set.
     */
    public String getDefaultTarget() {
        return defaultTarget;
    }
    
    /**
     * Sets the default target of the project.
     * 
     * @param defaultTarget The name of the default target for this project.
     *                      May be <code>null</code>, indicating that there is
     *                      no default target.
     */
    public void setDefault(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    /**
     * Sets the name of the project, also setting the user
     * property <code>ant.project.name</code>.
     * 
     * @param name The name of the project.
     *             Must not be <code>null</code>.
     */
    public void setName(String name) {
        setUserProperty("ant.project.name",  name);
        this.name = name;
    }

    /** 
     * Returns the project name, if one has been set.
     * 
     * @return the project name, or <code>null</code> if it hasn't been set.
     */
    public String getName() {
        return name;
    }

    /** 
     * Sets the project description.
     * 
     * @param description The description of the project. 
     *                    May be <code>null</code>.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 
     * Returns the project description, if one has been set.
     * 
     * @return the project description, or <code>null</code> if it hasn't 
     *         been set.
     */
    public String getDescription() {
        return description;
    }

    /** 
     * Adds a filter to the set of global filters.
     * 
     * @param token The token to filter.
     *              Must not be <code>null</code>.
     * @param value The replacement value.
     *              Must not be <code>null</code>.
     * @deprecated Use getGlobalFilterSet().addFilter(token,value)
     * 
     * @see #getGlobalFilterSet()
     * @see FilterSet#addFilter(String,String)
     */
    public void addFilter(String token, String value) {
        if (token == null) {
            return;
        }
 
        globalFilterSet.addFilter(new FilterSet.Filter(token, value));
    }

    /** 
     * Returns a hashtable of global filters, mapping tokens to values.
     * 
     * @return a hashtable of global filters, mapping tokens to values 
     *         (String to String).
     * 
     * @deprecated Use getGlobalFilterSet().getFilterHash()
     * 
     * @see #getGlobalFilterSet()
     * @see FilterSet#getFilterHash()
     */
    public Hashtable getFilters() {
        // we need to build the hashtable dynamically
        return globalFilterSet.getFilterHash();
    }

    /**
     * Sets the base directory for the project, checking that
     * the given filename exists and is a directory.
     * 
     * @param baseD The project base directory.
     *              Must not be <code>null</code>.
     * 
     * @exception BuildException if the directory if invalid
     */
    public void setBasedir(String baseD) throws BuildException {
        setBaseDir(new File(baseD));
    }

    /**
     * Sets the base directory for the project, checking that
     * the given file exists and is a directory.
     * 
     * @param baseDir The project base directory.
     *                Must not be <code>null</code>.
     * @exception BuildException if the specified file doesn't exist or 
     *                           isn't a directory
     */
    public void setBaseDir(File baseDir) throws BuildException {
        baseDir = fileUtils.normalize(baseDir.getAbsolutePath());
        if (!baseDir.exists()) { 
            throw new BuildException("Basedir " + baseDir.getAbsolutePath() 
                + " does not exist");
        }
        if (!baseDir.isDirectory()) { 
            throw new BuildException("Basedir " + baseDir.getAbsolutePath() 
                + " is not a directory");
        }
        this.baseDir = baseDir;
        setPropertyInternal("basedir", this.baseDir.getPath());
        String msg = "Project base dir set to: " + this.baseDir;
        log(msg, MSG_VERBOSE);
    }

    /**
     * Returns the base directory of the project as a file object.
     * 
     * @return the project base directory, or <code>null</code> if the
     *         base directory has not been successfully set to a valid value.
     */
    public File getBaseDir() {
        if (baseDir == null) {
            try {
                setBasedir(".");
            } catch (BuildException ex) {
                ex.printStackTrace();
            }
        }
        return baseDir;
    }

    /**
     * Returns the version of Java this class is running under.
     * @return the version of Java as a String, e.g. "1.1"
     * @see org.apache.tools.ant.util.JavaEnvUtils#getJavaVersion
     */
    public static String getJavaVersion() {
        return JavaEnvUtils.getJavaVersion();
    }

    /**
     * Sets the <code>ant.java.version</code> property and tests for
     * unsupported JVM versions. If the version is supported,
     * verbose log messages are generated to record the Java version
     * and operating system name.
     *
     * @exception BuildException if this Java version is not supported
     * 
     * @see org.apache.tools.ant.util.JavaEnvUtils#getJavaVersion
     */
    public void setJavaVersionProperty() throws BuildException {
        String javaVersion = JavaEnvUtils.getJavaVersion();
        setPropertyInternal("ant.java.version", javaVersion);

        // sanity check
        if (javaVersion == JavaEnvUtils.JAVA_1_0) {
            throw new BuildException("Ant cannot work on Java 1.0");
        }

        log("Detected Java version: " + javaVersion + " in: " 
            + System.getProperty("java.home"), MSG_VERBOSE);

        log("Detected OS: " + System.getProperty("os.name"), MSG_VERBOSE);
    }

    /**
     * Adds all system properties which aren't already defined as
     * user properties to the project properties.
     */
    public void setSystemProperties() {
        Properties systemP = System.getProperties();
        Enumeration e = systemP.keys();
        while (e.hasMoreElements()) {
            Object name = e.nextElement();
            String value = systemP.get(name).toString();
            this.setPropertyInternal(name.toString(), value);
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
                log("Ignoring override for task " + taskName 
                    + ", it is already defined by the same class.", 
                    MSG_VERBOSE);
                return;
            } else {
                log("Trying to override old definition of task " + taskName, 
                    MSG_WARN);
                invalidateCreatedTasks(taskName);
            }
        }

        String msg = " +User task: " + taskName + "     " + taskClass.getName();
        log(msg, MSG_DEBUG);
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
            log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        if (Modifier.isAbstract(taskClass.getModifiers())) {
            final String message = taskClass + " is abstract";
            log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        try {
            taskClass.getConstructor(null);
            // don't have to check for public, since
            // getConstructor finds public constructors only.
        } catch (NoSuchMethodException e) {
            final String message = "No public no-arg constructor in " 
                + taskClass;
            log(message, Project.MSG_ERR);
            throw new BuildException(message);
        }
        if (!Task.class.isAssignableFrom(taskClass)) {
            TaskAdapter.checkTaskClass(taskClass, this);
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
        Class old = (Class) dataClassDefinitions.get(typeName);
        if (null != old) {
            if (old.equals(typeClass)) {
                log("Ignoring override for datatype " + typeName 
                    + ", it is already defined by the same class.", 
                    MSG_VERBOSE);
                return;
            } else {
                log("Trying to override old definition of datatype " 
                    + typeName, MSG_WARN);
            }
        }

        String msg = " +User datatype: " + typeName + "     " 
            + typeClass.getName();
        log(msg, MSG_DEBUG);
        dataClassDefinitions.put(typeName, typeClass);
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
     * Adds a <em>new</em> target to the project.
     * 
     * @param target The target to be added to the project.
     *               Must not be <code>null</code>.
     * 
     * @exception BuildException if the target already exists in the project
     * 
     * @see Project#addOrReplaceTarget
     */
    public void addTarget(Target target) throws BuildException {
        String name = target.getName();
        if (targets.get(name) != null) {
            throw new BuildException("Duplicate target: `" + name + "'");
        }
        addOrReplaceTarget(name, target);
    }

    /**
     * Adds a <em>new</em> target to the project.
     *
     * @param targetName The name to use for the target.
     *             Must not be <code>null</code>.
     * @param target The target to be added to the project.
     *               Must not be <code>null</code>.
     * 
     * @exception BuildException if the target already exists in the project
     * 
     * @see Project#addOrReplaceTarget
     */
     public void addTarget(String targetName, Target target)
         throws BuildException {
         if (targets.get(targetName) != null) {
             throw new BuildException("Duplicate target: `" + targetName + "'");
         }
         addOrReplaceTarget(targetName, target);
     }

    /**
     * Adds a target to the project, or replaces one with the same
     * name.
     * 
     * @param target The target to be added or replaced in the project.
     *               Must not be <code>null</code>.
     */
    public void addOrReplaceTarget(Target target) {
        addOrReplaceTarget(target.getName(), target);
    }

    /**
     * Adds a target to the project, or replaces one with the same
     * name.
     * 
     * @param targetName The name to use for the target.
     *                   Must not be <code>null</code>.
     * @param target The target to be added or replaced in the project.
     *               Must not be <code>null</code>.
     */
    public void addOrReplaceTarget(String targetName, Target target) {
        String msg = " +Target: " + targetName;
        log(msg, MSG_DEBUG);
        target.setProject(this);
        targets.put(targetName, target);
    }

    /**
     * Returns the hashtable of targets. The returned hashtable 
     * is "live" and so should not be modified.
     * @return a map from name to target (String to Target). 
     */
    public Hashtable getTargets() {
        return targets;
    }

    /**
     * Creates a new instance of a task.
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
        Class c = (Class) taskClassDefinitions.get(taskType);

        if (c == null) {
            return null;
        }
        
        try {
            Object o = c.newInstance();
            Task task = null;
            if (o instanceof Task) {
               task = (Task) o;
            } else {
                // "Generic" Bean - use the setter pattern
                // and an Adapter
                TaskAdapter taskA = new TaskAdapter();
                taskA.setProxy(o);
                task = taskA;
            }
            task.setProject(this);
            task.setTaskType(taskType);

            // set default value, can be changed by the user
            task.setTaskName(taskType);

            String msg = "   +Task: " + taskType;
            log (msg, MSG_DEBUG);
            addCreatedTask(taskType, task);
            return task;
        } catch (Throwable t) {
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
            v.addElement(task);
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
                    Task t = (Task) enum.nextElement();
                    t.markInvalid();
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
                 o = ctor.newInstance(new Object[] {this});
            }
            if (o instanceof ProjectComponent) {
                ((ProjectComponent) o).setProject(this);
            }
            String msg = "   +DataType: " + typeName;
            log (msg, MSG_DEBUG);
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
     * Execute the specified sequence of targets, and the targets 
     * they depend on.
     * 
     * @param targetNames A vector of target name strings to execute.
     *                    Must not be <code>null</code>.
     * 
     * @exception BuildException if the build failed
     */
    public void executeTargets(Vector targetNames) throws BuildException {
        Throwable error = null;

        for (int i = 0; i < targetNames.size(); i++) {
            executeTarget((String) targetNames.elementAt(i));
        }
    }

    /**
     * Demultiplexes output so that each task receives the appropriate
     * messages. If the current thread is not currently executing a task,
     * the message is logged directly.
     * 
     * @param line Message to handle. Should not be <code>null</code>.
     * @param isError Whether the text represents an error (<code>true</code>)
     *        or information (<code>false</code>).
     */
    public void demuxOutput(String line, boolean isError) {
        Task task = (Task) threadTasks.get(Thread.currentThread());
        if (task == null) {
            fireMessageLogged(this, line, isError ? MSG_ERR : MSG_INFO);
        } else {
            if (isError) {
                task.handleErrorOutput(line);
            } else {
                task.handleOutput(line);
            }
        }
    }
    
    /**
     * Executes the specified target and any targets it depends on.
     * 
     * @param targetName The name of the target to execute. 
     *                   Must not be <code>null</code>.
     * 
     * @exception BuildException if the build failed
     */
    public void executeTarget(String targetName) throws BuildException {

        // sanity check ourselves, if we've been asked to build nothing
        // then we should complain

        if (targetName == null) {
            String msg = "No target specified";
            throw new BuildException(msg);
        }

        // Sort the dependency tree, and run everything from the
        // beginning until we hit our targetName.
        // Sorting checks if all the targets (and dependencies)
        // exist, and if there is any cycle in the dependency
        // graph.
        Vector sortedTargets = topoSort(targetName, targets);

        int curidx = 0;
        Target curtarget;

        do {
            curtarget = (Target) sortedTargets.elementAt(curidx++);
            curtarget.performTasks();
        } while (!curtarget.getName().equals(targetName));
    }

    /**
     * Returns the canonical form of a filename.
     * <p>
     * If the specified file name is relative it is resolved
     * with respect to the given root directory.
     *
     * @param fileName The name of the file to resolve. 
     *                 Must not be <code>null</code>.
     * 
     * @param rootDir  The directory to resolve relative file names with 
     *                 respect to. May be <code>null</code>, in which case
     *                 the current directory is used.
     *
     * @return the resolved File. 
     * 
     * @deprecated
     */
    public File resolveFile(String fileName, File rootDir) {
        return fileUtils.resolveFile(rootDir, fileName);
    }

    /**
     * Returns the canonical form of a filename.
     * <p>
     * If the specified file name is relative it is resolved
     * with respect to the project's base directory.
     *
     * @param fileName The name of the file to resolve. 
     *                 Must not be <code>null</code>.
     *
     * @return the resolved File. 
     * 
     */
    public File resolveFile(String fileName) {
        return fileUtils.resolveFile(baseDir, fileName);
    }

    /**
     * Translates a path into its native (platform specific) format. 
     * <p>
     * This method uses PathTokenizer to separate the input path
     * into its components. This handles DOS style paths in a relatively
     * sensible way. The file separators are then converted to their platform
     * specific versions.
     *
     * @param toProcess The path to be translated.
     *                  May be <code>null</code>.
     *
     * @return the native version of the specified path or 
     *         an empty string if the path is <code>null</code> or empty.
     * 
     * @see PathTokenizer
     */
    public static String translatePath(String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            return "";
        }

        StringBuffer path = new StringBuffer(toProcess.length() + 50);
        PathTokenizer tokenizer = new PathTokenizer(toProcess);
        while (tokenizer.hasMoreTokens()) {
            String pathComponent = tokenizer.nextToken();
            pathComponent = pathComponent.replace('/', File.separatorChar);
            pathComponent = pathComponent.replace('\\', File.separatorChar);
            if (path.length() != 0) {
                path.append(File.pathSeparatorChar);
            }
            path.append(pathComponent);
        }
        
        return path.toString();
    }

    /**
     * Convenience method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * 
     * @exception IOException if the copying fails
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile) 
          throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convenience method to copy a file from a source to a destination
     * specifying if token filtering should be used.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filtering Whether or not token filtering should be used during
     *                  the copy.
     * 
     * @exception IOException if the copying fails
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, 
            filtering ? globalFilters : null);
    }

    /**
     * Convenience method to copy a file from a source to a
     * destination specifying if token filtering should be used and if
     * source files may overwrite newer destination files.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filtering Whether or not token filtering should be used during
     *                  the copy.
     * @param overwrite Whether or not the destination file should be 
     *                  overwritten if it already exists.
     * 
     * @exception IOException if the copying fails
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile, 
            filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convenience method to copy a file from a source to a
     * destination specifying if token filtering should be used, if
     * source files may overwrite newer destination files, and if the
     * last modified time of the resulting file should be set to
     * that of the source file.
     *
     * @param sourceFile Name of file to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile Name of file to copy to.
     *                 Must not be <code>null</code>.
     * @param filtering Whether or not token filtering should be used during
     *                  the copy.
     * @param overwrite Whether or not the destination file should be 
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * 
     * @exception IOException if the copying fails
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, 
            filtering ? globalFilters : null, overwrite, preserveLastModified);
    }

    /**
     * Convenience method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @param sourceFile File to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile File to copy to.
     *                 Must not be <code>null</code>.
     * 
     * @exception IOException if the copying fails
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convenience method to copy a file from a source to a destination
     * specifying if token filtering should be used.
     *
     * @param sourceFile File to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile File to copy to.
     *                 Must not be <code>null</code>.
     * @param filtering Whether or not token filtering should be used during
     *                  the copy.
     *
     * @exception IOException if the copying fails
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, 
            filtering ? globalFilters : null);
    }

    /**
     * Convenience method to copy a file from a source to a
     * destination specifying if token filtering should be used and if
     * source files may overwrite newer destination files.
     *
     * @param sourceFile File to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile File to copy to.
     *                 Must not be <code>null</code>.
     * @param filtering Whether or not token filtering should be used during
     *                  the copy.
     * @param overwrite Whether or not the destination file should be 
     *                  overwritten if it already exists.
     * 
     * @exception IOException if the file cannot be copied.
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile, 
            filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convenience method to copy a file from a source to a
     * destination specifying if token filtering should be used, if
     * source files may overwrite newer destination files, and if the
     * last modified time of the resulting file should be set to
     * that of the source file.
     *
     * @param sourceFile File to copy from.
     *                   Must not be <code>null</code>.
     * @param destFile File to copy to.
     *                 Must not be <code>null</code>.
     * @param filtering Whether or not token filtering should be used during
     *                  the copy.
     * @param overwrite Whether or not the destination file should be 
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the resulting file should be set to that
     *                             of the source file.
     * 
     * @exception IOException if the file cannot be copied.
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, 
            filtering ? globalFilters : null, overwrite, preserveLastModified);
    }

    /**
     * Calls File.setLastModified(long time) on Java above 1.1, and logs
     * a warning on Java 1.1.
     * 
     * @param file The file to set the last modified time on.
     *             Must not be <code>null</code>.
     *
     * @param time the required modification time.
     * 
     * @deprecated
     * 
     * @exception BuildException if the last modified time cannot be set
     *                           despite running on a platform with a version 
     *                           above 1.1.
     */
    public void setFileLastModified(File file, long time) 
         throws BuildException {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            log("Cannot change the modification time of " + file
                + " in JDK 1.1", Project.MSG_WARN);
            return;
        }
        fileUtils.setFileLastModified(file, time);
        log("Setting modification time for " + file, MSG_VERBOSE);
    }

    /**
     * Returns the boolean equivalent of a string, which is considered 
     * <code>true</code> if either <code>"on"</code>, <code>"true"</code>, 
     * or <code>"yes"</code> is found, ignoring case.
     * 
     * @param s The string to convert to a boolean value. 
     *          Must not be <code>null</code>.
     * 
     * @return <code>true</code> if the given string is <code>"on"</code>,
     *         <code>"true"</code> or <code>"yes"</code>, or
     *         <code>false</code> otherwise.
     */
    public static boolean toBoolean(String s) {
        return (s.equalsIgnoreCase("on") ||
                s.equalsIgnoreCase("true") ||
                s.equalsIgnoreCase("yes"));
    }

    /**
     * Topologically sorts a set of targets.
     * 
     * @param root The name of the root target. The sort is created in such 
     *             a way that the sequence of Targets up to the root
     *             target is the minimum possible such sequence.
     *             Must not be <code>null</code>.
     * @param targets A map of names to targets (String to Target).
     *                Must not be <code>null</code>.
     * @return a vector of strings with the names of the targets in
     *         sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     */
    public final Vector topoSort(String root, Hashtable targets)
        throws BuildException {
        Vector ret = new Vector();
        Hashtable state = new Hashtable();
        Stack visiting = new Stack();

        // We first run a DFS based sort using the root as the starting node.
        // This creates the minimum sequence of Targets to the root node.
        // We then do a sort on any remaining unVISITED targets.
        // This is unnecessary for doing our build, but it catches
        // circular dependencies or missing Targets on the entire
        // dependency tree, not just on the Targets that depend on the
        // build Target.

        tsort(root, targets, state, visiting, ret);
        log("Build sequence for target `" + root + "' is " + ret, MSG_VERBOSE);
        for (Enumeration en = targets.keys(); en.hasMoreElements();) {
            String curTarget = (String) en.nextElement();
            String st = (String) state.get(curTarget);
            if (st == null) {
                tsort(curTarget, targets, state, visiting, ret);
            } else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: "
                    + curTarget);
            }
        }
        log("Complete build sequence is " + ret, MSG_VERBOSE);
        return ret;
    }

    /**
     * Performs a single step in a recursive depth-first-search traversal of
     * the target dependency tree. 
     * <p>
     * The current target is first set to the "visiting" state, and pushed 
     * onto the "visiting" stack. 
     * <p>
     * An exception is then thrown if any child of the current node is in the 
     * visiting state, as that implies a circular dependency. The exception
     * contains details of the cycle, using elements of the "visiting" stack.
     * <p>
     * If any child has not already been "visited", this method is called
     * recursively on it.
     * <p>
     * The current target is then added to the ordered list of targets. Note
     * that this is performed after the children have been visited in order
     * to get the correct order. The current target is set to the "visited"
     * state.
     * <p>
     * By the time this method returns, the ordered list contains the sequence
     * of targets up to and including the current target.
     * 
     * @param root The current target to inspect. 
     *             Must not be <code>null</code>.
     * @param targets A mapping from names to targets (String to Target).
     *                Must not be <code>null</code>.
     * @param state   A mapping from target names to states 
     *                (String to String).
     *                The states in question are "VISITING" and "VISITED".
     *                Must not be <code>null</code>.
     * @param visiting A stack of targets which are currently being visited.
     *                 Must not be <code>null</code>.
     * @param ret     The list to add target names to. This will end up 
     *                containing the complete list of depenencies in 
     *                dependency order.
     *                Must not be <code>null</code>.
     * 
     * @exception BuildException if a non-existent target is specified or if
     *                           a circular dependency is detected.
     */
    private final void tsort(String root, Hashtable targets,
                             Hashtable state, Stack visiting,
                             Vector ret)
        throws BuildException {
        state.put(root, VISITING);
        visiting.push(root);

        Target target = (Target) targets.get(root);

        // Make sure we exist
        if (target == null) {
            StringBuffer sb = new StringBuffer("Target `");
            sb.append(root);
            sb.append("' does not exist in this project. ");
            visiting.pop();
            if (!visiting.empty()) {
                String parent = (String) visiting.peek();
                sb.append("It is used from target `");
                sb.append(parent);
                sb.append("'.");
            }

            throw new BuildException(new String(sb));
        }

        for (Enumeration en = target.getDependencies(); en.hasMoreElements();) {
            String cur = (String) en.nextElement();
            String m = (String) state.get(cur);
            if (m == null) {
                // Not been visited
                tsort(cur, targets, state, visiting, ret);
            } else if (m == VISITING) {
                // Currently visiting this node, so have a cycle
                throw makeCircularException(cur, visiting);
            }
        }

        String p = (String) visiting.pop();
        if (root != p) {
            throw new RuntimeException("Unexpected internal error: expected to "
                + "pop " + root + " but got " + p);
        }
        state.put(root, VISITED);
        ret.addElement(target);
    }

    /**
     * Builds an appropriate exception detailing a specified circular 
     * dependency.
     * 
     * @param end The dependency to stop at. Must not be <code>null</code>.
     * @param stk A stack of dependencies. Must not be <code>null</code>.
     * 
     * @return a BuildException detailing the specified circular dependency.
     */
    private static BuildException makeCircularException(String end, Stack stk) {
        StringBuffer sb = new StringBuffer("Circular dependency: ");
        sb.append(end);
        String c;
        do {
            c = (String) stk.pop();
            sb.append(" <- ");
            sb.append(c);
        } while (!c.equals(end));
        return new BuildException(new String(sb));
    }

    /**
     * Adds a reference to the project.
     * 
     * @param name The name of the reference. Must not be <code>null</code>.
     * @param value The value of the reference. Must not be <code>null</code>.
     */
    public void addReference(String name, Object value) {
        Object old = references.get(name);
        if (old == value) {
            // no warning, this is not changing anything
            return;
        }
        if (old != null) {
            log("Overriding previous definition of reference to " + name, 
                MSG_WARN);
        }
        log("Adding reference: " + name + " -> " + value, MSG_DEBUG);
        references.put(name, value);
    }

    /**
     * Returns a map of the references in the project (String to Object).
     * The returned hashtable is "live" and so should not be modified.
     * 
     * @return a map of the references in the project (String to Object).
     */
    public Hashtable getReferences() {
        return references;
    }

    /**
     * Looks up a reference by its key (ID).
     * 
     * @param key The key for the desired reference. 
     *            Must not be <code>null</code>.
     * 
     * @return the reference with the specified ID, or <code>null</code> if
     *         there is no such reference in the project.
     */
    public Object getReference(String key) {
        return references.get(key);
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
     * @since 1.95, Ant 1.5
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

    /**
     * Sends a "build started" event to the build listeners for this project.
     */
    protected void fireBuildStarted() {
        BuildEvent event = new BuildEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.buildStarted(event);
        }
    }

    /**
     * Sends a "build finished" event to the build listeners for this project.
     * @param exception an exception indicating a reason for a build
     *                  failure. May be <code>null</code>, indicating
     *                  a successful build.
     */
    protected void fireBuildFinished(Throwable exception) {
        BuildEvent event = new BuildEvent(this);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.buildFinished(event);
        }
    }

    
    /**
     * Sends a "target started" event to the build listeners for this project.
     * 
     * @param target The target which is starting to build.
     *               Must not be <code>null</code>.
     */
    protected void fireTargetStarted(Target target) {
        BuildEvent event = new BuildEvent(target);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.targetStarted(event);
        }
    }

    /**
     * Sends a "target finished" event to the build listeners for this 
     * project.
     * 
     * @param target    The target which has finished building.
     *                  Must not be <code>null</code>.
     * @param exception an exception indicating a reason for a build
     *                  failure. May be <code>null</code>, indicating
     *                  a successful build.
     */
    protected void fireTargetFinished(Target target, Throwable exception) {
        BuildEvent event = new BuildEvent(target);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.targetFinished(event);
        }
    }
    
    /**
     * Sends a "task started" event to the build listeners for this project.
     * 
     * @param task The target which is starting to execute.
     *               Must not be <code>null</code>.
     */
    protected void fireTaskStarted(Task task) {
        // register this as the current task on the current thread.
        registerThreadTask(Thread.currentThread(), task);
        BuildEvent event = new BuildEvent(task);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.taskStarted(event);
        }
    }

    /**
     * Sends a "task finished" event to the build listeners for this 
     * project.
     * 
     * @param task      The task which has finished executing.
     *                  Must not be <code>null</code>.
     * @param exception an exception indicating a reason for a build
     *                  failure. May be <code>null</code>, indicating
     *                  a successful build.
     */
    protected void fireTaskFinished(Task task, Throwable exception) {
        registerThreadTask(Thread.currentThread(), null);
        System.out.flush();
        System.err.flush();
        BuildEvent event = new BuildEvent(task);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.taskFinished(event);
        }
    }

    /**
     * Sends a "message logged" event to the build listeners for this project.
     * 
     * @param event    The event to send. This should be built up with the 
     *                 appropriate task/target/project by the caller, so that
     *                 this method can set the message and priority, then send
     *                 the event. Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    private void fireMessageLoggedEvent(BuildEvent event, String message, 
                                        int priority) {
        event.setMessage(message, priority);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.messageLogged(event);
        }
    }

    /**
     * Sends a "message logged" project level event to the build listeners for 
     * this project.
     * 
     * @param project  The project generating the event.
     *                 Should not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    protected void fireMessageLogged(Project project, String message, 
                                     int priority) {
        BuildEvent event = new BuildEvent(project);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Sends a "message logged" target level event to the build listeners for 
     * this project.
     * 
     * @param target   The target generating the event. 
     *                 Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    protected void fireMessageLogged(Target target, String message, 
                                     int priority) {
        BuildEvent event = new BuildEvent(target);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Sends a "message logged" task level event to the build listeners for 
     * this project.
     * 
     * @param task     The task generating the event. 
     *                 Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    protected void fireMessageLogged(Task task, String message, int priority) {
        BuildEvent event = new BuildEvent(task);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Register a task as the current task for a thread.
     * If the task is null, the thread's entry is removed.
     *
     * @param thread the thread on which the task is registered.
     * @param task the task to be registered.
     * @since 1.102, Ant 1.5
     */
    public void registerThreadTask(Thread thread, Task task) {
        if (task != null) {
            threadTasks.put(thread, task);
        } else {
            threadTasks.remove(thread);
        }
    }
    
    /**
     * Get the current task assopciated with a thread, if any
     *
     * @param thread the thread for which the task is required.
     * @return the task which is currently registered for the given thread or
     *         null if no task is registered. 
     */
    public Task getThreadTask(Thread thread) {
        return (Task) threadTasks.get(thread);
    }
    
    
}
