/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.helper.DefaultExecutor;
import org.apache.tools.ant.helper.KeepGoingExecutor;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Description;
import org.apache.tools.ant.types.Path;
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

    /**
     * The class name of the Ant class loader to use for
     * JDK 1.2 and above
     */
    private static final String ANTCLASSLOADER_JDK12
        = "org.apache.tools.ant.loader.AntClassLoader2";

    /**
     * Version constant for Java 1.0
     *
     * @deprecated Use {@link JavaEnvUtils#JAVA_1_0} instead.
     */
    public static final String JAVA_1_0 = JavaEnvUtils.JAVA_1_0;
    /**
     * Version constant for Java 1.1
     *
     * @deprecated Use {@link JavaEnvUtils#JAVA_1_1} instead.
     */
    public static final String JAVA_1_1 = JavaEnvUtils.JAVA_1_1;
    /**
     * Version constant for Java 1.2
     *
     * @deprecated Use {@link JavaEnvUtils#JAVA_1_2} instead.
     */
    public static final String JAVA_1_2 = JavaEnvUtils.JAVA_1_2;
    /**
     * Version constant for Java 1.3
     *
     * @deprecated Use {@link JavaEnvUtils#JAVA_1_3} instead.
     */
    public static final String JAVA_1_3 = JavaEnvUtils.JAVA_1_3;
    /**
     * Version constant for Java 1.4
     *
     * @deprecated Use {@link JavaEnvUtils#JAVA_1_4} instead.
     */
    public static final String JAVA_1_4 = JavaEnvUtils.JAVA_1_4;

    /** Default filter start token. */
    public static final String TOKEN_START = FilterSet.DEFAULT_TOKEN_START;
    /** Default filter end token. */
    public static final String TOKEN_END = FilterSet.DEFAULT_TOKEN_END;

    /** Name of this project. */
    private String name;
    /** Description for this project (if any). */
    private String description;


    /** Map of references within the project (paths etc) (String to Object). */
    private Hashtable references = new AntRefTable(this);

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
     * The Ant core classloader - may be <code>null</code> if using
     * parent classloader.
     */
    private ClassLoader coreLoader = null;

    /** Records the latest task to be executed on a thread (Thread to Task). */
    private Hashtable threadTasks = new Hashtable();

    /** Records the latest task to be executed on a thread Group. */
    private Hashtable threadGroupTasks = new Hashtable();

    /**
     * Called to handle any input requests.
     */
    private InputHandler inputHandler = null;

    /**
     * The default input stream used to read any input
     */
    private InputStream defaultInputStream = null;

    /**
     * Keep going flag
     */
    private boolean keepGoingMode = false;

    /**
     * Sets the input handler
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
     * Get this project's input stream
     *
     * @return the InputStream instance in use by this Project instance to
     * read input
     */
    public InputStream getDefaultInputStream() {
        return defaultInputStream;
    }

    /**
     * Retrieves the current input handler.
     *
     * @return the InputHandler instance currently in place for the project
     *         instance.
     */
    public InputHandler getInputHandler() {
        return inputHandler;
    }

    /** Instance of a utility class to use for file operations. */
    private FileUtils fileUtils;

    /**
     * Flag which catches Listeners which try to use System.out or System.err
     */
    private boolean loggingMessage = false;

    /**
     * Creates a new Ant project.
     */
    public Project() {
        fileUtils = FileUtils.newFileUtils();
        inputHandler = new DefaultInputHandler();
    }

    /**
     * inits a sub project - used by taskdefs.Ant
     * @param subProject the subproject to initialize
     */
    public void initSubProject(Project subProject) {
        ComponentHelper.getComponentHelper(subProject)
            .initSubProject(ComponentHelper.getComponentHelper(this));
        subProject.setKeepGoingMode(this.isKeepGoingMode());
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

        ComponentHelper.getComponentHelper(this).initDefaultDefinitions();

        setSystemProperties();
    }

    /**
     * Factory method to create a class loader for loading classes
     *
     * @return an appropriate classloader
     */
    private AntClassLoader createClassLoader() {
        AntClassLoader loader = null;
        try {
            // 1.2+ - create advanced helper dynamically
            Class loaderClass
                    = Class.forName(ANTCLASSLOADER_JDK12);
            loader = (AntClassLoader) loaderClass.newInstance();
        } catch (Exception e) {
            log("Unable to create Class Loader: "
                    + e.getMessage(), Project.MSG_DEBUG);
        }

        if (loader == null) {
            loader = new AntClassLoader();
        }

        loader.setProject(this);
        return loader;
    }

    /**
     * Factory method to create a class loader for loading classes from
     * a given path
     *
     * @param path the path from which classes are to be loaded.
     *
     * @return an appropriate classloader
     */
    public AntClassLoader createClassLoader(Path path) {
        AntClassLoader loader = createClassLoader();
        loader.setClassPath(path);
        return loader;
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
    public synchronized void addBuildListener(BuildListener listener) {
        // create a new Vector to avoid ConcurrentModificationExc when
        // the listeners get added/removed while we are in fire
        Vector newListeners = getBuildListeners();
        newListeners.addElement(listener);
        listeners = newListeners;
    }

    /**
     * Removes a build listener from the list. This listener
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
     * Returns a copy of the list of build listeners for the project.
     *
     * @return a list of build listeners for the project
     */
    public Vector getBuildListeners() {
        return (Vector) listeners.clone();
    }

    /**
     * Writes a message to the log with the default log level
     * of MSG_INFO
     * @param message The text to log. Should not be <code>null</code>.
     */

    public void log(String message) {
        log(message, MSG_INFO);
    }

    /**
     * Writes a project level message to the log with the given log level.
     * @param message The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log(String message, int msgLevel) {
        fireMessageLogged(this, message, msgLevel);
    }

    /**
     * Writes a task level message to the log with the given log level.
     * @param task The task to use in the log. Must not be <code>null</code>.
     * @param message The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log(Task task, String message, int msgLevel) {
        fireMessageLogged(task, message, msgLevel);
    }

    /**
     * Writes a target level message to the log with the given log level.
     * @param target The target to use in the log.
     *               Must not be <code>null</code>.
     * @param message The text to log. Should not be <code>null</code>.
     * @param msgLevel The priority level to log at.
     */
    public void log(Target target, String message, int msgLevel) {
        fireMessageLogged(target, message, msgLevel);
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
        PropertyHelper.getPropertyHelper(this).
                setProperty(null, name, value, true);
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
        PropertyHelper.getPropertyHelper(this).setNewProperty(null, name,
                                                              value);
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
        PropertyHelper.getPropertyHelper(this).setUserProperty(null, name,
                                                               value);
    }

    /**
     * Sets a user property, which cannot be overwritten by set/unset
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
     * Sets a property unless it is already defined as a user property
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
     * Returns the value of a property, if it is set.
     *
     * @param name The name of the property.
     *             May be <code>null</code>, in which case
     *             the return value is also <code>null</code>.
     * @return the property value, or <code>null</code> for no match
     *         or if a <code>null</code> name is provided.
     */
    public String getProperty(String name) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return (String) ph.getProperty(null, name);
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
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return ph.replaceProperties(null, value, null);
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
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return (String) ph.getUserProperty(null, name);
    }

    /**
     * Returns a copy of the properties table.
     * @return a hashtable containing all properties
     *         (including user properties).
     */
    public Hashtable getProperties() {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return ph.getProperties();
    }

    /**
     * Returns a copy of the user property hashtable
     * @return a hashtable containing just the user properties
     */
    public Hashtable getUserProperties() {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(this);
        return ph.getUserProperties();
    }

    /**
     * Copies all user properties that have been set on the command
     * line or a GUI tool from this instance to the Project instance
     * given as the argument.
     *
     * <p>To copy all "user" properties, you will also have to call
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
     * Copies all user properties that have not been set on the
     * command line or a GUI tool from this instance to the Project
     * instance given as the argument.
     *
     * <p>To copy all "user" properties, you will also have to call
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
        if (description == null) {
            description = Description.getDescription(this);
        }

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
     * Sets "keep-going" mode. In this mode ANT will try to execute
     * as many targets as possible. All targets that do not depend
     * on failed target(s) will be executed.  If the keepGoing settor/getter
     * methods are used in conjunction with the <code>ant.executor.class</code>
     * property, they will have no effect.
     * @param keepGoingMode "keep-going" mode
     * @since Ant 1.6
     */
    public void setKeepGoingMode(boolean keepGoingMode) {
        this.keepGoingMode = keepGoingMode;
    }

    /**
     * Returns the keep-going mode.  If the keepGoing settor/getter
     * methods are used in conjunction with the <code>ant.executor.class</code>
     * property, they will have no effect.
     * @return "keep-going" mode
     * @since Ant 1.6
     */
    public boolean isKeepGoingMode() {
        return this.keepGoingMode;
    }

    /**
     * Returns the version of Java this class is running under.
     * @return the version of Java as a String, e.g. "1.1"
     * @see org.apache.tools.ant.util.JavaEnvUtils#getJavaVersion
     * @deprecated use org.apache.tools.ant.util.JavaEnvUtils instead
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
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_0)) {
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
        Enumeration e = systemP.propertyNames();
        while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            String value = systemP.getProperty(propertyName);
            this.setPropertyInternal(propertyName, value);
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
        ComponentHelper.getComponentHelper(this).addTaskDefinition(taskName,
                taskClass);
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
     * Returns the current task definition hashtable. The returned hashtable is
     * "live" and so should not be modified.
     *
     * @return a map of from task name to implementing class
     *         (String to Class).
     */
    public Hashtable getTaskDefinitions() {
        return ComponentHelper.getComponentHelper(this).getTaskDefinitions();
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
        ComponentHelper.getComponentHelper(this).addDataTypeDefinition(typeName,
                typeClass);
    }

    /**
     * Returns the current datatype definition hashtable. The returned
     * hashtable is "live" and so should not be modified.
     *
     * @return a map of from datatype name to implementing class
     *         (String to Class).
     */
    public Hashtable getDataTypeDefinitions() {
        return ComponentHelper.getComponentHelper(this).getDataTypeDefinitions();
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
        addTarget(target.getName(), target);
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
     * Creates a new instance of a task, adding it to a list of
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
        return ComponentHelper.getComponentHelper(this).createDataType(typeName);
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

        Object o = getReference("ant.executor");
        if (o == null) {
            String classname = getProperty("ant.executor.class");
            if (classname == null) {
                classname = (keepGoingMode)
                    ? KeepGoingExecutor.class.getName()
                    : DefaultExecutor.class.getName();
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
            if (o != null) {
                addReference("ant.executor", o);
            }
        }

        if (o == null) {
            throw new BuildException("Unable to obtain a Target Executor instance.");
        } else {
            String[] targetNameArray = (String[]) (targetNames.toArray(
                new String[targetNames.size()]));
            ((Executor) o).executeTargets(this, targetNameArray);
        }
    }

    /**
     * Demultiplexes output so that each task receives the appropriate
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
     * @param length the amount of data to read
     *
     * @return the number of bytes read
     *
     * @exception IOException if the data cannot be read
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
     * @param length the amount of data to read
     *
     * @return the number of bytes read
     *
     * @exception IOException if the data cannot be read
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
     * Demultiplexes flush operation so that each task receives the appropriate
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

        // Sort and run the dependency tree.
        // Sorting checks if all the targets (and dependencies)
        // exist, and if there is any cycle in the dependency
        // graph.
        executeSortedTargets(topoSort(targetName, targets, false));
    }

    /**
     * Executes a <code>Vector</code> of sorted targets.
     * @param sortedTargets   the aforementioned <code>Vector</code>.
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
        fileUtils.setFileLastModified(file, time);
        log("Setting modification time for " + file, MSG_VERBOSE);
    }

    /**
     * Returns the boolean equivalent of a string, which is considered
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
     * Topologically sorts a set of targets.  Equivalent to calling
     * <code>topoSort(new String[] {root}, targets, true)</code>.
     *
     * @param root The name of the root target. The sort is created in such
     *             a way that the sequence of Targets up to the root
     *             target is the minimum possible such sequence.
     *             Must not be <code>null</code>.
     * @param targets A Hashtable mapping names to Targets.
     *                Must not be <code>null</code>.
     * @return a Vector of ALL Target objects in sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     */
    public final Vector topoSort(String root, Hashtable targets)
        throws BuildException {
        return topoSort(new String[] {root}, targets, true);
    }

    /**
     * Topologically sorts a set of targets.  Equivalent to calling
     * <code>topoSort(new String[] {root}, targets, returnAll)</code>.
     *
     * @param root The name of the root target. The sort is created in such
     *             a way that the sequence of Targets up to the root
     *             target is the minimum possible such sequence.
     *             Must not be <code>null</code>.
     * @param targets A Hashtable mapping names to Targets.
     *                Must not be <code>null</code>.
     * @param returnAll <code>boolean</code> indicating whether to return all
     *                  targets, or the execution sequence only.
     * @return a Vector of Target objects in sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     * @since Ant 1.6.3
     */
    public final Vector topoSort(String root, Hashtable targets,
                                 boolean returnAll) throws BuildException {
        return topoSort(new String[] {root}, targets, returnAll);
    }

    /**
     * Topologically sorts a set of targets.
     *
     * @param root <code>String[]</code> containing the names of the root targets.
     *             The sort is created in such a way that the ordered sequence of
     *             Targets is the minimum possible such sequence to the specified
     *             root targets.
     *             Must not be <code>null</code>.
     * @param targets A map of names to targets (String to Target).
     *                Must not be <code>null</code>.
     * @param returnAll <code>boolean</code> indicating whether to return all
     *                  targets, or the execution sequence only.
     * @return a Vector of Target objects in sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     *                           targets, or if a named target does not exist.
     * @since Ant 1.6.3
     */
    public final Vector topoSort(String[] root, Hashtable targets,
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
                tsort(root[i], targets, state, visiting, ret);
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
        for (Enumeration en = targets.keys(); en.hasMoreElements();) {
            String curTarget = (String) en.nextElement();
            String st = (String) state.get(curTarget);
            if (st == null) {
                tsort(curTarget, targets, state, visiting, complete);
            } else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: "
                    + curTarget);
            }
        }
        log("Complete build sequence is " + complete, MSG_VERBOSE);
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
     *                containing the complete list of dependencies in
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
        synchronized (references) {
            Object old = ((AntRefTable) references).getReal(name);
            if (old == value) {
                // no warning, this is not changing anything
                return;
            }
            if (old != null && !(old instanceof UnknownElement)) {
                log("Overriding previous definition of reference to " + name,
                    MSG_WARN);
            }
            log("Adding reference: " + name, MSG_DEBUG);
            references.put(name, value);
        }
    }

    /**
     * Returns a map of the references in the project (String to Object).
     * The returned hashtable is "live" and so must not be modified.
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
        return ComponentHelper.getComponentHelper(this).getElementName(element);
    }

    /**
     * Sends a "build started" event to the build listeners for this project.
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
     * Sends a "build finished" event to the build listeners for this project.
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
    }

    /**
     * Sends a "subbuild started" event to the build listeners for
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
     * Sends a "subbuild finished" event to the build listeners for
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
     * Sends a "target started" event to the build listeners for this project.
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            BuildListener listener = (BuildListener) iter.next();
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

        if (message.endsWith(StringUtils.LINE_SEP)) {
            int endIndex = message.length() - StringUtils.LINE_SEP.length();
            event.setMessage(message.substring(0, endIndex), priority);
        } else {
            event.setMessage(message, priority);
        }
        synchronized (this) {
            if (loggingMessage) {
                throw new BuildException("Listener attempted to access "
                    + (priority == MSG_ERR ? "System.err" : "System.out")
                    + " with message [" + message
                    + "] - infinite loop terminated");
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
     * Get the current task associated with a thread, if any
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
        private Project project;

        public AntRefTable(Project project) {
            super();
            this.project = project;
        }

        /** Returns the unmodified original object.
         * This method should be called internally to
         * get the 'real' object.
         * The normal get method will do the replacement
         * of UnknownElement ( this is similar with the JDNI
         * refs behavior )
         */
        public Object getReal(Object key) {
            return super.get(key);
        }

        /** Get method for the reference table.
         *  It can be used to hook dynamic references and to modify
         * some references on the fly - for example for delayed
         * evaluation.
         *
         * It is important to make sure that the processing that is
         * done inside is not calling get indirectly.
         *
         * @param key
         * @return
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
     * are called
     * @param obj the object to invoke setProject(this) on
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
}
