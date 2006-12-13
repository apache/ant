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

package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;

/**
 * Compiles Java source files. This task can take the following
 * arguments:
 * <ul>
 * <li>sourcedir
 * <li>destdir
 * <li>deprecation
 * <li>classpath
 * <li>bootclasspath
 * <li>extdirs
 * <li>optimize
 * <li>debug
 * <li>encoding
 * <li>target
 * <li>depend
 * <li>verbose
 * <li>failonerror
 * <li>includeantruntime
 * <li>includejavaruntime
 * <li>source
 * <li>compiler
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * When this task executes, it will recursively scan the sourcedir and
 * destdir looking for Java source files to compile. This task makes its
 * compile decision based on timestamp.
 *
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */

public class Javac extends MatchingTask {

    private static final String FAIL_MSG
        = "Compile failed; see the compiler error output for details.";

    private static final String JAVAC16 = "javac1.6";
    private static final String JAVAC15 = "javac1.5";
    private static final String JAVAC14 = "javac1.4";
    private static final String JAVAC13 = "javac1.3";
    private static final String JAVAC12 = "javac1.2";
    private static final String JAVAC11 = "javac1.1";
    private static final String MODERN = "modern";
    private static final String CLASSIC = "classic";
    private static final String EXTJAVAC = "extJavac";

    private Path src;
    private File destDir;
    private Path compileClasspath;
    private Path compileSourcepath;
    private String encoding;
    private boolean debug = false;
    private boolean optimize = false;
    private boolean deprecation = false;
    private boolean depend = false;
    private boolean verbose = false;
    private String targetAttribute;
    private Path bootclasspath;
    private Path extdirs;
    private boolean includeAntRuntime = true;
    private boolean includeJavaRuntime = false;
    private boolean fork = false;
    private String forkedExecutable = null;
    private boolean nowarn = false;
    private String memoryInitialSize;
    private String memoryMaximumSize;
    private FacadeTaskHelper facade = null;

    // CheckStyle:VisibilityModifier OFF - bc
    protected boolean failOnError = true;
    protected boolean listFiles = false;
    protected File[] compileList = new File[0];
    // CheckStyle:VisibilityModifier ON

    private String source;
    private String debugLevel;
    private File tmpDir;

    /**
     * Javac task for compilation of Java files.
     */
    public Javac() {
        facade = new FacadeTaskHelper(assumedJavaVersion());
    }

    private String assumedJavaVersion() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)) {
            return JAVAC12;
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) {
            return JAVAC13;
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4)) {
            return JAVAC14;
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_5)) {
            return JAVAC15;
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_6)) {
            return JAVAC16;
        } else {
            return CLASSIC;
        }
    }

    /**
     * Get the value of debugLevel.
     * @return value of debugLevel.
     */
    public String getDebugLevel() {
        return debugLevel;
    }

    /**
     * Keyword list to be appended to the -g command-line switch.
     *
     * This will be ignored by all implementations except modern
     * and classic(ver >= 1.2). Legal values are none or a
     * comma-separated list of the following keywords: lines, vars,
     * and source. If debuglevel is not specified, by default, :none
     * will be appended to -g. If debug is not turned on, this attribute
     * will be ignored.
     *
     * @param v  Value to assign to debugLevel.
     */
    public void setDebugLevel(String  v) {
        this.debugLevel = v;
    }

    /**
     * Get the value of source.
     * @return value of source.
     */
    public String getSource() {
        return source != null
            ? source : getProject().getProperty(MagicNames.BUILD_JAVAC_SOURCE);
    }

    /**
     * Value of the -source command-line switch; will be ignored
     * by all implementations except modern and jikes.
     *
     * If you use this attribute together with jikes, you must make
     * sure that your version of jikes supports the -source switch.
     * Legal values are 1.3, 1.4, 1.5, and 5 - by default, no
     * -source argument will be used at all.
     *
     * @param v  Value to assign to source.
     */
    public void setSource(String  v) {
        this.source = v;
    }

    /**
     * Adds a path for source compilation.
     *
     * @return a nested src element.
     */
    public Path createSrc() {
        if (src == null) {
            src = new Path(getProject());
        }
        return src.createPath();
    }

    /**
     * Recreate src.
     *
     * @return a nested src element.
     */
    protected Path recreateSrc() {
        src = null;
        return createSrc();
    }

    /**
     * Set the source directories to find the source Java files.
     * @param srcDir the source directories as a path
     */
    public void setSrcdir(Path srcDir) {
        if (src == null) {
            src = srcDir;
        } else {
            src.append(srcDir);
        }
    }

    /**
     * Gets the source dirs to find the source java files.
     * @return the source directories as a path
     */
    public Path getSrcdir() {
        return src;
    }

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     * @param destDir the destination director
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Gets the destination directory into which the java source files
     * should be compiled.
     * @return the destination directory
     */
    public File getDestdir() {
        return destDir;
    }

    /**
     * Set the sourcepath to be used for this compilation.
     * @param sourcepath the source path
     */
    public void setSourcepath(Path sourcepath) {
        if (compileSourcepath == null) {
            compileSourcepath = sourcepath;
        } else {
            compileSourcepath.append(sourcepath);
        }
    }

    /**
     * Gets the sourcepath to be used for this compilation.
     * @return the source path
     */
    public Path getSourcepath() {
        return compileSourcepath;
    }

    /**
     * Adds a path to sourcepath.
     * @return a sourcepath to be configured
     */
    public Path createSourcepath() {
        if (compileSourcepath == null) {
            compileSourcepath = new Path(getProject());
        }
        return compileSourcepath.createPath();
    }

    /**
     * Adds a reference to a source path defined elsewhere.
     * @param r a reference to a source path
     */
    public void setSourcepathRef(Reference r) {
        createSourcepath().setRefid(r);
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
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
     * @return the class path
     */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Adds a path to the classpath.
     * @return a class path to be configured
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @param r a reference to a classpath
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Sets the bootclasspath that will be used to compile the classes
     * against.
     * @param bootclasspath a path to use as a boot class path (may be more
     *                      than one)
     */
    public void setBootclasspath(Path bootclasspath) {
        if (this.bootclasspath == null) {
            this.bootclasspath = bootclasspath;
        } else {
            this.bootclasspath.append(bootclasspath);
        }
    }

    /**
     * Gets the bootclasspath that will be used to compile the classes
     * against.
     * @return the boot path
     */
    public Path getBootclasspath() {
        return bootclasspath;
    }

    /**
     * Adds a path to the bootclasspath.
     * @return a path to be configured
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(getProject());
        }
        return bootclasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     * @param r a reference to a classpath
     */
    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    /**
     * Sets the extension directories that will be used during the
     * compilation.
     * @param extdirs a path
     */
    public void setExtdirs(Path extdirs) {
        if (this.extdirs == null) {
            this.extdirs = extdirs;
        } else {
            this.extdirs.append(extdirs);
        }
    }

    /**
     * Gets the extension directories that will be used during the
     * compilation.
     * @return the extension directories as a path
     */
    public Path getExtdirs() {
        return extdirs;
    }

    /**
     * Adds a path to extdirs.
     * @return a path to be configured
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path(getProject());
        }
        return extdirs.createPath();
    }

    /**
     * If true, list the source files being handed off to the compiler.
     * @param list if true list the source files
     */
    public void setListfiles(boolean list) {
        listFiles = list;
    }

    /**
     * Get the listfiles flag.
     * @return the listfiles flag
     */
    public boolean getListfiles() {
        return listFiles;
    }

    /**
     * Indicates whether the build will continue
     * even if there are compilation errors; defaults to true.
     * @param fail if true halt the build on failure
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * @ant.attribute ignore="true"
     * @param proceed inverse of failoferror
     */
    public void setProceed(boolean proceed) {
        failOnError = !proceed;
    }

    /**
     * Gets the failonerror flag.
     * @return the failonerror flag
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Indicates whether source should be
     * compiled with deprecation information; defaults to off.
     * @param deprecation if true turn on deprecation information
     */
    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

    /**
     * Gets the deprecation flag.
     * @return the deprecation flag
     */
    public boolean getDeprecation() {
        return deprecation;
    }

    /**
     * The initial size of the memory for the underlying VM
     * if javac is run externally; ignored otherwise.
     * Defaults to the standard VM memory setting.
     * (Examples: 83886080, 81920k, or 80m)
     * @param memoryInitialSize string to pass to VM
     */
    public void setMemoryInitialSize(String memoryInitialSize) {
        this.memoryInitialSize = memoryInitialSize;
    }

    /**
     * Gets the memoryInitialSize flag.
     * @return the memoryInitialSize flag
     */
    public String getMemoryInitialSize() {
        return memoryInitialSize;
    }

    /**
     * The maximum size of the memory for the underlying VM
     * if javac is run externally; ignored otherwise.
     * Defaults to the standard VM memory setting.
     * (Examples: 83886080, 81920k, or 80m)
     * @param memoryMaximumSize string to pass to VM
     */
    public void setMemoryMaximumSize(String memoryMaximumSize) {
        this.memoryMaximumSize = memoryMaximumSize;
    }

    /**
     * Gets the memoryMaximumSize flag.
     * @return the memoryMaximumSize flag
     */
    public String getMemoryMaximumSize() {
        return memoryMaximumSize;
    }

    /**
     * Set the Java source file encoding name.
     * @param encoding the source file encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Gets the java source file encoding name.
     * @return the source file encoding name
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Indicates whether source should be compiled
     * with debug information; defaults to off.
     * @param debug if true compile with debug information
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Gets the debug flag.
     * @return the debug flag
     */
    public boolean getDebug() {
        return debug;
    }

    /**
     * If true, compiles with optimization enabled.
     * @param optimize if true compile with optimization enabled
     */
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    /**
     * Gets the optimize flag.
     * @return the optimize flag
     */
    public boolean getOptimize() {
        return optimize;
    }

    /**
     * Enables dependency-tracking for compilers
     * that support this (jikes and classic).
     * @param depend if true enable dependency-tracking
     */
    public void setDepend(boolean depend) {
        this.depend = depend;
    }

    /**
     * Gets the depend flag.
     * @return the depend flag
     */
    public boolean getDepend() {
        return depend;
    }

    /**
     * If true, asks the compiler for verbose output.
     * @param verbose if true, asks the compiler for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Gets the verbose flag.
     * @return the verbose flag
     */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * Sets the target VM that the classes will be compiled for. Valid
     * values depend on the compiler, for jdk 1.4 the valid values are
     * "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "5" and "6".
     * @param target the target VM
     */
    public void setTarget(String target) {
        this.targetAttribute = target;
    }

    /**
     * Gets the target VM that the classes will be compiled for.
     * @return the target VM
     */
    public String getTarget() {
        return targetAttribute != null
            ? targetAttribute
            : getProject().getProperty(MagicNames.BUILD_JAVAC_TARGET);
    }

    /**
     * If true, includes Ant's own classpath in the classpath.
     * @param include if true, includes Ant's own classpath in the classpath
     */
    public void setIncludeantruntime(boolean include) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the classpath.
     * @return whether or not the ant classpath is to be included in the classpath
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime;
    }

    /**
     * If true, includes the Java runtime libraries in the classpath.
     * @param include if true, includes the Java runtime libraries in the classpath
     */
    public void setIncludejavaruntime(boolean include) {
        includeJavaRuntime = include;
    }

    /**
     * Gets whether or not the java runtime should be included in this
     * task's classpath.
     * @return the includejavaruntime attribute
     */
    public boolean getIncludejavaruntime() {
        return includeJavaRuntime;
    }

    /**
     * If true, forks the javac compiler.
     *
     * @param f "true|false|on|off|yes|no"
     */
    public void setFork(boolean f) {
        fork = f;
    }

    /**
     * Sets the name of the javac executable.
     *
     * <p>Ignored unless fork is true or extJavac has been specified
     * as the compiler.</p>
     * @param forkExec the name of the executable
     */
    public void setExecutable(String forkExec) {
        forkedExecutable = forkExec;
    }

    /**
     * The value of the executable attribute, if any.
     *
     * @since Ant 1.6
     * @return the name of the java executable
     */
    public String getExecutable() {
        return forkedExecutable;
    }

    /**
     * Is this a forked invocation of JDK's javac?
     * @return true if this is a forked invocation
     */
    public boolean isForkedJavac() {
        return fork || "extJavac".equals(getCompiler());
    }

    /**
     * The name of the javac executable to use in fork-mode.
     *
     * <p>This is either the name specified with the executable
     * attribute or the full path of the javac compiler of the VM Ant
     * is currently running in - guessed by Ant.</p>
     *
     * <p>You should <strong>not</strong> invoke this method if you
     * want to get the value of the executable command - use {@link
     * #getExecutable getExecutable} for this.</p>
     * @return the name of the javac executable
     */
    public String getJavacExecutable() {
        if (forkedExecutable == null && isForkedJavac()) {
            forkedExecutable = getSystemJavac();
        } else if (forkedExecutable != null && !isForkedJavac()) {
            forkedExecutable = null;
        }
        return forkedExecutable;
    }

    /**
     * If true, enables the -nowarn option.
     * @param flag if true, enable the -nowarn option
     */
    public void setNowarn(boolean flag) {
        this.nowarn = flag;
    }

    /**
     * Should the -nowarn option be used.
     * @return true if the -nowarn option should be used
     */
    public boolean getNowarn() {
        return nowarn;
    }

    /**
     * Adds an implementation specific command-line argument.
     * @return a ImplementationSpecificArgument to be configured
     */
    public ImplementationSpecificArgument createCompilerArg() {
        ImplementationSpecificArgument arg =
            new ImplementationSpecificArgument();
        facade.addImplementationArgument(arg);
        return arg;
    }

    /**
     * Get the additional implementation specific command line arguments.
     * @return array of command line arguments, guaranteed to be non-null.
     */
    public String[] getCurrentCompilerArgs() {
        String chosen = facade.getExplicitChoice();
        try {
            // make sure facade knows about magic properties and fork setting
            String appliedCompiler = getCompiler();
            facade.setImplementation(appliedCompiler);

            String[] result = facade.getArgs();

            String altCompilerName = getAltCompilerName(facade.getImplementation());

            if (result.length == 0 && altCompilerName != null) {
                facade.setImplementation(altCompilerName);
                result = facade.getArgs();
            }

            return result;

        } finally {
            facade.setImplementation(chosen);
        }
    }

    private String getAltCompilerName(String anImplementation) {
        if (JAVAC16.equalsIgnoreCase(anImplementation)
                || JAVAC15.equalsIgnoreCase(anImplementation)
                || JAVAC14.equalsIgnoreCase(anImplementation)
                || JAVAC13.equalsIgnoreCase(anImplementation)) {
            return MODERN;
        }
        if (JAVAC12.equalsIgnoreCase(anImplementation)
                || JAVAC11.equalsIgnoreCase(anImplementation)) {
            return CLASSIC;
        }
        if (MODERN.equalsIgnoreCase(anImplementation)) {
            String nextSelected = assumedJavaVersion();
            if (JAVAC16.equalsIgnoreCase(nextSelected)
                    || JAVAC15.equalsIgnoreCase(nextSelected)
                    || JAVAC14.equalsIgnoreCase(nextSelected)
                    || JAVAC13.equalsIgnoreCase(nextSelected)) {
                return nextSelected;
            }
        }
        if (CLASSIC.equals(anImplementation)) {
            return assumedJavaVersion();
        }
        if (EXTJAVAC.equalsIgnoreCase(anImplementation)) {
            return assumedJavaVersion();
        }
        return null;
    }

    /**
     * Where Ant should place temporary files.
     *
     * @since Ant 1.6
     * @param tmpDir the temporary directory
     */
    public void setTempdir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    /**
     * Where Ant should place temporary files.
     *
     * @since Ant 1.6
     * @return the temporary directory
     */
    public File getTempdir() {
        return tmpDir;
    }

    /**
     * Executes the task.
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        checkParameters();
        resetFileLists();

        // scan source directories and dest directory to build up
        // compile lists
        String[] list = src.list();
        for (int i = 0; i < list.length; i++) {
            File srcDir = getProject().resolveFile(list[i]);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir \""
                                         + srcDir.getPath()
                                         + "\" does not exist!", getLocation());
            }

            DirectoryScanner ds = this.getDirectoryScanner(srcDir);
            String[] files = ds.getIncludedFiles();

            scanDir(srcDir, destDir != null ? destDir : srcDir, files);
        }

        compile();
    }

    /**
     * Clear the list of files to be compiled and copied..
     */
    protected void resetFileLists() {
        compileList = new File[0];
    }

    /**
     * Scans the directory looking for source files to be compiled.
     * The results are returned in the class variable compileList
     *
     * @param srcDir   The source directory
     * @param destDir  The destination directory
     * @param files    An array of filenames
     */
    protected void scanDir(File srcDir, File destDir, String[] files) {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("*.java");
        m.setTo("*.class");
        SourceFileScanner sfs = new SourceFileScanner(this);
        File[] newFiles = sfs.restrictAsFiles(files, srcDir, destDir, m);

        if (newFiles.length > 0) {
            File[] newCompileList
                = new File[compileList.length + newFiles.length];
            System.arraycopy(compileList, 0, newCompileList, 0,
                    compileList.length);
            System.arraycopy(newFiles, 0, newCompileList,
                    compileList.length, newFiles.length);
            compileList = newCompileList;
        }
    }

    /**
     * Gets the list of files to be compiled.
     * @return the list of files as an array
     */
    public File[] getFileList() {
        return compileList;
    }

    /**
     * Is the compiler implementation a jdk compiler
     *
     * @param compilerImpl the name of the compiler implementation
     * @return true if compilerImpl is "modern", "classic",
     * "javac1.1", "javac1.2", "javac1.3", "javac1.4", "javac1.5" or
     * "javac1.6".
     */
    protected boolean isJdkCompiler(String compilerImpl) {
        return MODERN.equals(compilerImpl)
            || CLASSIC.equals(compilerImpl)
            || JAVAC16.equals(compilerImpl)
            || JAVAC15.equals(compilerImpl)
            || JAVAC14.equals(compilerImpl)
            || JAVAC13.equals(compilerImpl)
            || JAVAC12.equals(compilerImpl)
            || JAVAC11.equals(compilerImpl);
    }

    /**
     * @return the executable name of the java compiler
     */
    protected String getSystemJavac() {
        return JavaEnvUtils.getJdkExecutable("javac");
    }

    /**
     * Choose the implementation for this particular task.
     * @param compiler the name of the compiler
     * @since Ant 1.5
     */
    public void setCompiler(String compiler) {
        facade.setImplementation(compiler);
    }

    /**
     * The implementation for this particular task.
     *
     * <p>Defaults to the build.compiler property but can be overridden
     * via the compiler and fork attributes.</p>
     *
     * <p>If fork has been set to true, the result will be extJavac
     * and not classic or java1.2 - no matter what the compiler
     * attribute looks like.</p>
     *
     * @see #getCompilerVersion
     * @return the compiler.
     * @since Ant 1.5
     */
    public String getCompiler() {
        String compilerImpl = getCompilerVersion();
        if (fork) {
            if (isJdkCompiler(compilerImpl)) {
                compilerImpl = "extJavac";
            } else {
                log("Since compiler setting isn't classic or modern,"
                    + "ignoring fork setting.", Project.MSG_WARN);
            }
        }
        return compilerImpl;
    }

    /**
     * The implementation for this particular task.
     *
     * <p>Defaults to the build.compiler property but can be overridden
     * via the compiler attribute.</p>
     *
     * <p>This method does not take the fork attribute into
     * account.</p>
     *
     * @see #getCompiler
     * @return the compiler.
     *
     * @since Ant 1.5
     */
    public String getCompilerVersion() {
        facade.setMagicValue(getProject().getProperty("build.compiler"));
        return facade.getImplementation();
    }

    /**
     * Check that all required attributes have been set and nothing
     * silly has been entered.
     *
     * @since Ant 1.5
     * @exception BuildException if an error occurs
     */
    protected void checkParameters() throws BuildException {
        if (src == null) {
            throw new BuildException("srcdir attribute must be set!",
                                     getLocation());
        }
        if (src.size() == 0) {
            throw new BuildException("srcdir attribute must be set!",
                                     getLocation());
        }

        if (destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory \""
                                     + destDir
                                     + "\" does not exist "
                                     + "or is not a directory", getLocation());
        }
    }

    /**
     * Perform the compilation.
     *
     * @since Ant 1.5
     */
    protected void compile() {
        String compilerImpl = getCompiler();

        if (compileList.length > 0) {
            log("Compiling " + compileList.length + " source file"
                + (compileList.length == 1 ? "" : "s")
                + (destDir != null ? " to " + destDir : ""));

            if (listFiles) {
                for (int i = 0; i < compileList.length; i++) {
                  String filename = compileList[i].getAbsolutePath();
                  log(filename);
                }
            }

            CompilerAdapter adapter =
                CompilerAdapterFactory.getCompiler(compilerImpl, this);

            // now we need to populate the compiler adapter
            adapter.setJavac(this);

            // finally, lets execute the compiler!!
            if (!adapter.execute()) {
                if (failOnError) {
                    throw new BuildException(FAIL_MSG, getLocation());
                } else {
                    log(FAIL_MSG, Project.MSG_ERR);
                }
            }
        }
    }

    /**
     * Adds an "compiler" attribute to Commandline$Attribute used to
     * filter command line attributes based on the current
     * implementation.
     */
    public class ImplementationSpecificArgument extends
        org.apache.tools.ant.util.facade.ImplementationSpecificArgument {

        /**
         * @param impl the name of the compiler
         */
        public void setCompiler(String impl) {
            super.setImplementation(impl);
        }
    }

}
