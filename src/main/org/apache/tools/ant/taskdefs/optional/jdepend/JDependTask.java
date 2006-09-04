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

package org.apache.tools.ant.taskdefs.optional.jdepend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LoaderUtils;

/**
 * Runs JDepend tests.
 *
 * <p>JDepend is a tool to generate design quality metrics for each Java package.
 * It has been initially created by Mike Clark. JDepend can be found at <a
 * href="http://www.clarkware.com/software/JDepend.html">http://www.clarkware.com/software/JDepend.html</a>.
 *
 * The current implementation spawn a new Java VM.
 *
 */
public class JDependTask extends Task {
    //private CommandlineJava commandline = new CommandlineJava();

    // required attributes
    private Path sourcesPath; // Deprecated!
    private Path classesPath; // Use this going forward

    // optional attributes
    private File outputFile;
    private File dir;
    private Path compileClasspath;
    private boolean haltonerror = false;
    private boolean fork = false;
    private Long timeout = null;

    private String jvm = null;
    private String format = "text";
    private PatternSet defaultPatterns = new PatternSet();

    private static Constructor packageFilterC;
    private static Method setFilter;

    private boolean includeRuntime = false;
    private Path runtimeClasses = null;

    static {
        try {
            Class packageFilter =
                Class.forName("jdepend.framework.PackageFilter");
            packageFilterC =
                packageFilter.getConstructor(new Class[] {java.util.Collection.class});
            setFilter =
                jdepend.textui.JDepend.class.getDeclaredMethod("setFilter",
                                                               new Class[] {packageFilter});
        } catch (Throwable t) {
            if (setFilter == null) {
                packageFilterC = null;
            }
        }
    }

    /**
     * If true,
     *  include jdepend.jar in the forked VM.
     *
     * @param b include ant run time yes or no
     * @since Ant 1.6
     */
    public void setIncluderuntime(boolean b) {
        includeRuntime = b;
    }

    /**
     * Set the timeout value (in milliseconds).
     *
     * <p>If the operation is running for more than this value, the jdepend
     * will be canceled. (works only when in 'fork' mode).</p>
     * @param value the maximum time (in milliseconds) allowed before
     * declaring the test as 'timed-out'
     * @see #setFork(boolean)
     */
    public void setTimeout(Long value) {
        timeout = value;
    }

    /**
     * @return the timeout value
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * The output file name.
     *
     * @param outputFile the output file name
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @return the output file name
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Whether or not to halt on failure. Default: false.
     * @param haltonerror the value to set
     */
    public void setHaltonerror(boolean haltonerror) {
        this.haltonerror = haltonerror;
    }

    /**
     * @return the value of the haltonerror attribute
     */
    public boolean getHaltonerror() {
        return haltonerror;
    }

    /**
     * If true, forks into a new JVM. Default: false.
     *
     * @param   value   <tt>true</tt> if a JVM should be forked,
     *                  otherwise <tt>false<tt>
     */
    public void setFork(boolean value) {
        fork = value;
    }

    /**
     * @return the value of the fork attribute
     */
    public boolean getFork() {
        return fork;
    }

    /**
     * The command used to invoke a forked Java Virtual Machine.
     *
     * Default is <tt>java</tt>. Ignored if no JVM is forked.
     * @param   value   the new VM to use instead of <tt>java</tt>
     * @see #setFork(boolean)
     */
    public void setJvm(String value) {
        jvm = value;

    }

    /**
     * Adds a path to source code to analyze.
     * @return a source path
     * @deprecated since 1.6.x.
     */
    public Path createSourcespath() {
        if (sourcesPath == null) {
            sourcesPath = new Path(getProject());
        }
        return sourcesPath.createPath();
    }

    /**
     * Gets the sourcepath.
     * @return the sources path
     * @deprecated since 1.6.x.
     */
    public Path getSourcespath() {
        return sourcesPath;
    }

    /**
     * Adds a path to class code to analyze.
     * @return a classes path
     */
    public Path createClassespath() {
        if (classesPath == null) {
            classesPath = new Path(getProject());
        }
        return classesPath.createPath();
    }

    /**
     * Gets the classespath.
     * @return the classes path
     */
    public Path getClassespath() {
        return classesPath;
    }

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     * @param   dir     the directory to invoke the JVM from.
     * @see #setFork(boolean)
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * @return the dir attribute
     */
    public File getDir() {
        return dir;
    }

    /**
     * Set the classpath to be used for this compilation.
     * @param classpath a class path to be used
     */
    public void setClasspath(Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }

    /**
     * Gets the classpath to be used for this compilation.
     * @return the class path used for compilation
     */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Adds a path to the classpath.
     * @return a classpath
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        return compileClasspath.createPath();
    }

    /**
     * Create a new JVM argument. Ignored if no JVM is forked.
     * @param commandline the commandline to create the argument on
     * @return  create a new JVM argument so that any argument can
     *          be passed to the JVM.
     * @see #setFork(boolean)
     */
    public Commandline.Argument createJvmarg(CommandlineJava commandline) {
        return commandline.createVmArgument();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @param r a classpath reference
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * add a name entry on the exclude list
     * @return a pattern for the excludes
     */
    public PatternSet.NameEntry createExclude() {
        return defaultPatterns.createExclude();
    }

    /**
     * @return the excludes patterns
     */
    public PatternSet getExcludes() {
        return defaultPatterns;
    }

    /**
     * The format to write the output in, "xml" or "text".
     *
     * @param ea xml or text
     */
    public void setFormat(FormatAttribute ea) {
        format = ea.getValue();
    }

    /**
     * A class for the enumerated attribute format,
     * values are xml and text.
     * @see EnumeratedAttribute
     */
    public static class FormatAttribute extends EnumeratedAttribute {
        private String [] formats = new String[]{"xml", "text"};

        /**
         * @return the enumerated values
         */
        public String[] getValues() {
            return formats;
        }
    }

    /**
     * No problems with this test.
     */
    private static final int SUCCESS = 0;
    /**
     * An error occurred.
     */
    private static final int ERRORS = 1;

    /**
     * Search for the given resource and add the directory or archive
     * that contains it to the classpath.
     *
     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
     * getResource doesn't contain the name of the archive.</p>
     *
     * @param resource resource that one wants to lookup
     * @since Ant 1.6
     */
    private void addClasspathEntry(String resource) {
        /*
         * pre Ant 1.6 this method used to call getClass().getResource
         * while Ant 1.6 will call ClassLoader.getResource().
         *
         * The difference is that Class.getResource expects a leading
         * slash for "absolute" resources and will strip it before
         * delegating to ClassLoader.getResource - so we now have to
         * emulate Class's behavior.
         */
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        } else {
            resource = "org/apache/tools/ant/taskdefs/optional/jdepend/"
                + resource;
        }

        File f = LoaderUtils.getResourceSource(getClass().getClassLoader(),
                                               resource);
        if (f != null) {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            runtimeClasses.createPath().setLocation(f);
        } else {
            log("Couldn\'t find " + resource, Project.MSG_DEBUG);
        }
    }

    /**
     * execute the task
     *
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {

        CommandlineJava commandline = new CommandlineJava();

        if ("text".equals(format)) {
            commandline.setClassname("jdepend.textui.JDepend");
        } else
            if ("xml".equals(format)) {
                commandline.setClassname("jdepend.xmlui.JDepend");
            }

        if (jvm != null) {
            commandline.setVm(jvm);
        }
        if (getSourcespath() == null && getClassespath() == null) {
            throw new BuildException("Missing classespath required argument");
        } else if (getClassespath() == null) {
            String msg =
                "sourcespath is deprecated in JDepend >= 2.5 "
                + "- please convert to classespath";
            log(msg);
        }

        // execute the test and get the return code
        int exitValue = JDependTask.ERRORS;
        boolean wasKilled = false;
        if (!getFork()) {
            exitValue = executeInVM(commandline);
        } else {
            ExecuteWatchdog watchdog = createWatchdog();
            exitValue = executeAsForked(commandline, watchdog);
            // null watchdog means no timeout, you'd better not check with null
            if (watchdog != null) {
                wasKilled = watchdog.killedProcess();
            }
        }

        // if there is an error/failure and that it should halt, stop
        // everything otherwise just log a statement
        boolean errorOccurred = exitValue == JDependTask.ERRORS || wasKilled;

        if (errorOccurred) {
            String errorMessage = "JDepend FAILED"
                + (wasKilled ? " - Timed out" : "");

            if  (getHaltonerror()) {
                throw new BuildException(errorMessage, getLocation());
            } else {
                log(errorMessage, Project.MSG_ERR);
            }
        }
    }

    // this comment extract from JUnit Task may also apply here
    // "in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)"

    /**
     * Execute inside VM.
     *
     * @param commandline the command line
     * @return the return value of the mvm
     * @exception BuildException if an error occurs
     */
    public int executeInVM(CommandlineJava commandline) throws BuildException {
        jdepend.textui.JDepend jdepend;

        if ("xml".equals(format)) {
            jdepend = new jdepend.xmlui.JDepend();
        } else {
            jdepend = new jdepend.textui.JDepend();
        }

        FileWriter fw = null;
        if (getOutputFile() != null) {
            try {
                fw = new FileWriter(getOutputFile().getPath());
            } catch (IOException e) {
                String msg = "JDepend Failed when creating the output file: "
                    + e.getMessage();
                log(msg);
                throw new BuildException(msg);
            }
            jdepend.setWriter(new PrintWriter(fw));
            log("Output to be stored in " + getOutputFile().getPath());
        }


        try {
            if (getClassespath() != null) {
                // This is the new, better way - use classespath instead
                // of sourcespath.  The code is currently the same - you
                // need class files in a directory to use this or jar files.
                String[] cP = getClassespath().list();
                for (int i = 0; i < cP.length; i++) {
                    File f = new File(cP[i]);
                    // not necessary as JDepend would fail, but why loose
                    // some time?
                    if (!f.exists()) {
                        String msg = "\""
                            + f.getPath()
                            + "\" does not represent a valid"
                            + " file or directory. JDepend would fail.";
                        log(msg);
                        throw new BuildException(msg);
                    }
                    try {
                        jdepend.addDirectory(f.getPath());
                    } catch (IOException e) {
                        String msg =
                            "JDepend Failed when adding a class directory: "
                            + e.getMessage();
                        log(msg);
                        throw new BuildException(msg);
                    }
                }

            } else if (getSourcespath() != null) {

                // This is the old way and is deprecated - classespath is
                // the right way to do this and is above
                String[] sP = getSourcespath().list();
                for (int i = 0; i < sP.length; i++) {
                    File f = new File(sP[i]);

                    // not necessary as JDepend would fail, but why loose
                    // some time?
                    if (!f.exists() || !f.isDirectory()) {
                        String msg = "\""
                            + f.getPath()
                            + "\" does not represent a valid"
                            + " directory. JDepend would fail.";
                        log(msg);
                        throw new BuildException(msg);
                    }
                    try {
                        jdepend.addDirectory(f.getPath());
                    } catch (IOException e) {
                        String msg =
                            "JDepend Failed when adding a source directory: "
                            + e.getMessage();
                        log(msg);
                        throw new BuildException(msg);
                    }
                }
            }

            // This bit turns <exclude> child tags into patters to ignore
            String[] patterns = defaultPatterns.getExcludePatterns(getProject());
            if (patterns != null && patterns.length > 0) {
                if (setFilter != null) {
                    Vector v = new Vector();
                    for (int i = 0; i < patterns.length; i++) {
                        v.addElement(patterns[i]);
                    }
                    try {
                        Object o = packageFilterC.newInstance(new Object[] {v});
                        setFilter.invoke(jdepend, new Object[] {o});
                    } catch (Throwable e) {
                        log("excludes will be ignored as JDepend doesn't like me: "
                            + e.getMessage(), Project.MSG_WARN);
                    }
                } else {
                    log("Sorry, your version of JDepend doesn't support excludes",
                        Project.MSG_WARN);
                }
            }

            jdepend.analyze();
        } finally {
            FileUtils.close(fw);
        }
        return SUCCESS;
    }


    /**
     * Execute the task by forking a new JVM. The command will block until
     * it finishes. To know if the process was destroyed or not, use the
     * <tt>killedProcess()</tt> method of the watchdog class.
     * @param commandline the commandline for forked jvm
     * @param  watchdog   the watchdog in charge of cancelling the test if it
     * exceeds a certain amount of time. Can be <tt>null</tt>.
     * @return the result of running the jdepend
     * @throws BuildException in case of error
     */
    // JL: comment extracted from JUnitTask (and slightly modified)
    public int executeAsForked(CommandlineJava commandline,
                               ExecuteWatchdog watchdog) throws BuildException {
        runtimeClasses = new Path(getProject());
        addClasspathEntry("/jdepend/textui/JDepend.class");

        // if not set, auto-create the ClassPath from the project
        createClasspath();

        // not sure whether this test is needed but cost nothing to put.
        // hope it will be reviewed by anybody competent
        if (getClasspath().toString().length() > 0) {
            createJvmarg(commandline).setValue("-classpath");
            createJvmarg(commandline).setValue(getClasspath().toString());
        }

        if (includeRuntime) {
            Vector v = Execute.getProcEnvironment();
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                if (s.startsWith("CLASSPATH=")) {
                    commandline.createClasspath(getProject()).createPath()
                        .append(new Path(getProject(),
                                         s.substring("CLASSPATH=".length()
                                                     )));
                }
            }
            log("Implicitly adding " + runtimeClasses + " to CLASSPATH",
                Project.MSG_VERBOSE);
            commandline.createClasspath(getProject()).createPath()
                .append(runtimeClasses);
        }

        if (getOutputFile() != null) {
            // having a space between the file and its path causes commandline
            // to add quotes around the argument thus making JDepend not taking
            // it into account. Thus we split it in two
            commandline.createArgument().setValue("-file");
            commandline.createArgument().setValue(outputFile.getPath());
            // we have to find a cleaner way to put this output
        }

        if (getSourcespath() != null) {
            // This is deprecated - use classespath in the future
            String[] sP = getSourcespath().list();
            for (int i = 0; i < sP.length; i++) {
                File f = new File(sP[i]);

                // not necessary as JDepend would fail, but why loose
                // some time?
                if (!f.exists() || !f.isDirectory()) {
                    throw new BuildException("\"" + f.getPath()
                                             + "\" does not represent a valid"
                                             + " directory. JDepend would"
                                             + " fail.");
                }
                commandline.createArgument().setValue(f.getPath());
            }
        }

        if (getClassespath() != null) {
            // This is the new way - use classespath - code is the
            // same for now
            String[] cP = getClassespath().list();
            for (int i = 0; i < cP.length; i++) {
                File f = new File(cP[i]);
                // not necessary as JDepend would fail, but why loose
                // some time?
                if (!f.exists()) {
                    throw new BuildException("\"" + f.getPath()
                                             + "\" does not represent a valid"
                                             + " file or directory. JDepend would"
                                             + " fail.");
                }
                commandline.createArgument().setValue(f.getPath());
            }
        }

        Execute execute = new Execute(new LogStreamHandler(this,
            Project.MSG_INFO, Project.MSG_WARN), watchdog);
        execute.setCommandline(commandline.getCommandline());
        if (getDir() != null) {
            execute.setWorkingDirectory(getDir());
            execute.setAntRun(getProject());
        }

        if (getOutputFile() != null) {
            log("Output to be stored in " + getOutputFile().getPath());
        }
        log(commandline.describeCommand(), Project.MSG_VERBOSE);
        try {
            return execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed.", e, getLocation());
        }
    }

    /**
     * @return <tt>null</tt> if there is a timeout value, otherwise the
     * watchdog instance.
     * @throws BuildException in case of error
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (getTimeout() == null) {
            return null;
        }
        return new ExecuteWatchdog(getTimeout().longValue());
    }
}
