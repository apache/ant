/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

import java.io.*;
import java.util.*;
import java.text.*;

import org.apache.tools.ant.types.FilterSet; 
import org.apache.tools.ant.types.FilterSetCollection; 
import org.apache.tools.ant.util.FileUtils; 

/**
 * Central representation of an Ant project. This class defines a
 * Ant project with all of it's targets and tasks. It also provides
 * the mechanism to kick off a build using a particular target name.
 * <p>
 * This class also encapsulates methods which allow Files to be refered
 * to using abstract path names which are translated to native system
 * file paths at runtime as well as defining various project properties.
 *
 * @author duncan@x180.com
 */

public class Project {

    public static final int MSG_ERR = 0;
    public static final int MSG_WARN = 1;
    public static final int MSG_INFO = 2;
    public static final int MSG_VERBOSE = 3;
    public static final int MSG_DEBUG = 4;

    // private set of constants to represent the state
    // of a DFS of the Target dependencies
    private static final String VISITING = "VISITING";
    private static final String VISITED = "VISITED";

    private static String javaVersion;

    public static final String JAVA_1_0 = "1.0";
    public static final String JAVA_1_1 = "1.1";
    public static final String JAVA_1_2 = "1.2";
    public static final String JAVA_1_3 = "1.3";
    public static final String JAVA_1_4 = "1.4";

    public static final String TOKEN_START = FilterSet.DEFAULT_TOKEN_START;
    public static final String TOKEN_END = FilterSet.DEFAULT_TOKEN_END;

    private String name;
    private String description;

    private Hashtable properties = new Hashtable();
    private Hashtable userProperties = new Hashtable();
    private Hashtable references = new Hashtable();
    private String defaultTarget;
    private Hashtable dataClassDefinitions = new Hashtable();
    private Hashtable taskClassDefinitions = new Hashtable();
    private Hashtable targets = new Hashtable();
    private FilterSet globalFilterSet = new FilterSet();
    private FilterSetCollection globalFilters = new FilterSetCollection(globalFilterSet);
    private File baseDir;

    private Vector listeners = new Vector();

    /** The Ant core classloader - may be null if using system loader */    
    private ClassLoader coreLoader = null;

    /** Records the latest task on a thread */ 
    private Hashtable threadTasks = new Hashtable();
    
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

    private FileUtils fileUtils;

    public Project() {
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * Initialise the project.
     *
     * This involves setting the default task definitions and loading the
     * system properties.
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
                    // ignore...
                } catch (ClassNotFoundException cnfe) {
                    // ignore...
                }
            }
        } catch (IOException ioe) {
            throw new BuildException("Can't load default task list");
        }

        String dataDefs = "/org/apache/tools/ant/types/defaults.properties";

        try{
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

    public void setCoreLoader(ClassLoader coreLoader) {
        this.coreLoader = coreLoader;
    }
    
    public ClassLoader getCoreLoader() {
        return coreLoader;
    }
    
    public void addBuildListener(BuildListener listener) {
        listeners.addElement(listener);
    }

    public void removeBuildListener(BuildListener listener) {
        listeners.removeElement(listener);
    }

    public Vector getBuildListeners() {
        return listeners;
    }

    public void log(String msg) {
        log(msg, MSG_INFO);
    }

    public void log(String msg, int msgLevel) {
        fireMessageLogged(this, msg, msgLevel);
    }

    public void log(Task task, String msg, int msgLevel) {
        fireMessageLogged(task, msg, msgLevel);
    }

    public void log(Target target, String msg, int msgLevel) {
        fireMessageLogged(target, msg, msgLevel);
    }

  
    public FilterSet getGlobalFilterSet() {
        return globalFilterSet;
    }
    
    
    public void setProperty(String name, String value) {
        // command line properties take precedence
        if (null != userProperties.get(name)) {
            log("Override ignored for user property " + name, MSG_VERBOSE);
            return;
        }
        log("Setting project property: " + name + " -> " +
            value, MSG_DEBUG);
        properties.put(name, value);
    }

    public void setUserProperty(String name, String value) {
        log("Setting ro project property: " + name + " -> " +
            value, MSG_DEBUG);
        userProperties.put(name, value);
        properties.put(name, value);
    }

    public String getProperty(String name) {
        if (name == null) return null;
        String property = (String) properties.get(name);
        return property;
    }

    public String getUserProperty(String name) {
        if (name == null) return null;
        String property = (String) userProperties.get(name);
        return property;
    }

    public Hashtable getProperties() {
        return properties;
    }

    public Hashtable getUserProperties() {
        return userProperties;
    }

    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    // deprecated, use setDefault
    public String getDefaultTarget() {
        return defaultTarget;
    }

    // match the attribute name
    public void setDefault(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }

    public void setName(String name) {
        setUserProperty("ant.project.name",  name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

   // Will return null if no description has been set
    public String getDescription() {
        return description;
    }

    /** @deprecated */
    public void addFilter(String token, String value) {
        if (token == null) {
            return;
        }
        
        globalFilterSet.addFilter(new FilterSet.Filter(token, value));
    }

    /** @deprecated */
    public Hashtable getFilters() {
        // we need to build the hashtable dynamically
        return globalFilterSet.getFilterHash();
    }

    // match basedir attribute in xml
    public void setBasedir(String baseD) throws BuildException {
        setBaseDir(new File(baseD));
    }

    public void setBaseDir(File baseDir) throws BuildException {
        baseDir = fileUtils.normalize(baseDir.getAbsolutePath());
        if (!baseDir.exists()) 
            throw new BuildException("Basedir " + baseDir.getAbsolutePath() + " does not exist");
        if (!baseDir.isDirectory()) 
            throw new BuildException("Basedir " + baseDir.getAbsolutePath() + " is not a directory");
        this.baseDir = baseDir;
        setProperty( "basedir", this.baseDir.getPath());
        String msg = "Project base dir set to: " + this.baseDir;
        log(msg, MSG_VERBOSE);
    }

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

    public static String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersionProperty() {
        setProperty("ant.java.version", javaVersion);

        // sanity check
        if (javaVersion == JAVA_1_0) {
            throw new BuildException("Ant cannot work on Java 1.0");
        }

        log("Detected Java version: " + javaVersion + " in: " + System.getProperty("java.home"), MSG_VERBOSE);

        log("Detected OS: " + System.getProperty("os.name"), MSG_VERBOSE);
    }

    public void setSystemProperties() {
        Properties systemP = System.getProperties();
        Enumeration e = systemP.keys();
        while (e.hasMoreElements()) {
            Object name = e.nextElement();
            String value = systemP.get(name).toString();
            this.setProperty(name.toString(), value);
        }
    }

    public void addTaskDefinition(String taskName, Class taskClass) {
        String msg = " +User task: " + taskName + "     " + taskClass.getName();
        log(msg, MSG_DEBUG);
        taskClassDefinitions.put(taskName, taskClass);
    }

    public Hashtable getTaskDefinitions() {
        return taskClassDefinitions;
    }

    public void addDataTypeDefinition(String typeName, Class typeClass) {
        String msg = " +User datatype: " + typeName + "     " + typeClass.getName();
        log(msg, MSG_DEBUG);
        dataClassDefinitions.put(typeName, typeClass);
    }

    public Hashtable getDataTypeDefinitions() {
        return dataClassDefinitions;
    }

    /**
     * This call expects to add a <em>new</em> Target.
     * @param target is the Target to be added to the current
     * Project.
     * @exception BuildException if the Target already exists
     * in the project.
     * @see Project#addOrReplaceTarget to replace existing Targets.
     */
    public void addTarget(Target target) {
        String name = target.getName();
        if (targets.get(name) != null) {
            throw new BuildException("Duplicate target: `"+name+"'");
        }
        addOrReplaceTarget(name, target);
    }

    /**
     * This call expects to add a <em>new</em> Target.
     * @param target is the Target to be added to the current
     * Project.
     * @param targetName is the name to use for the Target
     * @exception BuildException if the Target already exists
     * in the project.
     * @see Project#addOrReplaceTarget to replace existing Targets.
     */
     public void addTarget(String targetName, Target target)
         throws BuildException {
         if (targets.get(targetName) != null) {
             throw new BuildException("Duplicate target: `"+targetName+"'");
         }
         addOrReplaceTarget(targetName, target);
     }

    /**
     * @param target is the Target to be added or replaced in
     * the current Project.
     */
    public void addOrReplaceTarget(Target target) {
        addOrReplaceTarget(target.getName(), target);
    }

    /**
     * @param target is the Target to be added/replaced in
     * the current Project.
     * @param targetName is the name to use for the Target
     */
    public void addOrReplaceTarget(String targetName, Target target) {
        String msg = " +Target: " + targetName;
        log(msg, MSG_DEBUG);
        target.setProject(this);
        targets.put(targetName, target);
    }

    public Hashtable getTargets() {
        return targets;
    }

    public Task createTask(String taskType) throws BuildException {
        Class c = (Class) taskClassDefinitions.get(taskType);

        if (c == null)
            return null;
        try {
            Object o = c.newInstance();
            Task task = null;
            if( o instanceof Task ) {
               task=(Task)o;
            } else {
                // "Generic" Bean - use the setter pattern
                // and an Adapter
                TaskAdapter taskA=new TaskAdapter();
                taskA.setProxy( o );
                task=taskA;
            }
            task.setProject(this);
            task.setTaskType(taskType);

            // set default value, can be changed by the user
            task.setTaskName(taskType);

            String msg = "   +Task: " + taskType;
            log (msg, MSG_DEBUG);
            return task;
        } catch (Throwable t) {
            String msg = "Could not create task of type: "
                 + taskType + " due to " + t;
            throw new BuildException(msg, t);
        }
    }

    public Object createDataType(String typeName) throws BuildException {
        Class c = (Class) dataClassDefinitions.get(typeName);

        if (c == null)
            return null;

        try {
            java.lang.reflect.Constructor ctor = null;
            boolean noArg = false;
            // DataType can have a "no arg" constructor or take a single 
            // Project argument.
            try {
                ctor = c.getConstructor(new Class[0]);
                noArg = true;
            } catch (NoSuchMethodException nse) {
                ctor = c.getConstructor(new Class[] {getClass()});
                noArg = false;
            }

            Object o = null;
            if (noArg) {
                 o = ctor.newInstance(new Object[0]);
            } else {
                 o = ctor.newInstance(new Object[] {this});
            }
            if (o instanceof ProjectComponent) {
                ((ProjectComponent)o).setProject(this);
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

    public void executeTargets(Vector targetNames) throws BuildException {
        Throwable error = null;

        for (int i = 0; i < targetNames.size(); i++) {
            executeTarget((String)targetNames.elementAt(i));
        }
    }

    public void demuxOutput(String line, boolean isError) {
        Task task = (Task)threadTasks.get(Thread.currentThread());
        if (task == null) {
            fireMessageLogged(this, line, isError ? MSG_ERR : MSG_INFO);
        }
        else {
            if (isError) {
                task.handleErrorOutput(line);
            }
            else {
                task.handleOutput(line);
            }
        }
    }
    
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
     * Return the canonical form of fileName as an absolute path.
     *
     * <p>If fileName is a relative file name, resolve it relative to
     * rootDir.</p>
     *
     * @deprecated
     */
    public File resolveFile(String fileName, File rootDir) {
        return fileUtils.resolveFile(rootDir, fileName);
    }

    public File resolveFile(String fileName) {
        return fileUtils.resolveFile(baseDir, fileName);
    }

    /**
     * Translate a path into its native (platform specific) format. 
     * <p>
     * This method uses the PathTokenizer class to separate the input path
     * into its components. This handles DOS style paths in a relatively
     * sensible way. The file separators are then converted to their platform
     * specific versions.
     *
     * @param to_process the path to be converted   
     *
     * @return the native version of to_process or 
     *         an empty string if to_process is null or empty
     */
    static public String translatePath(String to_process) {
        if ( to_process == null || to_process.length() == 0 ) {
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
     * Convienence method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @throws IOException
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @throws IOException
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @throws IOException 
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null, overwrite);
    }

     /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @throws IOException 
     *
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null, 
                           overwrite, preserveLastModified);
    }

    /**
     * Convienence method to copy a file from a source to a destination.
     * No filtering is performed.
     *
     * @throws IOException
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @throws IOException
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used and if
     * source files may overwrite newer destination files.
     *
     * @throws IOException 
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convienence method to copy a file from a source to a
     * destination specifying if token filtering must be used, if
     * source files may overwrite newer destination files and the
     * last modified time of <code>destFile</code> file should be made equal
     * to the last modified time of <code>sourceFile</code>.
     *
     * @throws IOException 
     *
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
        throws IOException {
        fileUtils.copyFile(sourceFile, destFile, filtering ? globalFilters : null, 
                           overwrite, preserveLastModified);
    }

    /**
     * Calls File.setLastModified(long time) in a Java 1.1 compatible way.
     *
     * @deprecated
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
     * returns the boolean equivalent of a string, which is considered true
     * if either "on", "true", or "yes" is found, ignoring case.
     */
    public static boolean toBoolean(String s) {
        return (s.equalsIgnoreCase("on") ||
                s.equalsIgnoreCase("true") ||
                s.equalsIgnoreCase("yes"));
    }

    /**
     * Topologically sort a set of Targets.
     * @param root is the (String) name of the root Target. The sort is
     * created in such a way that the sequence of Targets uptil the root
     * target is the minimum possible such sequence.
     * @param targets is a Hashtable representing a "name to Target" mapping
     * @return a Vector of Strings with the names of the targets in
     * sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     * Targets, or if a Target does not exist.
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
        log("Build sequence for target `"+root+"' is "+ret, MSG_VERBOSE);
        for (Enumeration en=targets.keys(); en.hasMoreElements();) {
            String curTarget = (String)(en.nextElement());
            String st = (String) state.get(curTarget);
            if (st == null) {
                tsort(curTarget, targets, state, visiting, ret);
            }
            else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: "+curTarget);
            }
        }
        log("Complete build sequence is "+ret, MSG_VERBOSE);
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

    private final void tsort(String root, Hashtable targets,
                             Hashtable state, Stack visiting,
                             Vector ret)
        throws BuildException {
        state.put(root, VISITING);
        visiting.push(root);

        Target target = (Target)(targets.get(root));

        // Make sure we exist
        if (target == null) {
            StringBuffer sb = new StringBuffer("Target `");
            sb.append(root);
            sb.append("' does not exist in this project. ");
            visiting.pop();
            if (!visiting.empty()) {
                String parent = (String)visiting.peek();
                sb.append("It is used from target `");
                sb.append(parent);
                sb.append("'.");
            }

            throw new BuildException(new String(sb));
        }

        for (Enumeration en=target.getDependencies(); en.hasMoreElements();) {
            String cur = (String) en.nextElement();
            String m=(String)state.get(cur);
            if (m == null) {
                // Not been visited
                tsort(cur, targets, state, visiting, ret);
            }
            else if (m == VISITING) {
                // Currently visiting this node, so have a cycle
                throw makeCircularException(cur, visiting);
            }
        }

        String p = (String) visiting.pop();
        if (root != p) {
            throw new RuntimeException("Unexpected internal error: expected to pop "+root+" but got "+p);
        }
        state.put(root, VISITED);
        ret.addElement(target);
    }

    private static BuildException makeCircularException(String end, Stack stk) {
        StringBuffer sb = new StringBuffer("Circular dependency: ");
        sb.append(end);
        String c;
        do {
            c = (String)stk.pop();
            sb.append(" <- ");
            sb.append(c);
        } while(!c.equals(end));
        return new BuildException(new String(sb));
    }

    public void addReference(String name, Object value) {
        if (null != references.get(name)) {
            log("Overriding previous definition of reference to " + name, 
                MSG_WARN);
        }
        log("Adding reference: " + name + " -> " + value, MSG_DEBUG);
        references.put(name,value);
    }

    public Hashtable getReferences() {
        return references;
    }

    protected void fireBuildStarted() {
        BuildEvent event = new BuildEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.buildStarted(event);
        }
    }

    protected void fireBuildFinished(Throwable exception) {
        BuildEvent event = new BuildEvent(this);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.buildFinished(event);
        }
    }

    protected void fireTargetStarted(Target target) {
        BuildEvent event = new BuildEvent(target);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.targetStarted(event);
        }
    }

    protected void fireTargetFinished(Target target, Throwable exception) {
        BuildEvent event = new BuildEvent(target);
        event.setException(exception);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.targetFinished(event);
        }
    }

    protected void fireTaskStarted(Task task) {
        // register this as the current task on the current thread.
        threadTasks.put(Thread.currentThread(), task);
        BuildEvent event = new BuildEvent(task);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.taskStarted(event);
        }
    }

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

    private void fireMessageLoggedEvent(BuildEvent event, String message, int priority) {
        event.setMessage(message, priority);
        for (int i = 0; i < listeners.size(); i++) {
            BuildListener listener = (BuildListener) listeners.elementAt(i);
            listener.messageLogged(event);
        }
    }

    protected void fireMessageLogged(Project project, String message, int priority) {
        BuildEvent event = new BuildEvent(project);
        fireMessageLoggedEvent(event, message, priority);
    }

    protected void fireMessageLogged(Target target, String message, int priority) {
        BuildEvent event = new BuildEvent(target);
        fireMessageLoggedEvent(event, message, priority);
    }

    protected void fireMessageLogged(Task task, String message, int priority) {
        BuildEvent event = new BuildEvent(task);
        fireMessageLoggedEvent(event, message, priority);
    }
}
