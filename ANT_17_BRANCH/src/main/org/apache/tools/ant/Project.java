/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.helper.DefaultExecutor;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Description;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.StringUtils;

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
 */
public class Project implements ResourceFactory {
    private static final String LINE_SEP = System.getProperty("line.separator");

    /** Message priority of &quot;error&quot;. */
    public static final int MSG_ERR = 0;
    /** Message priority of &quot;warning&quot;. */
    public static final int MSG_WARN = 1;
    /** Message priority of &quot;information&quot;. */
    public static final int MSG_INFO = 2;
    /** Message priority of &quot;verbose&quot;. */
    public static final int MSG_VERBOSE = 3;
    /** Message priority of &quot;debug&quot;. */
    public static final int MSG_DEBUG = 4;

    /**
     * Constant for the &quot;visiting&quot; state, used when
     * traversing a DFS of target dependencies.
     */
    private static final String VISITING = "VISITING";
    /**
     * Constant for the &quot;visited&quot; state, used when
     * traversing a DFS of target dependencies.
     */
    private static final String VISITED = "VISITED";

    /**
     * Version constant for Java 1.0 .
     *
     * @deprecated since 1.5.x.
     *             Use {@link JavaEnvUtils#JAVA_1_0} instead.
     */
    public static final String JAVA_1_0 = JavaEnvUtils.JAVA_1_0;
    /**
     * Version constant for Java 1.1 .
     *
     * @deprecated since 1.5.x.
     *             Use {@link JavaEnvUtils#JAVA_1_1} instead.
     */
    public static final String JAVA_1_1 = JavaEnvUtils.JAVA_1_1;
    /**
     * Version constant for Java 1.2 .
     *
     * @deprecated since 1.5.x.
     *             Use {@link JavaEnvUtils#JAVA_1_2} instead.
     */
    public static final String JAVA_1_2 = JavaEnvUtils.JAVA_1_2;
    /**
     * Version constant for Java 1.3 .
     *
     * @deprecated since 1.5.x.
     *             Use {@link JavaEnvUtils#JAVA_1_3} instead.
     */
    public static final String JAVA_1_3 = JavaEnvUtils.JAVA_1_3;
    /**
     * Version constant for Java 1.4 .
     *
     * @deprecated since 1.5.x.
     *             Use {@link JavaEnvUtils#JAVA_1_4} instead.
     */
    public static final String JAVA_1_4 = JavaEnvUtils.JAVA_1_4;

    /** Default filter start token. */
    public static final String TOKEN_START = FilterSet.DEFAULT_TOKEN_START;
    /** Default filter end token. */
    public static final String TOKEN_END = FilterSet.DEFAULT_TOKEN_END;

    /** Instance of a utility class to use for file operations. */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** Name of this project. */
    private String name;
    /** Description for this project (if any). */
    private String description;


    /** Map of references within the project (paths etc) (String to Object). */
    private Hashtable references = new AntRefTable();

    /** Map of id references - used for indicating broken build files */
    private HashMap idReferences = new HashMap();

    /** the parent project for old id resolution (if inheritreferences is set) */
    private Project parentIdProject = null;

    /** Name of the project's default target. */
    private String defaultTarget;

    /** Map from target names to targets (String to Target). */
    private Hashtable targets = new Hashtable();
    /** Set of global filters. */
    private FilterSet globalFilterSet = new FilterSet();
    {
        // Initialize the globalFileSet's project
        globalFilterSet.setProject(this);
    }

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
     * The Ant core classloader--may be <code>null</code> if using
     * parent classloader.
     */
    private ClassLoader coreLoader = null;

    /** Records the latest task to be executed on a thread. */
    private Map/*<Thread,Task>*/ threadTasks = Collections.synchronizedMap(new WeakHashMap());

    /** Records the latest task to be executed on a thread group. */
    private Map/*<ThreadGroup,Task>*/ threadGroupTasks
        = Collections.synchronizedMap(new WeakHashMap());

    /**
     * Called to handle any input requests.
     */
    private InputHandler inputHandler = null;

    /**
     * The default input stream used to read any input.
     */
    private InputStream defaultInputStream = null;

    /**
     * Keep going flag.
     */
    private boolean keepGoingMode = false;

    /**
     * Flag which catches Listeners which try to use System.out or System.err .
     */
    private boolean loggingMessage = false;

    /**
     * Set the input handler.
     *
     * @param handler the InputHandler instance to use for gathering input.
     */
    public void setInputHandler(InputHandler handler) {
        inputHandler = handler;
    }

    /**
     * Set the default System input stream. Normally this stream is set to
     * System.in. This inputStream is used when no task input redirection is
     * being performed.
     *
     * @param defaultInputStream the default input stream to use when input
     *        is requested.
     * @since Ant 1.6
     */
    public void setDefaultInputStream(InputStream defaultInputStream) {
        this.defaultInputStream = defaultInputStream;
    }

    /**
     * Get this project's input stream.
     *
     * @return the InputStream instance in use by this Project instance to
     * read input.
     */
    public InputStream getDefaultInputStream() {
        return defaultInputStream;
    }

    /**
     * Retrieve the current input handler.
     *
     * @return the InputHandler instance currently in place for the project
     *         instance.
     */
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /**
     * Create a new Ant project.
     */
    public Project() {
        inputHandler = new DefaultInputHandler();
    }

    /**
     * Create and initialize a subproject. By default the subproject will be of
     * the same type as its parent. If a no-arg constructor is unavailable, the
     * <code>Project</code> class will be used.
     * @return a Project instance configured as a subproject of this Project.
     * @since Ant 1.7
     */
    public Project createSubProject() {
        Project subProject = null;
        try {
            subProject = (Project) (getClass().newInstance());
        } catch (Exception e) {
            subProject = new Project();
        }
        initSubProject(subProject);
        return subProject;
    }

    /**
     * Initialize a subproject.
     * @param subProject the subproject to initialize.
     */
    public void initSubProject(Project subProject) {
        ComponentHelper.getComponentHelper(subProject)
            .initSubProject(ComponentHelper.getComponentHelper(this));
        subProject.setDefaultInputStream(getDefaultInputStream());
        subProject.setKeepGoingMode(this.isKeepGoingMode());
        subProject.setExecutor(getExecutor().getSubProjectExecutor());
    }

    /**
     * Initialise the project.
     *
     * This involves setting the default task definitions and loading the
     * system properties.
     *
     * @exception BuildException if the default task list cannot be loaded.
     */
    public void init() throws BuildException {
        initProperties();

        ComponentHelper.getComponentHelper(this).initDefaultDefinitions();
    }

    /**
     * Initializes the properties.
     * @exception BuildException if an vital property could not be set.
     * @since Ant 1.7
     */
    public void initProperties() throws BuildException {
        setJavaVersionProperty();
        setSystemProperties();
        setPropertyInternal(MagicNames.ANT_VERSION, Main.getAntVersion());
        setAntLib();
    }

    /**
     * Set a property to the location of ant.jar.
     * Use the locator to find the location of the Project.class, and
     * if this is not null, set the property {@link MagicNames#ANT_LIB}
     * to the result
     */
    private void setAntLib() {
        File antlib = org.apache.tools.ant.launch.Locator.getClassSource(
            Project.class);
        if (antlib != null) {
            setPropertyInternal(MagicNames.ANT_LIB, antlib.getAbsolutePath());
        }
    }
    /**
     * Factory method to create a class loader for loading classes from
     * a given path.
     *
     * @param path the path from which classes are to be loaded.
     *
     * @return an appropriate classloader.
     */
    public AntClassLoader createClassLoader(Path path) {
        return new AntClassLoader(
            getClass().getClassLoader(), this, path);
    }

    /**
     * Factory method to create a class loader for loading classes from
     * a given path.
     *
     * @param parent the parent classloader for the new loader.
     * @param path the path from which classes are to be loaded.
     *
     * @return an appropriate classloader.
     */
    public AntClassLoader createClassLoader(
        ClassLoader parent, Path path) {
        return new AntClassLoader(parent, this, path);
    }

    /**
     * Set the core classloader for the project. If a <code>null</code>
     * classloader is specified, the parent classloader should be used.
     *
     * @param coreLoader The classloader to use for the project.
     *                   May be <code>null</code>.
     */
    public void setCoreLoader(ClassLoader coreLoader) {
        this.coreLoader = coreLoader;
    }

    /**
     * Return the core classloader to use for this project.
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
     * Add a build listener to the list. This listener will
     * be notified of build events for this project.
     *
     * @param listener The listener to add to the list.
     *                 Must not be <code>null</code>.
     */
    public synchronized void addBuildListener(BuildListener listener) {
        // If the listeners already has this listener, do nothing
        if (listeners.contains(listener)) {
            return;
        }
        // create a new Vector to avoid ConcurrentModificationExc when
        // the listeners get added/removed while we are in fire
        Vector newListeners = getBuildListeners();
        newListeners.addElement(listener);
        listeners = newListeners;
    }

    /**
     * Remove a build listener from the list. This listener
     * will no longer be notified of build events for this project.
     *
     * @param listener The listener to remove from the list.
     *                 Should not be <code>null</code>.
     */
    public synchronized void removeBuildListener(BuildListener listener) {
        // create a new Vector to avoid ConcurrentModificationExc when
        // the listeners get added/removed while we are in fire
        Vector newListeners = getBuildListeners();
        newListeners.removeElement(listener);
        listeners = newListeners;
    }

    /**
     * Return a copy of the list of build listeners for the project.
     *
     * @return a list of build listeners for the project
     */
    public Vector getBuildListeners() {
        return (Vector) listeners.clone();
    }

    /**
     * Write a message to the log with the default log level
     * of MSG_INFO .
     * @param message The text to log. Should not be <code>null</code>.
     */

    public void log(String message) {
        log(message, MSG_INFO);
    }

    /**
     * Write a project level message to the log with the given log level.
     * @param message The text to log. Should not be <code>null</code>.
     * @param msgLevel The log priority level to use.
     */
    public void log(String message, int msgLevel) {
        log(message, null, msgLevel);
    }

    /**
     * Write a project level message to the log with the given log level.
     * @param message The text to log. Should not be <code>null</code>.
     * @param throwable The exception causing this log, may be <code>null</code>.
     * @param msgLevel The log priority level to use.
     * @since 1.7
     */
    public void log(String message, Throwable throwable, int msgLevel) {
        fireMessageLogged(this, message, throwable, msgLevel);
    }

    /**
     * Write a task level message to the log with the given log level.
     * @param task The task to use in the log. Must not be <code>null</code>.
     * @param message The text to log. Should not be <code>null</code>.
     * @param msgLevel The log priority level to use.
     */
    public void log(Task task, String message, int msgLevel) {
        fireMessageLogged(task, message, null, msgLevel);
    }

    /**
     * Write a task level message to the log with the given log level.
     * @param task The task to use in the log. Must not be <code>null</code>.
     * @param message The text to log. Should not be <code>null</code>.
     * @param throwable The exception causing this log, may be <code>null</code>.
     * @param msgLevel The log priority level to use.
     * @since 1.7
     */
    public void log(Task task, String message, Throwable throwable, int msgLevel) {
        fireMessageLogged(task, message, throwable, msgLevel);
    }

    /**
     * Write a target level message to the log with the given log level.
     * @param target The target to use in the log.
     *               Must not be <code>null</code>.
     * @param message The text to log. Should not be <code>null</code>.
     * @param msgLevel The log priority level to use.
     */
    public void log(Target target, String message, int msgLevel) {
        log(target, message, null, msgLevel);
    }

    /**
     * Write a target level message to the log with the given log level.
     * @param target The target to use in the log.
     *               Must not be <code>null</code>.
     * @param message The text to log. Should not be <code>null</code>.
     * @param throwable The exception causing this log, may be <code>null</code>.
     * @param msgLevel The log priority level to use.
     * @since 1.7
     */
    public void log(Target target, String message, Throwable throwable,
            int msgLevel) {
        fireMessageLogged(target, message, throwable, msgLevel);
    }

    /**
     * Return the set of global filters.
     *
     * @return the set of global filters.
     */
    public FilterSet getGlobalFilterSet() {
        return globalFilterSet;
    }

    /**
     * Set a property. Any existing property of the same name
     * is overwritten, unless it is a user property.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     */
    public void setProperty(String name, String value) {
        PropertyHelper.getPropertyHelper(this).
                setProperty(null, name, value, true);
    }

    /**
     * Set a property if no value currently exists. If the property
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
        PropertyHelper.getPropertyHelper(this).setNewProperty(null, name,
                                                              value);
    }

    /**
     * Set a user property, which cannot be overwritten by
     * set/unset property calls. Any previous value is overwritten.
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @see #setProperty(String,String)
     */
    public void setUserProperty(String name, String value) {
        PropertyHelper.getPropertyHelper(this).setUserProperty(null, name,
                                                               value);
    }

    /**
     * Set a user property, which cannot be overwritten by set/unset
     * property calls. Any previous value is overwritten. Also marks
     * these properties as properties that have not come from the
     * command line.
     *
     * @param name The name of property to set.
     *             Must not be <code>null</code>.
     * @param value The new value of the property.
     *              Must not be <code>null</code>.
     * @see #setProperty(String,String)
     */
    public void setInheritedProperty(String name, String value) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        ph.setInheritedProperty(null, name, value);
    }

    /**
     * Set a property unless it is already defined as a user property
     * (in which case the method returns silently).
     *
     * @param name The name of the property.
     *             Must not be <code>null</code>.
     * @param value The property value. Must not be <code>null</code>.
     */
    private void setPropertyInternal(String name, String value) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        ph.setProperty(null, name, value, false);
    }

    /**
     * Return the value of a property, if it is set.
     *
     * @param propertyName The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public String getProperty(String propertyName) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return (String) ph.getProperty(null, propertyName);
    }

    /**
     * Replace ${} style constructions in the given value with the
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
     *                           property name, e.g. <code>${xxx</code>.
     */
    public String replaceProperties(String value)
        throws BuildException {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return ph.replaceProperties(null, value, null);
    }

    /**
     * Return the value of a user property, if it is set.
     *
     * @param propertyName The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
     public String getUserProperty(String propertyName) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return (String) ph.getUserProperty(null, propertyName);
    }

    /**
     * Return a copy of the properties table.
     * @return a hashtable containing all properties
     *         (including user properties).
     */
    public Hashtable getProperties() {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return ph.getProperties();
    }

    /**
     * Return a copy of the user property hashtable.
     * @return a hashtable containing just the user properties.
     */
    public Hashtable getUserProperties() {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return ph.getUserProperties();
    }

    /**
     * Copy all user properties that have been set on the command
     * line or a GUI tool from this instance to the Project instance
     * given as the argument.
     *
     * <p>To copy all &quot;user&quot; properties, you will also have to call
     * {@link #copyInheritedProperties copyInheritedProperties}.</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.5
     */
    public void copyUserProperties(Project other) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        ph.copyUserProperties(other);
    }

    /**
     * Copy all user properties that have not been set on the
     * command line or a GUI tool from this instance to the Project
     * instance given as the argument.
     *
     * <p>To copy all &quot;user&quot; properties, you will also have to call
     * {@link #copyUserProperties copyUserProperties}.</p>
     *
     * @param other the project to copy the properties to.  Must not be null.
     *
     * @since Ant 1.5
     */
    public void copyInheritedProperties(Project other) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        ph.copyInheritedProperties(other);
    }

    /**
     * Set the default target of the project.
     *
     * @param defaultTarget The name of the default target for this project.
     *                      May be <code>null</code>, indicating that there is
     *                      no default target.
     *
     * @deprecated since 1.5.x.
     *             Use setDefault.
     * @see #setDefault(String)
     */
    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    /**
     * Return the name of the default target of the project.
     * @return name of the default target or
     *         <code>null</code> if no default has been set.
     */
    public String getDefaultTarget() {
        return defaultTarget;
    }

    /**
     * Set the default target of the project.
     *
     * @param defaultTarget The name of the default target for this project.
     *                      May be <code>null</code>, indicating that there is
     *                      no default target.
     */
    public void setDefault(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    /**
     * Set the name of the project, also setting the user
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
     * Return the project name, if one has been set.
     *
     * @return the project name, or <code>null</code> if it hasn't been set.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the project description.
     *
     * @param description The description of the project.
     *                    May be <code>null</code>.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return the project description, if one has been set.
     *
     * @return the project description, or <code>null</code> if it hasn't
     *         been set.
     */
    public String getDescription() {
        if (description == null) {
            description = Description.getDescription(this);
        }
        return description;
    }

    /**
     * Add a filter to the set of global filters.
     *
     * @param token The token to filter.
     *              Must not be <code>null</code>.
     * @param value The replacement value.
     *              Must not be <code>null</code>.
     * @deprecated since 1.4.x.
     *             Use getGlobalFilterSet().addFilter(token,value)
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
     * Return a hashtable of global filters, mapping tokens to values.
     *
     * @return a hashtable of global filters, mapping tokens to values
     *         (String to String).
     *
     * @deprecated since 1.4.x
     *             Use getGlobalFilterSet().getFilterHash().
     *
     * @see #getGlobalFilterSet()
     * @see FilterSet#getFilterHash()
     */
    public Hashtable getFilters() {
        // we need to build the hashtable dynamically
        return globalFilterSet.getFilterHash();
    }

    /**
     * Set the base directory for the project, checking that
     * the given filename exists and is a directory.
     *
     * @param baseD The project base directory.
     *              Must not be <code>null</code>.
     *
     * @exception BuildException if the directory if invalid.
     */
    public void setBasedir(String baseD) throws BuildException {
        setBaseDir(new File(baseD));
    }

    /**
     * Set the base directory for the project, checking that
     * the given file exists and is a directory.
     *
     * @param baseDir The project base directory.
     *                Must not be <code>null</code>.
     * @exception BuildException if the specified file doesn't exist or
     *                           isn't a directory.
     */
    public void setBaseDir(File baseDir) throws BuildException {
        baseDir = FILE_UTILS.normalize(baseDir.getAbsolutePath());
        if (!baseDir.exists()) {
            throw new BuildException("Basedir " + baseDir.getAbsolutePath()
                + " does not exist");
        }
        if (!baseDir.isDirectory()) {
            throw new BuildException("Basedir " + baseDir.getAbsolutePath()
                + " is not a directory");
        }
        this.baseDir = baseDir;
        setPropertyInternal(MagicNames.PROJECT_BASEDIR, this.baseDir.getPath());
        String msg = "Project base dir set to: " + this.baseDir;
         log(msg, MSG_VERBOSE);
    }

    /**
     * Return the base directory of the project as a file object.
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
     * Set &quot;keep-going&quot; mode. In this mode Ant will try to execute
     * as many targets as possible. All targets that do not depend
     * on failed target(s) will be executed.  If the keepGoing settor/getter
     * methods are used in conjunction with the <code>ant.executor.class</code>
     * property, they will have no effect.
     * @param keepGoingMode &quot;keep-going&quot; mode
     * @since Ant 1.6
     */
    public void setKeepGoingMode(boolean keepGoingMode) {
        this.keepGoingMode = keepGoingMode;
    }

    /**
     * Return the keep-going mode.  If the keepGoing settor/getter
     * methods are used in conjunction with the <code>ant.executor.class</code>
     * property, they will have no effect.
     * @return &quot;keep-going&quot; mode
     * @since Ant 1.6
     */
    public boolean isKeepGoingMode() {
        return this.keepGoingMode;
    }

    /**
     * Return the version of Java this class is running under.
     * @return the version of Java as a String, e.g. "1.1" .
     * @see org.apache.tools.ant.util.JavaEnvUtils#getJavaVersion
     * @deprecated since 1.5.x.
     *             Use org.apache.tools.ant.util.JavaEnvUtils instead.
     */
    public static String getJavaVersion() {
        return JavaEnvUtils.getJavaVersion();
    }

    /**
     * Set the <code>ant.java.version</code> property and tests for
     * unsupported JVM versions. If the version is supported,
     * verbose log messages are generated to record the Java version
     * and operating system name.
     *
     * @exception BuildException if this Java version is not supported.
     *
     * @see org.apache.tools.ant.util.JavaEnvUtils#getJavaVersion
     */
    public void setJavaVersionProperty() throws BuildException {
        String javaVersion = JavaEnvUtils.getJavaVersion();
        setPropertyInternal(MagicNames.ANT_JAVA_VERSION, javaVersion);

        // sanity check
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_0)
                || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1))  {
            throw new BuildException("Ant cannot work on Java 1.0 / 1.1");
        }
        log("Detected Java version: " + javaVersion + " in: "
            + System.getProperty("java.home"), MSG_VERBOSE);

        log("Detected OS: " + System.getProperty("os.name"), MSG_VERBOSE);
    }

    /**
     * Add all system properties which aren't already defined as
     * user properties to the project properties.
     */
    public void setSystemProperties() {
        Properties systemP = System.getProperties();
        Enumeration e = systemP.propertyNames();
        while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            String value = systemP.getProperty(propertyName);
            if (value != null) {
                this.setPropertyInternal(propertyName, value);
            }
        }
    }

    /**
     * Add a new task definition to the project.
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
        ComponentHelper.getComponentHelper(this).addTaskDefinition(taskName,
                taskClass);
    }

    /**
     * Check whether or not a class is suitable for serving as Ant task.
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
        ComponentHelper.getComponentHelper(this).checkTaskClass(taskClass);

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
            taskClass.getConstructor((Class[]) null);
            // don't have to check for public, since
            // getConstructor finds public constructors only.
        } catch (NoSuchMethodException e) {
            final String message = "No public no-arg constructor in "
                + taskClass;
            log(message, Project.MSG_ERR);
            throw new BuildException(message);
        } catch (LinkageError e) {
            String message = "Could not load " + taskClass + ": " + e;
            log(message, Project.MSG_ERR);
            throw new BuildException(message, e);
        }
        if (!Task.class.isAssignableFrom(taskClass)) {
            TaskAdapter.checkTaskClass(taskClass, this);
        }
    }

    /**
     * Return the current task definition hashtable. The returned hashtable is
     * &quot;live&quot; and so should not be modified.
     *
     * @return a map of from task name to implementing class
     *         (String to Class).
     */
    public Hashtable getTaskDefinitions() {
        return ComponentHelper.getComponentHelper(this).getTaskDefinitions();
    }

    /**
     * Add a new datatype definition.
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
        ComponentHelper.getComponentHelper(this).addDataTypeDefinition(typeName,
                typeClass);
    }

    /**
     * Return the current datatype definition hashtable. The returned
     * hashtable is &quot;live&quot; and so should not be modified.
     *
     * @return a map of from datatype name to implementing class
     *         (String to Class).
     */
    public Hashtable getDataTypeDefinitions() {
        return ComponentHelper.getComponentHelper(this).getDataTypeDefinitions();
    }

    /**
     * Add a <em>new</em> target to the project.
     *
     * @param target The target to be added to the project.
     *               Must not be <code>null</code>.
     *
     * @exception BuildException if the target already exists in the project
     *
     * @see Project#addOrReplaceTarget(Target)
     */
    public void addTarget(Target target) throws BuildException {
        addTarget(target.getName(), target);
    }

    /**
     * Add a <em>new</em> target to the project.
     *
     * @param targetName The name to use for the target.
     *             Must not be <code>null</code>.
     * @param target The target to be added to the project.
     *               Must not be <code>null</code>.
     *
     * @exception BuildException if the target already exists in the project.
     *
     * @see Project#addOrReplaceTarget(String, Target)
     */
     public void addTarget(String targetName, Target target)
         throws BuildException {
         if (targets.get(targetName) != null) {
             throw new BuildException("Duplicate target: `" + targetName + "'");
         }
         addOrReplaceTarget(targetName, target);
     }

    /**
     * Add a target to the project, or replaces one with the same
     * name.
     *
     * @param target The target to be added or replaced in the project.
     *               Must not be <code>null</code>.
     */
    public void addOrReplaceTarget(Target target) {
        addOrReplaceTarget(target.getName(), target);
    }

    /**
     * Add a target to the project, or replaces one with the same
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
     * Return the hashtable of targets. The returned hashtable
     * is &quot;live&quot; and so should not be modified.
     * @return a map from name to target (String to Target).
     */
    public Hashtable getTargets() {
        return targets;
    }

    /**
     * Create a new instance of a task, adding it to a list of
     * created tasks for later invalidation. This causes all tasks
     * to be remembered until the containing project is removed
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
        return ComponentHelper.getComponentHelper(this).createTask(taskType);
    }

    /**
     * Create a new instance of a data type.
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
        return ComponentHelper.getComponentHelper(this).createDataType(typeName);
    }

    /**
     * Set the Executor instance for this Project.
     * @param e the Executor to use.
     */
    public void setExecutor(Executor e) {
        addReference(MagicNames.ANT_EXECUTOR_REFERENCE, e);
    }

    /**
     * Get this Project's Executor (setting it if necessary).
     * @return an Executor instance.
     */
    public Executor getExecutor() {
        Object o = getReference(MagicNames.ANT_EXECUTOR_REFERENCE);
        if (o == null) {
            String classname = getProperty(MagicNames.ANT_EXECUTOR_CLASSNAME);
            if (classname == null) {
                classname = DefaultExecutor.class.getName();
            }
            log("Attempting to create object of type " + classname, MSG_DEBUG);
            try {
                o = Class.forName(classname, true, coreLoader).newInstance();
            } catch (ClassNotFoundException seaEnEfEx) {
                //try the current classloader
                try {
                    o = Class.forName(classname).newInstance();
                } catch (Exception ex) {
                    log(ex.toString(), MSG_ERR);
                }
            } catch (Exception ex) {
                log(ex.toString(), MSG_ERR);
            }
            if (o == null) {
                throw new BuildException(
                    "Unable to obtain a Target Executor instance.");
            }
            setExecutor((Executor) o);
        }
        return (Executor) o;
    }

    /**
     * Execute the specified sequence of targets, and the targets
     * they depend on.
     *
     * @param names A vector of target name strings to execute.
     *              Must not be <code>null</code>.
     *
     * @exception BuildException if the build failed.
     */
    public void executeTargets(Vector names) throws BuildException {
        getExecutor().executeTargets(this,
            (String[]) (names.toArray(new String[names.size()])));
    }

    /**
     * Demultiplex output so that each task receives the appropriate
     * messages. If the current thread is not currently executing a task,
     * the message is logged directly.
     *
     * @param output Message to handle. Should not be <code>null</code>.
     * @param isWarning Whether the text represents an warning (<code>true</code>)
     *        or information (<code>false</code>).
     */
    public void demuxOutput(String output, boolean isWarning) {
        Task task = getThreadTask(Thread.currentThread());
        if (task == null) {
            log(output, isWarning ? MSG_WARN : MSG_INFO);
        } else {
            if (isWarning) {
                task.handleErrorOutput(output);
            } else {
                task.handleOutput(output);
            }
        }
    }

    /**
     * Read data from the default input stream. If no default has been
     * specified, System.in is used.
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @since Ant 1.6
     */
    public int defaultInput(byte[] buffer, int offset, int length)
        throws IOException {
        if (defaultInputStream != null) {
            System.out.flush();
            return defaultInputStream.read(buffer, offset, length);
        } else {
            throw new EOFException("No input provided for project");
        }
    }

    /**
     * Demux an input request to the correct task.
     *
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     *
     * @exception IOException if the data cannot be read.
     * @since Ant 1.6
     */
    public int demuxInput(byte[] buffer, int offset, int length)
        throws IOException {
        Task task = getThreadTask(Thread.currentThread());
        if (task == null) {
            return defaultInput(buffer, offset, length);
        } else {
            return task.handleInput(buffer, offset, length);
        }
    }

    /**
     * Demultiplex flush operations so that each task receives the appropriate
     * messages. If the current thread is not currently executing a task,
     * the message is logged directly.
     *
     * @since Ant 1.5.2
     *
     * @param output Message to handle. Should not be <code>null</code>.
     * @param isError Whether the text represents an error (<code>true</code>)
     *        or information (<code>false</code>).
     */
    public void demuxFlush(String output, boolean isError) {
        Task task = getThreadTask(Thread.currentThread());
        if (task == null) {
            fireMessageLogged(this, output, isError ? MSG_ERR : MSG_INFO);
        } else {
            if (isError) {
                task.handleErrorFlush(output);
            } else {
                task.handleFlush(output);
            }
        }
    }

    /**
     * Execute the specified target and any targets it depends on.
     *
     * @param targetName The name of the target to execute.
     *                   Must not be <code>null</code>.
     *
     * @exception BuildException if the build failed.
     */
    public void executeTarget(String targetName) throws BuildException {

        // sanity check ourselves, if we've been asked to build nothing
        // then we should complain

        if (targetName == null) {
            String msg = "No target specified";
            throw new BuildException(msg);
        }

        // Sort and run the dependency tree.
        // Sorting checks if all the targets (and dependencies)
        // exist, and if there is any cycle in the dependency
        // graph.
        executeSortedTargets(topoSort(targetName, targets, false));
    }

    /**
     * Execute a <code>Vector</code> of sorted targets.
     * @param sortedTargets   the aforementioned <code>Vector</code>.
     * @throws BuildException on error.
     */
    public void executeSortedTargets(Vector sortedTargets)
        throws BuildException {
        Set succeededTargets = new HashSet();
        BuildException buildException = null; // first build exception
        for (Enumeration iter = sortedTargets.elements();
             iter.hasMoreElements();) {
            Target curtarget = (Target) iter.nextElement();
            boolean canExecute = true;
            for (Enumeration depIter = curtarget.getDependencies();
                 depIter.hasMoreElements();) {
                String dependencyName = ((String) depIter.nextElement());
                if (!succeededTargets.contains(dependencyName)) {
                    canExecute = false;
                    log(curtarget,
                        "Cannot execute '" + curtarget.getName() + "' - '"
                        + dependencyName + "' failed or was not executed.",
                        MSG_ERR);
                    break;
                }
            }
            if (canExecute) {
                Throwable thrownException = null;
                try {
                    curtarget.performTasks();
                    succeededTargets.add(curtarget.getName());
                } catch (RuntimeException ex) {
                    if (!(keepGoingMode)) {
                        throw ex; // throw further
                    }
                    thrownException = ex;
                } catch (Throwable ex) {
                    if (!(keepGoingMode)) {
                        throw new BuildException(ex);
                    }
                    thrownException = ex;
                }
                if (thrownException != null) {
                    if (thrownException instanceof BuildException) {
                        log(curtarget,
                            "Target '" + curtarget.getName()
                            + "' failed with message '"
                            + thrownException.getMessage() + "'.", MSG_ERR);
                        // only the first build exception is reported
                        if (buildException == null) {
                            buildException = (BuildException) thrownException;
                        }
                    } else {
                        log(curtarget,
                            "Target '" + curtarget.getName()
                            + "' failed with message '"
                            + thrownException.getMessage() + "'.", MSG_ERR);
                        thrownException.printStackTrace(System.err);
                        if (buildException == null) {
                            buildException =
                                new BuildException(thrownException);
                        }
                    }
                }
            }
        }
        if (buildException != null) {
            throw buildException;
        }
    }

    /**
     * Return the canonical form of a filename.
     * <p>
     * If the specified file name is relative it is resolved
     * with respect to the given root directory.
     *
     * @param fileName The name of the file to resolve.
     *                 Must not be <code>null</code>.
     *
     * @param rootDir  The directory respective to which relative file names
     *                 are resolved. May be <code>null</code>, in which case
     *                 the current directory is used.
     *
     * @return the resolved File.
     *
     * @deprecated since 1.4.x
     */
    public File resolveFile(String fileName, File rootDir) {
        return FILE_UTILS.resolveFile(rootDir, fileName);
    }

    /**
     * Return the canonical form of a filename.
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
        return FILE_UTILS.resolveFile(baseDir, fileName);
    }

    /**
     * Translate a path into its native (platform specific) format.
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
     * @deprecated since 1.7
     *             Use FileUtils.translatePath instead.
     *
     * @see PathTokenizer
     */
    public static String translatePath(String toProcess) {
        return FileUtils.translatePath(toProcess);
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
     * @exception IOException if the copying fails.
     *
     * @deprecated since 1.4.x
     */
    public void copyFile(String sourceFile, String destFile)
          throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile);
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
     * @exception IOException if the copying fails.
     *
     * @deprecated since 1.4.x
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering)
        throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile,
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
     * @exception IOException if the copying fails.
     *
     * @deprecated since 1.4.x
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile,
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
     * @exception IOException if the copying fails.
     *
     * @deprecated since 1.4.x
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile,
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
     * @exception IOException if the copying fails.
     *
     * @deprecated since 1.4.x
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile);
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
     * @exception IOException if the copying fails.
     *
     * @deprecated since 1.4.x
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering)
        throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile,
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
     * @deprecated since 1.4.x
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile,
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
     * @deprecated since 1.4.x
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        FILE_UTILS.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite, preserveLastModified);
    }

    /**
     * Call File.setLastModified(long time) on Java above 1.1, and logs
     * a warning on Java 1.1.
     *
     * @param file The file to set the last modified time on.
     *             Must not be <code>null</code>.
     *
     * @param time the required modification time.
     *
     * @deprecated since 1.4.x
     *
     * @exception BuildException if the last modified time cannot be set
     *                           despite running on a platform with a version
     *                           above 1.1.
     */
    public void setFileLastModified(File file, long time)
         throws BuildException {
        FILE_UTILS.setFileLastModified(file, time);
        log("Setting modification time for " + file, MSG_VERBOSE);
    }

    /**
     * Return the boolean equivalent of a string, which is considered
     * <code>true</code> if either <code>"on"</code>, <code>"true"</code>,
     * or <code>"yes"</code> is found, ignoring case.
     *
     * @param s The string to convert to a boolean value.
     *
     * @return <code>true</code> if the given string is <code>"on"</code>,
     *         <code>"true"</code> or <code>"yes"</code>, or
     *         <code>false</code> otherwise.
     */
    public static boolean toBoolean(String s) {
        return ("on".equalsIgnoreCase(s)
                || "true".equalsIgnoreCase(s)
                || "yes".equalsIgnoreCase(s));
    }

    /**
     * Get the Project instance associated with the specified object.
     * @param o the object to query.
     * @return Project instance, if any.
     * @since Ant 1.7.1
     */
    public static Project getProject(Object o) {
        if (o instanceof ProjectComponent) {
            return ((ProjectComponent) o).getProject();
        }
        try {
            Method m = o.getClass().getMethod("getProject", (Class[]) null);
            if (Project.class == m.getReturnType()) {
                return (Project) m.invoke(o, (Object[]) null);
            }
        } catch (Exception e) {
            //too bad
        }
        return null;
    }

    /**
     * Topologically sort a set of targets.  Equivalent to calling
     * <code>topoSort(new String[] {root}, targets, true)</code>.
     *
     * @param root The name of the root target. The sort is created in such
     *             a way that the sequence of Targets up to the root
     *             target is the minimum possible such sequence.
     *             Must not be <code>null</code>.
     * @param targetTable A Hashtable mapping names to Targets.
     *                Must not be <code>null</code>.
     * @return a Vector of ALL Target objects in sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     */
    public final Vector topoSort(String root, Hashtable targetTable)
        throws BuildException {
        return topoSort(new String[] {root}, targetTable, true);
    }

    /**
     * Topologically sort a set of targets.  Equivalent to calling
     * <code>topoSort(new String[] {root}, targets, returnAll)</code>.
     *
     * @param root The name of the root target. The sort is created in such
     *             a way that the sequence of Targets up to the root
     *             target is the minimum possible such sequence.
     *             Must not be <code>null</code>.
     * @param targetTable A Hashtable mapping names to Targets.
     *                Must not be <code>null</code>.
     * @param returnAll <code>boolean</code> indicating whether to return all
     *                  targets, or the execution sequence only.
     * @return a Vector of Target objects in sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     * @since Ant 1.6.3
     */
    public final Vector topoSort(String root, Hashtable targetTable,
                                 boolean returnAll) throws BuildException {
        return topoSort(new String[] {root}, targetTable, returnAll);
    }

    /**
     * Topologically sort a set of targets.
     *
     * @param root <code>String[]</code> containing the names of the root targets.
     *             The sort is created in such a way that the ordered sequence of
     *             Targets is the minimum possible such sequence to the specified
     *             root targets.
     *             Must not be <code>null</code>.
     * @param targetTable A map of names to targets (String to Target).
     *                Must not be <code>null</code>.
     * @param returnAll <code>boolean</code> indicating whether to return all
     *                  targets, or the execution sequence only.
     * @return a Vector of Target objects in sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     * @since Ant 1.6.3
     */
    public final Vector topoSort(String[] root, Hashtable targetTable,
                                 boolean returnAll) throws BuildException {
        Vector ret = new Vector();
        Hashtable state = new Hashtable();
        Stack visiting = new Stack();

        // We first run a DFS based sort using each root as a starting node.
        // This creates the minimum sequence of Targets to the root node(s).
        // We then do a sort on any remaining unVISITED targets.
        // This is unnecessary for doing our build, but it catches
        // circular dependencies or missing Targets on the entire
        // dependency tree, not just on the Targets that depend on the
        // build Target.

        for (int i = 0; i < root.length; i++) {
            String st = (String) (state.get(root[i]));
            if (st == null) {
                tsort(root[i], targetTable, state, visiting, ret);
            } else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: "
                    + root[i]);
            }
        }
        StringBuffer buf = new StringBuffer("Build sequence for target(s)");

        for (int j = 0; j < root.length; j++) {
            buf.append((j == 0) ? " `" : ", `").append(root[j]).append('\'');
        }
        buf.append(" is " + ret);
        log(buf.toString(), MSG_VERBOSE);

        Vector complete = (returnAll) ? ret : new Vector(ret);
        for (Enumeration en = targetTable.keys(); en.hasMoreElements();) {
            String curTarget = (String) en.nextElement();
            String st = (String) state.get(curTarget);
            if (st == null) {
                tsort(curTarget, targetTable, state, visiting, complete);
            } else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: "
                    + curTarget);
            }
        }
        log("Complete build sequence is " + complete, MSG_VERBOSE);
        return ret;
    }

    /**
     * Perform a single step in a recursive depth-first-search traversal of
     * the target dependency tree.
     * <p>
     * The current target is first set to the &quot;visiting&quot; state, and
     * pushed onto the &quot;visiting&quot; stack.
     * <p>
     * An exception is then thrown if any child of the current node is in the
     * visiting state, as that implies a circular dependency. The exception
     * contains details of the cycle, using elements of the &quot;visiting&quot;
     * stack.
     * <p>
     * If any child has not already been &quot;visited&quot;, this method is
     * called recursively on it.
     * <p>
     * The current target is then added to the ordered list of targets. Note
     * that this is performed after the children have been visited in order
     * to get the correct order. The current target is set to the
     * &quot;visited&quot; state.
     * <p>
     * By the time this method returns, the ordered list contains the sequence
     * of targets up to and including the current target.
     *
     * @param root The current target to inspect.
     *             Must not be <code>null</code>.
     * @param targetTable A mapping from names to targets (String to Target).
     *                Must not be <code>null</code>.
     * @param state   A mapping from target names to states (String to String).
     *                The states in question are &quot;VISITING&quot; and
     *                &quot;VISITED&quot;. Must not be <code>null</code>.
     * @param visiting A stack of targets which are currently being visited.
     *                 Must not be <code>null</code>.
     * @param ret     The list to add target names to. This will end up
     *                containing the complete list of dependencies in
     *                dependency order.
     *                Must not be <code>null</code>.
     *
     * @exception BuildException if a non-existent target is specified or if
     *                           a circular dependency is detected.
     */
    private void tsort(String root, Hashtable targetTable,
                             Hashtable state, Stack visiting,
                             Vector ret)
        throws BuildException {
        state.put(root, VISITING);
        visiting.push(root);

        Target target = (Target) targetTable.get(root);

        // Make sure we exist
        if (target == null) {
            StringBuffer sb = new StringBuffer("Target \"");
            sb.append(root);
            sb.append("\" does not exist in the project \"");
            sb.append(name);
            sb.append("\". ");
            visiting.pop();
            if (!visiting.empty()) {
                String parent = (String) visiting.peek();
                sb.append("It is used from target \"");
                sb.append(parent);
                sb.append("\".");
            }
            throw new BuildException(new String(sb));
        }
        for (Enumeration en = target.getDependencies(); en.hasMoreElements();) {
            String cur = (String) en.nextElement();
            String m = (String) state.get(cur);
            if (m == null) {
                // Not been visited
                tsort(cur, targetTable, state, visiting, ret);
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
     * Build an appropriate exception detailing a specified circular
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
     * Inherit the id references.
     * @param parent the parent project of this project.
     */
    public void inheritIDReferences(Project parent) {
        parentIdProject = parent;
    }

    /**
     * Attempt to resolve an Unknown Reference using the
     * parsed id's - for BC.
     */
    private Object resolveIdReference(String key, Project callerProject) {
        UnknownElement origUE = (UnknownElement) idReferences.get(key);
        if (origUE == null) {
            return parentIdProject == null
                ? null
                : parentIdProject.resolveIdReference(key, callerProject);
        }
        callerProject.log(
            "Warning: Reference " + key + " has not been set at runtime,"
            + " but was found during" + LINE_SEP
            + "build file parsing, attempting to resolve."
            + " Future versions of Ant may support" + LINE_SEP
            + " referencing ids defined in non-executed targets.", MSG_WARN);
        UnknownElement copyUE = origUE.copy(callerProject);
        copyUE.maybeConfigure();
        return copyUE.getRealThing();
    }

    /**
     * Add an id reference.
     * Used for broken build files.
     * @param id the id to set.
     * @param value the value to set it to (Unknown element in this case.
     */
    public void addIdReference(String id, Object value) {
        idReferences.put(id, value);
    }

    /**
     * Add a reference to the project.
     *
     * @param referenceName The name of the reference. Must not be <code>null</code>.
     * @param value The value of the reference.
     */
    public void addReference(String referenceName, Object value) {
        synchronized (references) {
            Object old = ((AntRefTable) references).getReal(referenceName);
            if (old == value) {
                // no warning, this is not changing anything
                return;
            }
            if (old != null && !(old instanceof UnknownElement)) {
                log("Overriding previous definition of reference to " + referenceName,
                    MSG_VERBOSE);
            }
            log("Adding reference: " + referenceName, MSG_DEBUG);
            references.put(referenceName, value);
        }
    }

    /**
     * Return a map of the references in the project (String to Object).
     * The returned hashtable is &quot;live&quot; and so must not be modified.
     *
     * @return a map of the references in the project (String to Object).
     */
    public Hashtable getReferences() {
        return references;
    }

    /**
     * Look up a reference by its key (ID).
     *
     * @param key The key for the desired reference.
     *            Must not be <code>null</code>.
     *
     * @return the reference with the specified ID, or <code>null</code> if
     *         there is no such reference in the project.
     */
    public Object getReference(String key) {
        Object ret = references.get(key);
        if (ret != null) {
            return ret;
        }
        // Check for old id behaviour
        ret = resolveIdReference(key, this);
        if (ret == null && !key.equals(MagicNames.REFID_PROPERTY_HELPER)) {
            Vector p = new Vector();
            PropertyHelper.getPropertyHelper(this).parsePropertyString(
                key, new Vector(), p);
            if (p.size() == 1) {
                log("Unresolvable reference " + key
                    + " might be a misuse of property expansion syntax.",
                    MSG_WARN);
            }
        }
        return ret;
    }

    /**
     * Return a description of the type of the given element, with
     * special handling for instances of tasks and data types.
     * <p>
     * This is useful for logging purposes.
     *
     * @param element The element to describe.
     *                Must not be <code>null</code>.
     *
     * @return a description of the element type.
     *
     * @since 1.95, Ant 1.5
     */
    public String getElementName(Object element) {
        return ComponentHelper.getComponentHelper(this).getElementName(element);
    }

    /**
     * Send a &quot;build started&quot; event
     * to the build listeners for this project.
     */
    public void fireBuildStarted() {
        BuildEvent event = new BuildEvent(this);
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
            listener.buildStarted(event);
        }
    }

    /**
     * Send a &quot;build finished&quot; event to the build listeners
     * for this project.
     * @param exception an exception indicating a reason for a build
     *                  failure. May be <code>null</code>, indicating
     *                  a successful build.
     */
    public void fireBuildFinished(Throwable exception) {
        BuildEvent event = new BuildEvent(this);
        event.setException(exception);
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
            listener.buildFinished(event);
        }
        // Inform IH to clear the cache
        IntrospectionHelper.clearCache();
    }

    /**
     * Send a &quot;subbuild started&quot; event to the build listeners for
     * this project.
     *
     * @since Ant 1.6.2
     */
    public void fireSubBuildStarted() {
        BuildEvent event = new BuildEvent(this);
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            Object listener = iter.next();
            if (listener instanceof SubBuildListener) {
                ((SubBuildListener) listener).subBuildStarted(event);
            }
        }
    }

    /**
     * Send a &quot;subbuild finished&quot; event to the build listeners for
     * this project.
     * @param exception an exception indicating a reason for a build
     *                  failure. May be <code>null</code>, indicating
     *                  a successful build.
     *
     * @since Ant 1.6.2
     */
    public void fireSubBuildFinished(Throwable exception) {
        BuildEvent event = new BuildEvent(this);
        event.setException(exception);
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            Object listener = iter.next();
            if (listener instanceof SubBuildListener) {
                ((SubBuildListener) listener).subBuildFinished(event);
            }
        }
    }

    /**
     * Send a &quot;target started&quot; event to the build listeners
     * for this project.
     *
     * @param target The target which is starting to build.
     *               Must not be <code>null</code>.
     */
    protected void fireTargetStarted(Target target) {
        BuildEvent event = new BuildEvent(target);
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
            listener.targetStarted(event);
        }
    }

    /**
     * Send a &quot;target finished&quot; event to the build listeners
     * for this project.
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
            listener.targetFinished(event);
        }
    }

    /**
     * Send a &quot;task started&quot; event to the build listeners
     * for this project.
     *
     * @param task The target which is starting to execute.
     *               Must not be <code>null</code>.
     */
    protected void fireTaskStarted(Task task) {
        // register this as the current task on the current thread.
        registerThreadTask(Thread.currentThread(), task);
        BuildEvent event = new BuildEvent(task);
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
            listener.taskStarted(event);
        }
    }

    /**
     * Send a &quot;task finished&quot; event to the build listeners for this
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
            listener.taskFinished(event);
        }
    }

    /**
     * Send a &quot;message logged&quot; event to the build listeners
     * for this project.
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

        if (message.endsWith(StringUtils.LINE_SEP)) {
            int endIndex = message.length() - StringUtils.LINE_SEP.length();
            event.setMessage(message.substring(0, endIndex), priority);
        } else {
            event.setMessage(message, priority);
        }
        synchronized (this) {
            if (loggingMessage) {
                /*
                 * One of the Listeners has attempted to access
                 * System.err or System.out.
                 *
                 * We used to throw an exception in this case, but
                 * sometimes Listeners can't prevent it(like our own
                 * Log4jListener which invokes getLogger() which in
                 * turn wants to write to the console).
                 *
                 * @see http://marc.theaimsgroup.com/?t=110538624200006&r=1&w=2
                 *
                 * We now (Ant 1.7 and 1.6.3) simply swallow the message.
                 */
                return;
            }
            try {
                loggingMessage = true;
                Iterator iter = listeners.iterator();
                while (iter.hasNext()) {
                    BuildListener listener = (BuildListener) iter.next();
                    listener.messageLogged(event);
                }
            } finally {
                loggingMessage = false;
            }
        }
    }

    /**
     * Send a &quot;message logged&quot; project level event
     * to the build listeners for this project.
     *
     * @param project  The project generating the event.
     *                 Should not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    protected void fireMessageLogged(Project project, String message,
                                     int priority) {
        fireMessageLogged(project, message, null, priority);
    }

    /**
     * Send a &quot;message logged&quot; project level event
     * to the build listeners for this project.
     *
     * @param project  The project generating the event.
     *                 Should not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param throwable The exception that caused this message. May be <code>null</code>.
     * @param priority The priority of the message.
     * @since 1.7
     */
    protected void fireMessageLogged(Project project, String message,
            Throwable throwable, int priority) {
        BuildEvent event = new BuildEvent(project);
        event.setException(throwable);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Send a &quot;message logged&quot; target level event
     * to the build listeners for this project.
     *
     * @param target   The target generating the event.
     *                 Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    protected void fireMessageLogged(Target target, String message,
                                     int priority) {
        fireMessageLogged(target, message, null, priority);
    }

    /**
     * Send a &quot;message logged&quot; target level event
     * to the build listeners for this project.
     *
     * @param target   The target generating the event.
     *                 Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param throwable The exception that caused this message. May be <code>null</code>.
     * @param priority The priority of the message.
     * @since 1.7
     */
    protected void fireMessageLogged(Target target, String message,
            Throwable throwable, int priority) {
        BuildEvent event = new BuildEvent(target);
        event.setException(throwable);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Send a &quot;message logged&quot; task level event
     * to the build listeners for this project.
     *
     * @param task     The task generating the event.
     *                 Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param priority The priority of the message.
     */
    protected void fireMessageLogged(Task task, String message, int priority) {
        fireMessageLogged(task, message, null, priority);
    }

    /**
     * Send a &quot;message logged&quot; task level event
     * to the build listeners for this project.
     *
     * @param task     The task generating the event.
     *                 Must not be <code>null</code>.
     * @param message  The message to send. Should not be <code>null</code>.
     * @param throwable The exception that caused this message. May be <code>null</code>.
     * @param priority The priority of the message.
     * @since 1.7
     */
    protected void fireMessageLogged(Task task, String message,
            Throwable throwable, int priority) {
        BuildEvent event = new BuildEvent(task);
        event.setException(throwable);
        fireMessageLoggedEvent(event, message, priority);
    }

    /**
     * Register a task as the current task for a thread.
     * If the task is null, the thread's entry is removed.
     *
     * @param thread the thread on which the task is registered.
     * @param task the task to be registered.
     * @since Ant 1.5
     */
    public synchronized void registerThreadTask(Thread thread, Task task) {
        if (task != null) {
            threadTasks.put(thread, task);
            threadGroupTasks.put(thread.getThreadGroup(), task);
        } else {
            threadTasks.remove(thread);
            threadGroupTasks.remove(thread.getThreadGroup());
        }
    }

    /**
     * Get the current task associated with a thread, if any.
     *
     * @param thread the thread for which the task is required.
     * @return the task which is currently registered for the given thread or
     *         null if no task is registered.
     */
    public Task getThreadTask(Thread thread) {
        Task task = (Task) threadTasks.get(thread);
        if (task == null) {
            ThreadGroup group = thread.getThreadGroup();
            while (task == null && group != null) {
                task = (Task) threadGroupTasks.get(group);
                group = group.getParent();
            }
        }
        return task;
    }


    // Should move to a separate public class - and have API to add
    // listeners, etc.
    private static class AntRefTable extends Hashtable {

        AntRefTable() {
            super();
        }

        /** Returns the unmodified original object.
         * This method should be called internally to
         * get the &quot;real&quot; object.
         * The normal get method will do the replacement
         * of UnknownElement (this is similar with the JDNI
         * refs behavior).
         */
        private Object getReal(Object key) {
            return super.get(key);
        }

        /** Get method for the reference table.
         *  It can be used to hook dynamic references and to modify
         * some references on the fly--for example for delayed
         * evaluation.
         *
         * It is important to make sure that the processing that is
         * done inside is not calling get indirectly.
         *
         * @param key lookup key.
         * @return mapped value.
         */
        public Object get(Object key) {
            //System.out.println("AntRefTable.get " + key);
            Object o = getReal(key);
            if (o instanceof UnknownElement) {
                // Make sure that
                UnknownElement ue = (UnknownElement) o;
                ue.maybeConfigure();
                o = ue.getRealThing();
            }
            return o;
        }
    }

    /**
     * Set a reference to this Project on the parameterized object.
     * Need to set the project before other set/add elements
     * are called.
     * @param obj the object to invoke setProject(this) on.
     */
    public final void setProjectReference(final Object obj) {
        if (obj instanceof ProjectComponent) {
            ((ProjectComponent) obj).setProject(this);
            return;
        }
        try {
            Method method =
                obj.getClass().getMethod(
                    "setProject", new Class[] {Project.class});
            if (method != null) {
                method.invoke(obj, new Object[] {this});
            }
        } catch (Throwable e) {
            // ignore this if the object does not have
            // a set project method or the method
            // is private/protected.
        }
    }

    /**
     * Resolve the file relative to the project's basedir and return it as a
     * FileResource.
     * @param name the name of the file to resolve.
     * @return the file resource.
     * @since Ant 1.7
     */
    public Resource getResource(String name) {
        return new FileResource(getBaseDir(), name);
    }
}
