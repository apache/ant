/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.apache.tools.ant.types.DataTypeAdapterTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LazyHashtable;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.taskdefs.Antlib;
import org.apache.tools.ant.input.InputHandler;

/**
 *  Central representation of an Ant project. This class defines a Ant project
 *  with all of it's targets and tasks. It also provides the mechanism to kick
 *  off a build using a particular target name. <p>
 *
 *  This class also encapsulates methods which allow Files to be refered to
 *  using abstract path names which are translated to native system file paths
 *  at runtime as well as defining various project properties.
 *
 *@author     duncan@x180.com
 *@author     j_a_fernandez@yahoo.com
 *@created    February 27, 2002
 */

public class Project {

    /**
     *  Description of the Field
     */
    public final static int MSG_ERR = 0;
    /**
     *  Description of the Field
     */
    public final static int MSG_WARN = 1;
    /**
     *  Description of the Field
     */
    public final static int MSG_INFO = 2;
    /**
     *  Description of the Field
     */
    public final static int MSG_VERBOSE = 3;
    /**
     *  Description of the Field
     */
    public final static int MSG_DEBUG = 4;

    /**
     *  LoaderId for the CoreLoader.
     */
    public final static String CORELOADER_ID = null;

    /**
     *  Description of the Field
     */
    public final static String TASK_ROLE = "task";
    /**
     *  Description of the Field
     */
    public final static String DATATYPE_ROLE = "data-type";

    // private set of constants to represent the state
    // of a DFS of the Target dependencies
    private final static String VISITING = "VISITING";
    private final static String VISITED = "VISITED";

    private static String javaVersion;

    /**
     * The class name of the Ant class loader to use for
     * JDK 1.2 and above
     */
    private static final String ANTCLASSLOADER_JDK12
        = "org.apache.tools.ant.loader.AntClassLoader2";

    /**
     *  Description of the Field
     */
    public final static String JAVA_1_0 = "1.0";
    /**
     *  Description of the Field
     */
    public final static String JAVA_1_1 = "1.1";
    /**
     *  Description of the Field
     */
    public final static String JAVA_1_2 = "1.2";
    /**
     *  Description of the Field
     */
    public final static String JAVA_1_3 = "1.3";
    /**
     *  Description of the Field
     */
    public final static String JAVA_1_4 = "1.4";

    /**
     *  Description of the Field
     */
    public final static String TOKEN_START = FilterSet.DEFAULT_TOKEN_START;
    /**
     *  Description of the Field
     */
    public final static String TOKEN_END = FilterSet.DEFAULT_TOKEN_END;

    private final static String CORE_DEFINITIONS = "org/apache/tools/ant/antlib.xml";

    private String name;
    private String description;
    /** Map of references within the project (paths etc) (String to Object). */

    private Hashtable properties = new Hashtable();
    private Hashtable userProperties = new Hashtable();
    private Hashtable references = new AntRefTable(this);
    private String defaultTarget;
    /** Map from data type names to implementing classes (String to Class). */
    private Hashtable dataClassDefinitions = new AntTaskTable(this, false);
    /** Map from task names to implementing classes (String to Class). */
    private Hashtable taskClassDefinitions = new AntTaskTable(this, true);
    private Hashtable createdTasks = new Hashtable();
    private Hashtable targets = new Hashtable();
    private FilterSet globalFilterSet = new FilterSet();
    private FilterSetCollection globalFilters = new FilterSetCollection(globalFilterSet);
    private File baseDir;

    private Vector listeners = new Vector();

    /**
     *  The Ant core classloader - may be null if using system loader
     */
    private ClassLoader coreLoader = null;

    /** Records the latest task to be executed on a thread (Thread to Task). */
    private Hashtable threadTasks = new Hashtable();

    /** Records the latest task to be executed on a thread Group. */
    private Hashtable threadGroupTasks = new Hashtable();


    /**
     *  Store symbol tables
     */
    private SymbolTable symbols;

    static {

        // Determine the Java version by looking at available classes
        // java.lang.StrictMath was introduced in JDK 1.3
        // java.lang.ThreadLocal was introduced in JDK 1.2
        // java.lang.Void was introduced in JDK 1.1
        // Count up version until a NoClassDefFoundError ends the try

        try {
            javaVersion = JAVA_1_0;
            Class.forName("java.lang.Void");
            javaVersion = JAVA_1_1;
            Class.forName("java.lang.ThreadLocal");
            javaVersion = JAVA_1_2;
            Class.forName("java.lang.StrictMath");
            javaVersion = JAVA_1_3;
            Class.forName("java.lang.CharSequence");
            javaVersion = JAVA_1_4;
        } catch (ClassNotFoundException cnfe) {
            // swallow as we've hit the max class version that
            // we have
        }
    }

    /**
     * Called to handle any input requests.
     */
    private InputHandler inputHandler = null;

    /**
     * The default input stream used to read any input
     */
    private InputStream defaultInputStream = null;

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
     * System.in. This inputStream is used when no task inptu redirection is
     * being performed.
     *
     * @param defaultInputStream the default input stream to use when input
     *        is reuested.
     * @since Ant 1.6
     */
    public void setDefaultInputStream(InputStream defaultInputStream) {
        this.defaultInputStream = defaultInputStream;
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

    private FileUtils fileUtils;


    /**
     *  <p>
     *
     *  Description: The only reason for this class is to make the
     *  LoadDefinition method visible in this package.</p>
     *
     *@author     jfernandez
     *@created    February 27, 2002
     */
    private class Corelib extends Antlib {
        /**
         *  Constructor for the Corelib object
         */
        Corelib() {
            super(Project.this);
            setLoaderid(CORELOADER_ID);
            getOnerror().setValue("ignore");
        }


        /**
         *  Description of the Method
         */
        public void loadCoreDefinitions() {
            getOnerror().setValue("report");
            super.loadDefinitions(CORE_DEFINITIONS);
        }
    }


    /**
     *  create a new ant project
     */
    public Project() {
        fileUtils = FileUtils.newFileUtils();
        symbols = new SymbolTable();
        symbols.setProject(this);
    }


    /**
     *  create a new ant project that inherits from caller project
     *
     *@param  p  the calling project
     */
    private Project(Project p) {
        fileUtils = FileUtils.newFileUtils();
        symbols = new SymbolTable(p.getSymbols());
        symbols.setProject(this);
        setCoreLoader(p.getCoreLoader());
    }


    /**
     *  Loads the core definitions into the Root project.
     */
    private void loadDefinitions() {
        // Initialize symbol table just in case
        symbols.addRole(TASK_ROLE, TaskContainer.class, TaskAdapter.class);
        symbols.addRole(DATATYPE_ROLE, TaskContainer.class,
                DataTypeAdapterTask.class);

        Corelib load = new Corelib();
        load.loadDefinitions();

        // If the most basic of tasks, "property", is not defined
        // then there was no antlib jars from where to load the descriptors
        // we should be doing bootstrapping, or ant.jar is not an antlib.
        if (!isDefinedOnRole(TASK_ROLE, "property")) {
            load.loadCoreDefinitions();

            if (!isDefinedOnRole(TASK_ROLE, "property")) {
                throw new BuildException("Can't load core definitions");
            }
        }
        autoLoadDefinitions();
    }

    private void autoLoadDefinitions() {
        DirectoryScanner ds = new DirectoryScanner();
        try {
            File autolib=new File(getProperty("ant.home"),"autolib");
            log("scanning the autolib directory "+autolib.toString(),MSG_DEBUG);
            ds.setBasedir(autolib);
            ds.scan();
            String dirs[] = ds.getIncludedDirectories();
            for (int i = 0; i < dirs.length; i++) {
                autoLoad(ds.getBasedir(), dirs[i]);
            }
        } catch (Exception e) {}

    }

    private void autoLoad(File base, String dir) {
        FileSet fs = new FileSet();
        fs.setProject(this);
        fs.setDir(new File(base, dir));
        fs.setIncludes("*.jar");

        Path cp = new Path(this);
        cp.addFileset(fs);
        if (cp.size() == 0) {
            return;
        }

        Antlib.FailureAction fa = new Antlib.FailureAction();
        fa.setValue("ignore");

        Antlib lib = new Antlib(this);
        lib.setClasspath(cp);
        lib.setLoaderid(dir);
        lib.setOnerror(fa);
        lib.loadDefinitions();
    }

    /**
     *  Creates a subproject of the current project.
     *
     *@return    Description of the Return Value
     */
    public Project createSubProject() {
        return new Project(this);
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
        if (!isRoleDefined(TASK_ROLE)) {
            // Top project, need to load the core definitions
            loadDefinitions();
        }
        String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";

        try {
            Properties props = new Properties();
            InputStream in = this.getClass().getResourceAsStream(defs);
            if (in == null) {
                throw new BuildException("Can't load default task list");
            }
            props.load(in);
            in.close();
            ((AntTaskTable) taskClassDefinitions).addDefinitions(props);


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

            ((AntTaskTable) dataClassDefinitions).addDefinitions(props);


        } catch (IOException ioe) {
            throw new BuildException("Can't load default datatype list");
        }

        setSystemProperties();
    }



    /**
     *  Sets the CoreLoader to the default of the Project object
     */
    private void setDefaultCoreLoader() {
        coreLoader = this.getClass().getClassLoader();
        if (coreLoader == null) {
            // This should only happen if ANT is being
            // loader by the Bootstrap classloader
            // This may be the case in JDK 1.1
            coreLoader = ClassLoader.getSystemClassLoader();
        }
    }


    /**
     * Factory method to create a class loader for loading classes
     *
     * @return an appropriate classloader
     */
    private AntClassLoader createClassLoader() {
        AntClassLoader loader = null;
        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            try {
                // 1.2+ - create advanced helper dynamically
                Class loaderClass
                    = Class.forName(ANTCLASSLOADER_JDK12);
                loader = (AntClassLoader) loaderClass.newInstance();
            } catch (Exception e) {
                    log("Unable to create Class Loader: "
                        + e.getMessage(), Project.MSG_DEBUG);
            }
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
     * @param path the path from whcih clases are to be loaded.
     *
     * @return an appropriate classloader
     */
    public AntClassLoader createClassLoader(Path path) {
        AntClassLoader loader = createClassLoader();
        loader.setClassPath(path);
        return loader;
    }

    /**
     *  Sets the coreLoader attribute of the Project object
     *
     *@param  coreLoader  The new coreLoader value
     */
    public void setCoreLoader(ClassLoader coreLoader) {
        if (coreLoader == null) {
            setDefaultCoreLoader();
        }
        else this.coreLoader = coreLoader;
    }


    /**
     *  Gets the coreLoader attribute of the Project object
     *
     *@return    The coreLoader value
     */
    public ClassLoader getCoreLoader() {
        if (coreLoader == null) {
            setDefaultCoreLoader();
        }
        return coreLoader;
    }


    /**
     *  Adds a feature to the BuildListener attribute of the Project object
     *
     *@param  listener  The feature to be added to the BuildListener attribute
     */
    public void addBuildListener(BuildListener listener) {
        listeners.addElement(listener);
    }


    /**
     *  Description of the Method
     *
     *@param  listener  Description of the Parameter
     */
    public void removeBuildListener(BuildListener listener) {
        listeners.removeElement(listener);
    }


    /**
     *  Gets the buildListeners attribute of the Project object
     *
     *@return    The buildListeners value
     */
    public Vector getBuildListeners() {
        return listeners;
    }


    /**
     *  Get the symbols associated with this project.
     *
     *@return    The symbols value
     */
    private SymbolTable getSymbols() {
        // Package protected on purpose
        return symbols;
    }


    /**
     *  Output a message to the log with the default log level of MSG_INFO
     *
     *@param  msg  text to log
     */

    public void log(String msg) {
        log(msg, MSG_INFO);
    }


    /**
     *  Output a message to the log with the given log level and an event scope
     *  of project
     *
     *@param  msg       text to log
     *@param  msgLevel  level to log at
     */
    public void log(String msg, int msgLevel) {
        fireMessageLogged(this, msg, msgLevel);
    }


    /**
     *  Output a message to the log with the given log level and an event scope
     *  of a task
     *
     *@param  task      task to use in the log
     *@param  msg       text to log
     *@param  msgLevel  level to log at
     */
    public void log(Task task, String msg, int msgLevel) {
        fireMessageLogged(task, msg, msgLevel);
    }


    /**
     *  Output a message to the log with the given log level and an event scope
     *  of a target
     *
     *@param  target    target to use in the log
     *@param  msg       text to log
     *@param  msgLevel  level to log at
     */
    public void log(Target target, String msg, int msgLevel) {
        fireMessageLogged(target, msg, msgLevel);
    }


    /**
     *  Gets the globalFilterSet attribute of the Project object
     *
     *@return    The globalFilterSet value
     */
    public FilterSet getGlobalFilterSet() {
        return globalFilterSet;
    }


    /**
     *  set a property. Any existing property of the same name is overwritten,
     *  unless it is a user property.
     *
     *@param  name   name of property
     *@param  value  new value of the property
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
     *  set a property. An existing property of the same name will not be
     *  overwritten.
     *
     *@param  name   name of property
     *@param  value  new value of the property
     *@since         1.5
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
     *  set a user property, which can not be overwritten by set/unset property
     *  calls
     *
     *@param  name   The new userProperty value
     *@param  value  The new userProperty value
     *@see           #setProperty(String,String)
     */
    public void setUserProperty(String name, String value) {
        log("Setting ro project property: " + name + " -> " +
                value, MSG_DEBUG);
        userProperties.put(name, value);
        properties.put(name, value);
    }


    /**
     *  Allows Project and subclasses to set a property unless its already
     *  defined as a user property. There are a few cases internally to Project
     *  that need to do this currently.
     *
     *@param  name   The new propertyInternal value
     *@param  value  The new propertyInternal value
     */
    private void setPropertyInternal(String name, String value) {
        if (null != userProperties.get(name)) {
            return;
        }
        properties.put(name, value);
    }


    /**
     *  query a property.
     *
     *@param  name  the name of the property
     *@return       the property value, or null for no match
     */
    public String getProperty(String name) {
        if (name == null) {
            return null;
        }
        String property = (String) properties.get(name);
        return property;
    }


    /**
     *  Replace ${} style constructions in the given value with the string value
     *  of the corresponding data types.
     *
     *@param  value               the string to be scanned for property
     *      references.
     *@return                     Description of the Return Value
     *@exception  BuildException  Description of the Exception
     */
    public String replaceProperties(String value)
             throws BuildException {
        return ProjectHelper.replaceProperties(this, value, properties);
    }


    /**
     *  query a user property.
     *
     *@param  name  the name of the property
     *@return       the property value, or null for no match
     */
    public String getUserProperty(String name) {
        if (name == null) {
            return null;
        }
        String property = (String) userProperties.get(name);
        return property;
    }


    /**
     *  get a copy of the property hashtable
     *
     *@return    the hashtable containing all properties, user included
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
     *  get a copy of the user property hashtable
     *
     *@return    the hashtable user properties only
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
     *  set the default target of the project
     *
     *@param  defaultTarget  The new defaultTarget value
     *@deprecated,           use setDefault
     *@see                   #setDefault(String)
     */
    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }


    /**
     *  get the default target of the project
     *
     *@return    default target or null
     */
    public String getDefaultTarget() {
        return defaultTarget;
    }


    /**
     *  set the default target of the project XML attribute name.
     *
     *@param  defaultTarget  The new default value
     */
    public void setDefault(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }


    /**
     *  ant xml property. Set the project name as an attribute of this class,
     *  and of the property ant.project.name
     *
     *@param  name  The new name value
     */
    public void setName(String name) {
        setUserProperty("ant.project.name", name);
        this.name = name;
    }


    /**
     *  get the project name
     *
     *@return    name string
     */
    public String getName() {
        return name;
    }


    /**
     *  set the project description
     *
     *@param  description  text
     */
    public void setDescription(String description) {
        this.description = description;
    }


    /**
     *  get the project description
     *
     *@return    description or null if no description has been set
     */
    public String getDescription() {
        return description;
    }


    /**
     *@param  token  The feature to be added to the Filter attribute
     *@param  value  The feature to be added to the Filter attribute
     *@deprecated
     */
    public void addFilter(String token, String value) {
        if (token == null) {
            return;
        }

        globalFilterSet.addFilter(new FilterSet.Filter(token, value));
    }


    /**
     *@return        The filters value
     *@deprecated
     */
    public Hashtable getFilters() {
        // we need to build the hashtable dynamically
        return globalFilterSet.getFilterHash();
    }


    /**
     *  match basedir attribute in xml
     *
     *@param  baseD            project base directory.
     *@throws  BuildException  if the directory was invalid
     */
    public void setBasedir(String baseD) throws BuildException {
        setBaseDir(new File(baseD));
    }


    /**
     *  set the base directory; XML attribute. checks for the directory existing
     *  and being a directory type
     *
     *@param  baseDir          project base directory.
     *@throws  BuildException  if the directory was invalid
     */
    public void setBaseDir(File baseDir) throws BuildException {
        baseDir = fileUtils.normalize(baseDir.getAbsolutePath());
        if (!baseDir.exists()) {
            throw new BuildException("Basedir " + baseDir.getAbsolutePath() + " does not exist");
        }
        if (!baseDir.isDirectory()) {
            throw new BuildException("Basedir " + baseDir.getAbsolutePath() + " is not a directory");
        }
        this.baseDir = baseDir;
        setPropertyInternal("basedir", this.baseDir.getPath());
        String msg = "Project base dir set to: " + this.baseDir;
        log(msg, MSG_VERBOSE);
    }


    /**
     *  get the base directory of the project as a file object
     *
     *@return    the base directory. If this is null, then the base dir is not
     *      valid
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
     *  static query of the java version
     *
     *@return    something like "1.1" or "1.3"
     */
    public static String getJavaVersion() {
        return javaVersion;
    }


    /**
     *  set the ant.java.version property, also tests for unsupported JVM
     *  versions, prints the verbose log messages
     *
     *@throws  BuildException  if this Java version is not supported
     */
    public void setJavaVersionProperty() throws BuildException {
        setPropertyInternal("ant.java.version", javaVersion);

        // sanity check
        if (javaVersion == JAVA_1_0) {
            throw new BuildException("Ant cannot work on Java 1.0");
        }

        log("Detected Java version: " + javaVersion + " in: " + System.getProperty("java.home"), MSG_VERBOSE);

        log("Detected OS: " + System.getProperty("os.name"), MSG_VERBOSE);
    }


    /**
     *  turn all the system properties into ant properties. user properties
     *  still override these values
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
     *  Adds a feature to the ToLoader attribute of the Project object
     *
     *@param  loader  The feature to be added to the ToLoader attribute
     *@param  path    The feature to be added to the ToLoader attribute
     *@return         Description of the Return Value
     */
    public ClassLoader addToLoader(String loader, Path path) {
        if (loader == CORELOADER_ID) {
            // It is not possible to add more libraries to the CoreLoader
            // Just return it as is.
            return getCoreLoader();
        }
        return symbols.addToLoader(loader, path);
    }


    /**
     *  Adds a feature to the RoleDefinition attribute of the Project object
     *
     *@param  role       The feature to be added to the RoleDefinition attribute
     *@param  roleClass  The feature to be added to the RoleDefinition attribute
     *@param  adapter    The feature to be added to the RoleDefinition attribute
     *@return            Description of the Return Value
     */
    public boolean addRoleDefinition(String role,
            Class roleClass, Class adapter) {
        return symbols.addRole(role, roleClass, adapter);
    }


    /**
     *  test for a role name being in use already
     *
     *@param  name  the name to test
     *@return       true if it is a task or a datatype
     */
    public boolean isRoleDefined(String name) {
        return (symbols.getRole(name) != null);
    }


    /**
     *  Adds a feature to the DefinitionOnRole attribute of the Project object
     *
     *@param  role  The feature to be added to the DefinitionOnRole attribute
     *@param  type  The feature to be added to the DefinitionOnRole attribute
     *@param  clz   The feature to be added to the DefinitionOnRole attribute
     */
    public void addDefinitionOnRole(String role,
            String type, Class clz) {
        Class old = symbols.add(role, type, clz);
        // Special management for Tasks
        if (TASK_ROLE.equals(role) && null != old && !old.equals(clz)) {
            invalidateCreatedTasks(type);
        }
    }


    /**
     *  test for a name being in use already on this role
     *
     *@param  name  the name to test
     *@param  role  Description of the Parameter
     *@return       true if it is a task or a datatype
     */
    public boolean isDefinedOnRole(String role, String name) {
        return (symbols.get(role, name) != null);
    }


    /**
     *  add a new task definition, complain if there is an overwrite attempt
     *
     *@param  taskName         name of the task
     *@param  taskClass        full task classname
     *@throws  BuildException  and logs as Project.MSG_ERR for conditions, that
     *      will cause the task execution to fail.
     */
    public void addTaskDefinition(String taskName, Class taskClass)
             throws BuildException {
        addDefinitionOnRole(TASK_ROLE, taskName, taskClass);
    }


    /**
     *  Checks a class, whether it is suitable for serving as ant task.
     *
     *@param  taskClass        Description of the Parameter
     *@throws  BuildException  and logs as Project.MSG_ERR for conditions, that
     *      will cause the task execution to fail.
     *@deprecated              this is done now when added to SymbolTable
     */
    public void checkTaskClass(final Class taskClass) throws BuildException {
        if (!Task.class.isAssignableFrom(taskClass)) {
            TaskAdapter.checkTaskClass(taskClass, this);
        }
    }


    /**
     *  get the current task definition hashtable
     *
     *@return    The taskDefinitions value
     */
    public Hashtable getTaskDefinitions() {
        return symbols.getDefinitions(TASK_ROLE);
    }


    /**
     *  add a new datatype
     *
     *@param  typeName   name of the datatype
     *@param  typeClass  full datatype classname
     */
    public void addDataTypeDefinition(String typeName, Class typeClass) {
        addDefinitionOnRole(DATATYPE_ROLE, typeName, typeClass);
    }


    /**
     *  get the current task definition hashtable
     *
     *@return    The dataTypeDefinitions value
     */
    public Hashtable getDataTypeDefinitions() {
        return symbols.getDefinitions(DATATYPE_ROLE);
    }


    /**
     *  This call expects to add a <em>new</em> Target.
     *
     *@param  target              is the Target to be added to the current
     *      Project.
     *@see                        Project#addOrReplaceTarget to replace existing
     *      Targets.
     */
    public void addTarget(Target target) {
        String name = target.getName();
        if (targets.get(name) != null) {
            throw new BuildException("Duplicate target: `" + name + "'");
        }
        addOrReplaceTarget(name, target);
    }


    /**
     *  This call expects to add a <em>new</em> Target.
     *
     *@param  target              is the Target to be added to the current
     *      Project.
     *@param  targetName          is the name to use for the Target
     *@exception  BuildException  if the Target already exists in the project.
     *@see                        Project#addOrReplaceTarget to replace existing
     *      Targets.
     */
    public void addTarget(String targetName, Target target)
             throws BuildException {
        if (targets.get(targetName) != null) {
            throw new BuildException("Duplicate target: `" + targetName + "'");
        }
        addOrReplaceTarget(targetName, target);
    }


    /**
     *@param  target  is the Target to be added or replaced in the current
     *      Project.
     */
    public void addOrReplaceTarget(Target target) {
        addOrReplaceTarget(target.getName(), target);
    }


    /**
     *@param  target      is the Target to be added/replaced in the current
     *      Project.
     *@param  targetName  is the name to use for the Target
     */
    public void addOrReplaceTarget(String targetName, Target target) {
        String msg = " +Target: " + targetName;
        log(msg, MSG_DEBUG);
        target.setProject(this);
        targets.put(targetName, target);
    }


    /**
     *  get the target hashtable
     *
     *@return    hashtable, the contents of which can be cast to Target
     */
    public Hashtable getTargets() {
        return targets;
    }


    /**
     *  Create a new element instance on a Role
     *
     *@param  role  name of the role to use
     *@param  type  name of the element to create
     *@return       null if element unknown on this role
     */
    public Object createForRole(String role, String type) {
        SymbolTable.Factory f = symbols.get(role, type);
        if (f == null) {
            return null;
        }

        try {
            Object o = f.create(this);
            // Do special book keeping for ProjectComponents
            if (o instanceof ProjectComponent) {
                ((ProjectComponent) o).setProject(this);
                if (o instanceof Task) {
                    Task task = (Task) o;
                    task.setTaskType(type);

                    // set default value, can be changed by the user
                    task.setTaskName(type);
                    addCreatedTask(type, task);
                }
            }
            String msg = "   +" + role + ": " + type;
            log(msg, MSG_DEBUG);
            return o;
        } catch (Throwable t) {
            String msg = "Could not create " + role + " of type: "
                     + type + " due to " + t;
            throw new BuildException(msg, t);
        }
    }


    /**
     *@param  container  Description of the Parameter
     *@param  type       Description of the Parameter
     *@return            Description of the Return Value
     */
    public Object createInRole(Object container, String type) {
        Class clz = container.getClass();
        String roles[] = symbols.findRoles(clz);
        Object theOne = null;
        Method add = null;

        for (int i = 0; i < roles.length; i++) {
            Object o = createForRole(roles[i], type);
            if (o != null) {
                if (theOne != null) {
                    String msg = "Element " + type +
                            " is ambiguous for container " + clz.getName();
                    if (theOne instanceof RoleAdapter) {
                        theOne = ((RoleAdapter) theOne).getProxy();
                    }
                    if (o instanceof RoleAdapter) {
                        o = ((RoleAdapter) o).getProxy();
                    }

                    log(msg, MSG_ERR);
                    log("cannot distinguish between " +
                            theOne.getClass().getName() +
                            " and " + o.getClass().getName(), MSG_ERR);
                    throw new BuildException(msg);
                }
                theOne = o;
                add = symbols.getRole(roles[i]).getInterfaceMethod();
            }
        }
        if (theOne != null) {
            try {
                add.invoke(container, new Object[]{theOne});
            } catch (InvocationTargetException ite) {
                if (ite.getTargetException() instanceof BuildException) {
                    throw (BuildException) ite.getTargetException();
                }
                throw new BuildException(ite.getTargetException());
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
        return theOne;
    }


    /**
     *  create a new task instance
     *
     *@param  taskType         name of the task
     *@return                  null if the task name is unknown
     *@throws  BuildException  when task creation goes bad
     */
    public Task createTask(String taskType) throws BuildException {
        return (Task) createForRole(TASK_ROLE, taskType);
    }


    /**
     *  Keep a record of all tasks that have been created so that they can be
     *  invalidated if a taskdef overrides the definition.
     *
     *@param  type  The feature to be added to the CreatedTask attribute
     *@param  task  The feature to be added to the CreatedTask attribute
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
     *  Mark tasks as invalid which no longer are of the correct type for a
     *  given taskname.
     *
     *@param  type  Description of the Parameter
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
     *  create a new DataType instance
     *
     *@param  typeName         name of the datatype
     *@return                  null if the datatype name is unknown
     *@throws  BuildException  when datatype creation goes bad
     */
    public Object createDataType(String typeName) throws BuildException {
        // This is to make the function backward compatible
        // Since we know if it returning an adapter for it
        DataTypeAdapterTask dt =
                (DataTypeAdapterTask) createForRole(DATATYPE_ROLE, typeName);
        return (dt != null ? dt.getProxy() : null);
    }


    /**
     *  execute the sequence of targets, and the targets they depend on
     *
     *@param  targetNames      Description of the Parameter
     *@throws  BuildException  if the build failed
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
        Task task = getThreadTask(Thread.currentThread());
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
            return defaultInputStream.read(buffer, offset, length);
        } else {
            return System.in.read(buffer, offset, length);
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
     * @param line Message to handle. Should not be <code>null</code>.
     * @param isError Whether the text represents an error (<code>true</code>)
     *        or information (<code>false</code>).
     */
    public void demuxFlush(String line, boolean isError) {
        Task task = getThreadTask(Thread.currentThread());
        if (task == null) {
            fireMessageLogged(this, line, isError ? MSG_ERR : MSG_INFO);
        } else {
            if (isError) {
                task.handleErrorFlush(line);
            } else {
                task.handleFlush(line);
            }
        }
    }



    /**
     *  execute the targets and any targets it depends on
     *
     *@param  targetName       the target to execute
     *@throws  BuildException  if the build failed
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
     *  Return the canonical form of fileName as an absolute path. <p>
     *
     *  If fileName is a relative file name, resolve it relative to rootDir.</p>
     *
     *@param  fileName  Description of the Parameter
     *@param  rootDir   Description of the Parameter
     *@return           Description of the Return Value
     *@deprecated
     */
    public File resolveFile(String fileName, File rootDir) {
        return fileUtils.resolveFile(rootDir, fileName);
    }


    /**
     *  Description of the Method
     *
     *@param  fileName  Description of the Parameter
     *@return           Description of the Return Value
     */
    public File resolveFile(String fileName) {
        return fileUtils.resolveFile(baseDir, fileName);
    }


    /**
     *  Translate a path into its native (platform specific) format. <p>
     *
     *  This method uses the PathTokenizer class to separate the input path into
     *  its components. This handles DOS style paths in a relatively sensible
     *  way. The file separators are then converted to their platform specific
     *  versions.
     *
     *@param  to_process  the path to be converted
     *@return             the native version of to_process or an empty string if
     *      to_process is null or empty
     */
    public static String translatePath(String to_process) {
        if (to_process == null || to_process.length() == 0) {
            return "";
        }

        StringBuffer path = new StringBuffer(to_process.length() + 50);
        PathTokenizer tokenizer = new PathTokenizer(to_process);
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
     *  Convienence method to copy a file from a source to a destination. No
     *  filtering is performed.
     *
     *@param  sourceFile    Description of the Parameter
     *@param  destFile      Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(String sourceFile, String destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }


    /**
     *  Convienence method to copy a file from a source to a destination
     *  specifying if token filtering must be used.
     *
     *@param  sourceFile    Description of the Parameter
     *@param  destFile      Description of the Parameter
     *@param  filtering     Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering)
             throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null);
    }


    /**
     *  Convienence method to copy a file from a source to a destination
     *  specifying if token filtering must be used and if source files may
     *  overwrite newer destination files.
     *
     *@param  sourceFile    Description of the Parameter
     *@param  destFile      Description of the Parameter
     *@param  filtering     Description of the Parameter
     *@param  overwrite     Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
            boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null, overwrite);
    }


    /**
     *  Convienence method to copy a file from a source to a destination
     *  specifying if token filtering must be used, if source files may
     *  overwrite newer destination files and the last modified time of <code>destFile</code>
     *  file should be made equal to the last modified time of <code>sourceFile</code>
     *  .
     *
     *@param  sourceFile            Description of the Parameter
     *@param  destFile              Description of the Parameter
     *@param  filtering             Description of the Parameter
     *@param  overwrite             Description of the Parameter
     *@param  preserveLastModified  Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
            boolean overwrite, boolean preserveLastModified)
             throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null,
                overwrite, preserveLastModified);
    }


    /**
     *  Convienence method to copy a file from a source to a destination. No
     *  filtering is performed.
     *
     *@param  sourceFile    Description of the Parameter
     *@param  destFile      Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }


    /**
     *  Convienence method to copy a file from a source to a destination
     *  specifying if token filtering must be used.
     *
     *@param  sourceFile    Description of the Parameter
     *@param  destFile      Description of the Parameter
     *@param  filtering     Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering)
             throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null);
    }


    /**
     *  Convienence method to copy a file from a source to a destination
     *  specifying if token filtering must be used and if source files may
     *  overwrite newer destination files.
     *
     *@param  sourceFile    Description of the Parameter
     *@param  destFile      Description of the Parameter
     *@param  filtering     Description of the Parameter
     *@param  overwrite     Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
            boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null, overwrite);
    }


    /**
     *  Convienence method to copy a file from a source to a destination
     *  specifying if token filtering must be used, if source files may
     *  overwrite newer destination files and the last modified time of <code>destFile</code>
     *  file should be made equal to the last modified time of <code>sourceFile</code>
     *  .
     *
     *@param  sourceFile            Description of the Parameter
     *@param  destFile              Description of the Parameter
     *@param  filtering             Description of the Parameter
     *@param  overwrite             Description of the Parameter
     *@param  preserveLastModified  Description of the Parameter
     *@throws  IOException
     *@deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
            boolean overwrite, boolean preserveLastModified)
             throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null,
                overwrite, preserveLastModified);
    }


    /**
     *  Calls File.setLastModified(long time) in a Java 1.1 compatible way.
     *
     *@param  file                The new fileLastModified value
     *@param  time                The new fileLastModified value
     *@exception  BuildException  Description of the Exception
     *@deprecated
     */
    public void setFileLastModified(File file, long time) throws BuildException {
        if (getJavaVersion() == JAVA_1_1) {
            log("Cannot change the modification time of " + file
                     + " in JDK 1.1", Project.MSG_WARN);
            return;
        }
        fileUtils.setFileLastModified(file, time);
        log("Setting modification time for " + file, MSG_VERBOSE);
    }


    /**
     *  returns the boolean equivalent of a string, which is considered true if
     *  either "on", "true", or "yes" is found, ignoring case.
     *
     *@param  s  Description of the Parameter
     *@return    Description of the Return Value
     */
    public static boolean toBoolean(String s) {
        return (s.equalsIgnoreCase("on") ||
                s.equalsIgnoreCase("true") ||
                s.equalsIgnoreCase("yes"));
    }


    /**
     *  Topologically sort a set of Targets.
     *
     *@param  root                is the (String) name of the root Target. The
     *      sort is created in such a way that the sequence of Targets uptil the
     *      root target is the minimum possible such sequence.
     *@param  targets             is a Hashtable representing a "name to Target"
     *      mapping
     *@return                     a Vector of Strings with the names of the
     *      targets in sorted order.
     *@exception  BuildException  if there is a cyclic dependency among the
     *      Targets, or if a Target does not exist.
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
        for (Enumeration en = targets.keys(); en.hasMoreElements(); ) {
            String curTarget = (String) (en.nextElement());
            String st = (String) state.get(curTarget);
            if (st == null) {
                tsort(curTarget, targets, state, visiting, ret);
            } else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: " + curTarget);
            }
        }
        log("Complete build sequence is " + ret, MSG_VERBOSE);
        return ret;
    }


    // one step in a recursive DFS traversal of the Target dependency tree.
    // - The Hashtable "state" contains the state (VISITED or VISITING or null)
    // of all the target names.
    // - The Stack "visiting" contains a stack of target names that are
    // currently on the DFS stack. (NB: the target names in "visiting" are
    // exactly the target names in "state" that are in the VISITING state.)
    // 1. Set the current target to the VISITING state, and push it onto
    // the "visiting" stack.
    // 2. Throw a BuildException if any child of the current node is
    // in the VISITING state (implies there is a cycle.) It uses the
    // "visiting" Stack to construct the cycle.
    // 3. If any children have not been VISITED, tsort() the child.
    // 4. Add the current target to the Vector "ret" after the children
    //   have been visited. Move the current target to the VISITED state.
    //   "ret" now contains the sorted sequence of Targets upto the current
    //   Target.

    /**
     *  Description of the Method
     *
     *@param  root                Description of the Parameter
     *@param  targets             Description of the Parameter
     *@param  state               Description of the Parameter
     *@param  visiting            Description of the Parameter
     *@param  ret                 Description of the Parameter
     *@exception  BuildException  Description of the Exception
     */
    private final void tsort(String root, Hashtable targets,
            Hashtable state, Stack visiting,
            Vector ret)
             throws BuildException {
        state.put(root, VISITING);
        visiting.push(root);

        Target target = (Target) (targets.get(root));

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

        for (Enumeration en = target.getDependencies(); en.hasMoreElements(); ) {
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
            throw new RuntimeException("Unexpected internal error: expected to pop " + root + " but got " + p);
        }
        state.put(root, VISITED);
        ret.addElement(target);
    }


    /**
     *  Description of the Method
     *
     *@param  end  Description of the Parameter
     *@param  stk  Description of the Parameter
     *@return      Description of the Return Value
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

            String valueAsString = "";
            try {
                valueAsString = value.toString();
            } catch (Throwable t) {
                log("Caught exception (" + t.getClass().getName() + ")"
                    + " while expanding " + name + ": " + t.getMessage(),
                    MSG_WARN);
            }
            log("Adding reference: " + name + " -> " + valueAsString,
                MSG_DEBUG);
            references.put(name, value);
        }
    }


    /**
     *  Gets the references attribute of the Project object
     *
     *@return    The references value
     */
    public Hashtable getReferences() {
        return references;
    }


    /**
     *@param  key  Description of the Parameter
     *@return      The object with the "id" key.
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
     *  send build started event to the listeners
     */
    protected void fireBuildStarted() {
        BuildEvent event = new BuildEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.buildStarted(event);
        }
    }


    /**
     *  send build finished event to the listeners
     *
     *@param  exception  exception which indicates failure if not null
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
     *  send target started event to the listeners
     *
     *@param  target  Description of the Parameter
     */
    protected void fireTargetStarted(Target target) {
        BuildEvent event = new BuildEvent(target);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.targetStarted(event);
        }
    }


    /**
     *  send build finished event to the listeners
     *
     *@param  exception  exception which indicates failure if not null
     *@param  target     Description of the Parameter
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
     *  Description of the Method
     *
     *@param  task  Description of the Parameter
     */
    protected void fireTaskStarted(Task task) {
        // register this as the current task on the current thread.
        threadTasks.put(Thread.currentThread(), task);
        BuildEvent event = new BuildEvent(task);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.taskStarted(event);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  task       Description of the Parameter
     *@param  exception  Description of the Parameter
     */
    protected void fireTaskFinished(Task task, Throwable exception) {
        threadTasks.remove(Thread.currentThread());
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
     *  Description of the Method
     *
     *@param  event     Description of the Parameter
     *@param  message   Description of the Parameter
     *@param  priority  Description of the Parameter
     */
    private void fireMessageLoggedEvent(BuildEvent event, String message, int priority) {
        event.setMessage(message, priority);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.messageLogged(event);
        }
    }


    /**
     *  Description of the Method
     *
     *@param  project   Description of the Parameter
     *@param  message   Description of the Parameter
     *@param  priority  Description of the Parameter
     */
    protected void fireMessageLogged(Project project, String message, int priority) {
        BuildEvent event = new BuildEvent(project);
        fireMessageLoggedEvent(event, message, priority);
    }


    /**
     *  Description of the Method
     *
     *@param  target    Description of the Parameter
     *@param  message   Description of the Parameter
     *@param  priority  Description of the Parameter
     */
    protected void fireMessageLogged(Target target, String message, int priority) {
        BuildEvent event = new BuildEvent(target);
        fireMessageLoggedEvent(event, message, priority);
    }


    /**
     *  Description of the Method
     *
     *@param  task      Description of the Parameter
     *@param  message   Description of the Parameter
     *@param  priority  Description of the Parameter
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
     * Get the current task assopciated with a thread, if any
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
        Project project;
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
            Object o = super.get(key);
            if (o instanceof UnknownElement) {
                // Make sure that
                ((UnknownElement) o).maybeConfigure();
                o = ((UnknownElement) o).getTask();
            }
            return o;
        }
    }

    private static class AntTaskTable extends LazyHashtable {
        Project project;
        Properties props;
        boolean tasks = false;

        public AntTaskTable(Project p, boolean tasks) {
            this.project = p;
            this.tasks = tasks;
        }

        public void addDefinitions(Properties props) {
            this.props = props;
        }

        protected void initAll() {
            if (initAllDone ) return;
            project.log("InitAll", Project.MSG_DEBUG);
            if (props==null ) return;
            Enumeration enum = props.propertyNames();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                Class taskClass=getTask( key );
                if (taskClass!=null ) {
                    // This will call a get() and a put()
                    if (tasks )
                        project.addTaskDefinition(key, taskClass);
                    else
                        project.addDataTypeDefinition(key, taskClass );
                }
            }
            initAllDone=true;
        }

        protected Class getTask(String key) {
            if (props==null ) return null; // for tasks loaded before init()
            String value=props.getProperty(key);
            if (value==null) {
                //project.log( "No class name for " + key, Project.MSG_VERBOSE );
                return null;
            }
            try {
                Class taskClass=null;
                if (project.getCoreLoader() != null &&
                    !("only".equals(project.getProperty("build.sysclasspath")))) {
                    try {
                        taskClass=project.getCoreLoader().loadClass(value);
                        if (taskClass != null ) return taskClass;
                    } catch( Exception ex ) {
                    }
                }
                taskClass = Class.forName(value);
                return taskClass;
            } catch (NoClassDefFoundError ncdfe) {
                project.log("Could not load a dependent class ("
                        + ncdfe.getMessage() + ") for task " + key, Project.MSG_DEBUG);
            } catch (ClassNotFoundException cnfe) {
                project.log("Could not load class (" + value
                        + ") for task " + key, Project.MSG_DEBUG);
            }
            return null;
        }

        // Hashtable implementation
        public Object get( Object key ) {
            Object orig=super.get( key );
            if (orig!= null ) return orig;
            if (! (key instanceof String) ) return null;
            project.log("Get task " + key, Project.MSG_DEBUG );
            Object taskClass=getTask( (String) key);
            if (taskClass != null)
                super.put( key, taskClass );
            return taskClass;
        }

        public boolean containsKey(Object key) {
            return get(key) != null;
        }

    }

}
