/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Iterator;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.service.DataService;
import org.apache.ant.common.service.FileService;
import org.apache.ant.common.util.ExecutionException;
import org.apache.ant.common.util.PropertyUtils;
import org.apache.ant.common.util.MessageLevel;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.util.FileUtils;

/**
 * Project facade
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 30 January 2002
 */
public class Project {

    /** String which indicates Java version 1.0 */
    public final static String JAVA_1_0 = "1.0";
    /** String which indicates Java version 1.1 */
    public final static String JAVA_1_1 = "1.1";
    /** String which indicates Java version 1.2 */
    public final static String JAVA_1_2 = "1.2";
    /** String which indicates Java version 1.3 */
    public final static String JAVA_1_3 = "1.3";
    /** String which indicates Java version 1.4 */
    public final static String JAVA_1_4 = "1.4";

    /**
     * @see MessageLevel.MSG_ERR
     */
    public final static int MSG_ERR = MessageLevel.MSG_ERR;
    /**
     * @see MessageLevel.MSG_WARN
     */
    public final static int MSG_WARN = MessageLevel.MSG_WARN;
    /**
     * @see MessageLevel.MSG_INFO
     */
    public final static int MSG_INFO = MessageLevel.MSG_INFO;
    /**
     * @see MessageLevel.MSG_VERBOSE
     */
    public final static int MSG_VERBOSE = MessageLevel.MSG_VERBOSE;
    /**
     * @see MessageLevel.MSG_DEBUG
     */
    public final static int MSG_DEBUG = MessageLevel.MSG_DEBUG;

    /** The java version detected that Ant is running on */
    private static String javaVersion;

    /** The project description */
    private String description;

    /** The global filters of this project */
    private FilterSet globalFilterSet = new FilterSet();

    /** The AntContext that is used to access core services */
    private AntContext context;

    /** The core's FileService instance */
    private FileService fileService;

    /** The core's DataService instance */
    private DataService dataService;

    /** Ant1 FileUtils instance fro manipulating files */
    private FileUtils fileUtils;
    /** The collection of global filters */
    private FilterSetCollection globalFilters
         = new FilterSetCollection(globalFilterSet);

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

    /** Create the project */
    public Project() {
        fileUtils = FileUtils.newFileUtils();
    }

    /**
     * static query of the java version
     *
     * @return a string indicating the Java version
     */
    public static String getJavaVersion() {
        return javaVersion;
    }

    /**
     * returns the boolean equivalent of a string, which is considered true
     * if either "on", "true", or "yes" is found, ignoring case.
     *
     * @param s the string value to be interpreted at a boolean
     * @return the value of s as a boolean
     */
    public static boolean toBoolean(String s) {
        return PropertyUtils.toBoolean(s);
    }

    /**
     * Translate a path into its native (platform specific) format. <p>
     *
     * This method uses the PathTokenizer class to separate the input path
     * into its components. This handles DOS style paths in a relatively
     * sensible way. The file separators are then converted to their
     * platform specific versions.
     *
     * @param to_process the path to be converted
     * @return the native version of to_process or an empty string if
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
     * set the project description
     *
     * @param description text
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set a project property
     *
     * @param name the property name
     * @param value the property value
     */
    public void setProperty(String name, String value) {
        try {
            dataService.setMutableDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Set a property which must be a new value
     *
     * @param name the property name
     * @param value the property value
     */
    public void setNewProperty(String name, String value) {
        try {
            dataService.setDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Sets a userProperty of the Project. Note under Ant2, there is no
     * distinction between user and system properties
     *
     * @param name the property name
     * @param value the property value
     */
    public void setUserProperty(String name, String value) {
        try {
            dataService.setMutableDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Gets the AntContext of the Project
     *
     * @return the AntContext
     */
    public AntContext getContext() {
        return context;
    }

    /**
     * get the project description
     *
     * @return description or null if no description has been set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get a project property
     *
     * @param name the property name
     * @return the value of the property
     */
    public String getProperty(String name) {
        try {
            Object value = dataService.getDataValue(name);
            return value == null ? null : value.toString();
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get a project property. Ant2 does not distinguish between User and
     * system proerties
     *
     * @param name the property name
     * @return the value of the property
     */
    public String getUserProperty(String name) {
        try {
            return dataService.getDataValue(name).toString();
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Get a reference to a project property. Note that in Ant2, properties
     * and references occupy the same namespace.
     *
     * @param refId the reference Id
     * @return the object specified by the reference id
     */
    public Object getReference(String refId) {
        try {
            return dataService.getDataValue(refId);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Gets the globalFilterSet of the Project
     *
     * @return the globalFilterSet
     */
    public FilterSet getGlobalFilterSet() {
        return globalFilterSet;
    }

    /**
     * Gets the baseDir of the Project
     *
     * @return the baseDir
     */
    public File getBaseDir() {
        return context.getBaseDir();
    }

    /**
     * Gets the coreLoader of the Project
     *
     * @return the coreLoader value
     */
    public ClassLoader getCoreLoader() {
        return getClass().getClassLoader();
    }

    /**
     * Add a reference to an object. NOte that in Ant2 objects and
     * properties occupy the same namespace.
     *
     * @param name the reference name
     * @param value the object to be associated with the given name.
     */
    public void addReference(String name, Object value) {
        try {
            dataService.setDataValue(name, value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }


    /**
     * Convienence method to copy a file from a source to a destination. No
     * filtering is performed.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used and if source files may
     * overwrite newer destination files.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used, if source files may
     * overwrite newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal to the last modified
     * time of <code>sourceFile</code>.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @param preserveLastModified true if the last modified time of the
     *      source file is preserved
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(String sourceFile, String destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null,
            overwrite, preserveLastModified);
    }

    /**
     * Convienence method to copy a file from a source to a destination. No
     * filtering is performed.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile) throws IOException {
        fileUtils.copyFile(sourceFile, destFile);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used and if source files may
     * overwrite newer destination files.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite) throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite);
    }

    /**
     * Convienence method to copy a file from a source to a destination
     * specifying if token filtering must be used, if source files may
     * overwrite newer destination files and the last modified time of
     * <code>destFile</code> file should be made equal to the last modified
     * time of <code>sourceFile</code>.
     *
     * @param sourceFile the source file to be copied
     * @param destFile the destination to which the file is copied
     * @param filtering true if the copy should apply filters
     * @param overwrite true if the destination can be overwritten
     * @param preserveLastModified true if the last modified time of the
     *      source file is preserved
     * @exception IOException if the file cannot be copied
     * @deprecated
     */
    public void copyFile(File sourceFile, File destFile, boolean filtering,
                         boolean overwrite, boolean preserveLastModified)
         throws IOException {
        fileUtils.copyFile(sourceFile, destFile,
            filtering ? globalFilters : null, overwrite, preserveLastModified);
    }

    /**
     * Initialise this porject
     *
     * @param context the context the project uses to access core services
     * @exception ExecutionException if the project cannot be initialised.
     */
    public void init(AntContext context) throws ExecutionException {
        this.context = context;
        fileService = (FileService)context.getCoreService(FileService.class);
        dataService = (DataService)context.getCoreService(DataService.class);
    }

    /**
     * Output a message to the log with the default log level of MSG_INFO
     *
     * @param msg text to log
     */

    public void log(String msg) {
        log(msg, MSG_INFO);
    }

    /**
     * Output a message to the log with the given log level and an event
     * scope of project
     *
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log(String msg, int msgLevel) {
        context.log(msg, msgLevel);
    }

    /**
     * Output a message to the log with the given log level and an event
     * scope of a task
     *
     * @param task task to use in the log
     * @param msg text to log
     * @param msgLevel level to log at
     */
    public void log(Task task, String msg, int msgLevel) {
        context.log(msg, msgLevel);
    }

    /**
     * Resolve a file relative to the project's basedir
     *
     * @param fileName the file name
     * @return the file as a File resolved relative to the project's basedir
     */
    public File resolveFile(String fileName) {
        try {
            return fileService.resolveFile(fileName);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Replace property references (${} values) in the given string
     *
     * @param value the string in which property references are replaced
     * @return the string with the properties replaced.
     */
    public String replaceProperties(String value) {
        try {
            return dataService.replacePropertyRefs(value);
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }

    /**
     * add a build listener to this project
     *
     * @param listener the listener to be added to the project
     */
    public void addBuildListener(BuildListener listener) {
        // XXX do nothing for now
    }

    /**
     * Create a Task. This faced hard codes a few well known tasks at this
     * time
     *
     * @param taskName the name of the task to be created.
     * @return the created task instance
     */
    public Task createTask(String taskName) {
        // we piggy back the task onto the current context
        Task task = null;
        if (taskName.equals("java")) {
            task = new Java();
        } else if (taskName.equals("exec")) {
            task = new ExecTask();
        } else {
            return null;
        }
        try {
            task.setProject(this);
            task.init(context);
            return task;
        } catch (ExecutionException e) {
            throw new BuildException(e);
        }
    }
    
    /**
     * get a copy of the property hashtable
     * @return the hashtable containing all properties, user included
     */
    public Hashtable getProperties() {
        Map properties = dataService.getAllProperties();
        Hashtable result = new Hashtable();
        for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            Object value = properties.get(name);
            if (value instanceof String) {
                result.put(name, value);
            }
        }
        
        return result;
    }

    /**
     * get a copy of the property hashtable
     * @return the hashtable containing all properties, user included
     */
    public Hashtable getUserProperties() {
        return getProperties();
    }
    
    /**
     * Get all references in the project
     * @return the hashtable containing all references
     */
    public Hashtable getReferences() {
        Map properties = dataService.getAllProperties();
        Hashtable result = new Hashtable();
        for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            Object value = properties.get(name);
            if (!(value instanceof String)) {
                result.put(name, value);
            }
        }
        
        return result;
    }
}

