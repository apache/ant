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

package org.apache.tools.ant.taskdefs.optional.jdepend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import jdepend.textui.JDepend;

/**
 * Runs JDepend tests.
 *
 * <p>JDepend is a tool to generate design quality metrics for each Java package.
 * It has been initially created by Mike Clark. JDepend can be found at <a
 * href="https://github.com/clarkware/jdepend">https://github.com/clarkware/jdepend</a>.
 *
 * The current implementation spawn a new Java VM.
 *
 */
public class JDependTask extends Task {

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

    private static Constructor<?> packageFilterC;
    private static Method setFilter;

    private boolean includeRuntime = false;
    private Path runtimeClasses = null;

    static {
        try {
            Class<?> packageFilter =
                Class.forName("jdepend.framework.PackageFilter");
            packageFilterC =
                packageFilter.getConstructor(Collection.class);
            setFilter =
                JDepend.class.getDeclaredMethod("setFilter", packageFilter);
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
     * @param   value   <code>true</code> if a JVM should be forked,
     *                  otherwise <code>false</code>
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
     * Default is <code>java</code>. Ignored if no JVM is forked.
     * @param   value   the new VM to use instead of <code>java</code>
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
    @Deprecated
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
    @Deprecated
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
        private String[] formats = new String[] {"xml", "text"};

        /**
         * @return the enumerated values
         */
        @Override
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
        if (f == null) {
            log("Couldn't find " + resource, Project.MSG_DEBUG);
        } else {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            runtimeClasses.createPath().setLocation(f);
        }
    }

    /**
     * execute the task
     *
     * @exception BuildException if an error occurs
     */
    @Override
    public void execute() throws BuildException {

        CommandlineJava commandline = new CommandlineJava();

        if ("text".equals(format)) {
            commandline.setClassname("jdepend.textui.JDepend");
        } else if ("xml".equals(format)) {
            commandline.setClassname("jdepend.xmlui.JDepend");
        }

        if (jvm != null) {
            commandline.setVm(jvm);
        }
        if (getSourcespath() == null && getClassespath() == null) {
            throw new BuildException("Missing classespath required argument");
        }
        if (getClassespath() == null) {
            log("sourcespath is deprecated in JDepend >= 2.5 - please convert to classespath");
        }

        // execute the test and get the return code
        int exitValue;
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

            if (getHaltonerror()) {
                throw new BuildException(errorMessage, getLocation());
            }
            log(errorMessage, Project.MSG_ERR);
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
        PrintWriter pw = null;
        if (getOutputFile() != null) {
            try {
                fw = new FileWriter(getOutputFile().getPath()); //NOSONAR
            } catch (IOException e) {
                String msg = "JDepend Failed when creating the output file: "
                    + e.getMessage();
                log(msg);
                throw new BuildException(msg);
            }
            pw = new PrintWriter(fw);
            jdepend.setWriter(pw);
            log("Output to be stored in " + getOutputFile().getPath());
        }

        try {
            getWorkingPath().ifPresent(path -> {
                for (String filepath : path.list()) {
                    File f = new File(filepath);
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
            });

            // This bit turns <exclude> child tags into patters to ignore
            String[] patterns = defaultPatterns.getExcludePatterns(getProject());
            if (patterns != null && patterns.length > 0) {
                if (setFilter != null) {
                    List<String> v = new ArrayList<>();
                    Collections.addAll(v, patterns);
                    try {
                        Object o = packageFilterC.newInstance(v);
                        setFilter.invoke(jdepend, o);
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
            if (pw != null && pw.checkError()) {
                throw new IOException(
                    "Encountered an error writing JDepend output");
            }
        } catch (IOException ex) {
            throw new BuildException(ex);
        } finally {
            FileUtils.close(pw);
            FileUtils.close(fw);
        }
        return SUCCESS;
    }

    /**
     * Execute the task by forking a new JVM. The command will block until
     * it finishes. To know if the process was destroyed or not, use the
     * <code>killedProcess()</code> method of the watchdog class.
     * @param commandline the commandline for forked jvm
     * @param  watchdog   the watchdog in charge of cancelling the test if it
     * exceeds a certain amount of time. Can be <code>null</code>.
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
        if (!getClasspath().toString().isEmpty()) {
            createJvmarg(commandline).setValue("-classpath");
            createJvmarg(commandline).setValue(getClasspath().toString());
        }

        if (includeRuntime) {
            Map<String, String> env = Execute.getEnvironmentVariables();
            String cp = env.get("CLASSPATH");
            if (cp != null) {
                commandline.createClasspath(getProject()).createPath()
                    .append(new Path(getProject(), cp));
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

        getWorkingPath().ifPresent(path -> {
            for (String filepath : path.list()) {
                File f = new File(filepath);

                // not necessary as JDepend would fail, but why loose
                // some time?
                if (!f.exists() || !f.isDirectory()) {
                    throw new BuildException(
                        "\"%s\" does not represent a valid directory. JDepend would fail.",
                        f.getPath());
                }
                commandline.createArgument().setValue(f.getPath());
            }
        });
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
     * @return <code>null</code> if there is a timeout value, otherwise the
     * watchdog instance.
     * @throws BuildException in case of error
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (getTimeout() == null) {
            return null;
        }
        return new ExecuteWatchdog(getTimeout());
    }

    private Optional<Path> getWorkingPath() {
        Optional<Path> result = Optional.ofNullable(getClassespath());
        if (result.isPresent()) {
            return result;
        }
        result = Optional.ofNullable(getSourcespath());
        result.ifPresent(resources -> log("nested sourcespath is deprecated; please use classespath"));
        return result;
    }

}
