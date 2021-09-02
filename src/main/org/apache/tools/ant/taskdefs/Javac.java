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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterExtension;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileUtils;
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
 * <li>release
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

    private static final char GROUP_START_MARK = '{';   //modulesourcepath group start character
    private static final char GROUP_END_MARK = '}';   //modulesourcepath group end character
    private static final char GROUP_SEP_MARK = ',';   //modulesourcepath group element separator character
    private static final String MODULE_MARKER = "*";    //modulesourcepath module name marker

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private Path src;
    private File destDir;
    private File nativeHeaderDir;
    private Path compileClasspath;
    private Path modulepath;
    private Path upgrademodulepath;
    private Path compileSourcepath;
    private Path moduleSourcepath;
    private String encoding;
    private boolean debug = false;
    private boolean optimize = false;
    private boolean deprecation = false;
    private boolean depend = false;
    private boolean verbose = false;
    private String targetAttribute;
    private String release;
    private Path bootclasspath;
    private Path extdirs;
    private Boolean includeAntRuntime;
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
    private Map<String, Long> packageInfos = new HashMap<>();
    // CheckStyle:VisibilityModifier ON

    private String source;
    private String debugLevel;
    private File tmpDir;
    private String updatedProperty;
    private String errorProperty;
    private boolean taskSuccess = true; // assume the best
    private boolean includeDestClasses = true;
    private CompilerAdapter nestedAdapter = null;

    private boolean createMissingPackageInfoClass = true;

    /**
     * Javac task for compilation of Java files.
     */
    public Javac() {
        facade = new FacadeTaskHelper(assumedJavaVersion());
    }

    private String assumedJavaVersion() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_8)) {
            return CompilerAdapterFactory.COMPILER_JAVAC_1_8;
        }
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_9)) {
            return CompilerAdapterFactory.COMPILER_JAVAC_9;
        }
        if (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_10)) {
            return CompilerAdapterFactory.COMPILER_JAVAC_10_PLUS;
        }
        // as we are assumed to be 1.8+ and classic refers to the really old ones,  default to modern
        return CompilerAdapterFactory.COMPILER_MODERN;
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
     * and classic(ver &gt;= 1.2). Legal values are none or a
     * comma-separated list of the following keywords: lines, vars,
     * and source. If debuglevel is not specified, by default, :none
     * will be appended to -g. If debug is not turned on, this attribute
     * will be ignored.
     *
     * @param v  Value to assign to debugLevel.
     */
    public void setDebugLevel(final String  v) {
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
     * Value of the -source command-line switch; will be ignored by
     * all implementations except modern, jikes and gcj (gcj uses
     * -fsource).
     *
     * <p>If you use this attribute together with jikes or gcj, you
     * must make sure that your version of jikes supports the -source
     * switch.</p>
     *
     * <p>Legal values are 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, and any integral number bigger than 4
     * - by default, no -source argument will be used at all.</p>
     *
     * @param v  Value to assign to source.
     */
    public void setSource(final String  v) {
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
    public void setSrcdir(final Path srcDir) {
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
    public void setDestdir(final File destDir) {
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
     * Set the destination directory into which the generated native
     * header files should be placed.
     * @param nhDir where to place generated native header files
     * @since Ant 1.9.8
     */
    public void setNativeHeaderDir(final File nhDir) {
        this.nativeHeaderDir = nhDir;
    }

    /**
     * Gets the destination directory into which the generated native
     * header files should be placed.
     * @return where to place generated native header files
     * @since Ant 1.9.8
     */
    public File getNativeHeaderDir() {
        return nativeHeaderDir;
    }

    /**
     * Set the sourcepath to be used for this compilation.
     * @param sourcepath the source path
     */
    public void setSourcepath(final Path sourcepath) {
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
    public void setSourcepathRef(final Reference r) {
        createSourcepath().setRefid(r);
    }

    /**
     * Set the modulesourcepath to be used for this compilation.
     * @param msp  the modulesourcepath
     * @since 1.9.7
     */
    public void setModulesourcepath(final Path msp) {
        if (moduleSourcepath == null) {
            moduleSourcepath = msp;
        } else {
            moduleSourcepath.append(msp);
        }
    }

    /**
     * Gets the modulesourcepath to be used for this compilation.
     * @return the modulesourcepath
     * @since 1.9.7
     */
    public Path getModulesourcepath() {
        return moduleSourcepath;
    }

    /**
     * Adds a path to modulesourcepath.
     * @return a modulesourcepath to be configured
     * @since 1.9.7
     */
    public Path createModulesourcepath() {
        if (moduleSourcepath == null) {
            moduleSourcepath = new Path(getProject());
        }
        return moduleSourcepath.createPath();
    }

    /**
     * Adds a reference to a modulesourcepath defined elsewhere.
     * @param r a reference to a modulesourcepath
     * @since 1.9.7
     */
    public void setModulesourcepathRef(final Reference r) {
        createModulesourcepath().setRefid(r);
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param classpath an Ant Path object containing the compilation classpath.
     */
    public void setClasspath(final Path classpath) {
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
    public void setClasspathRef(final Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Set the modulepath to be used for this compilation.
     * @param mp an Ant Path object containing the modulepath.
     * @since 1.9.7
     */
    public void setModulepath(final Path mp) {
        if (modulepath == null) {
            modulepath = mp;
        } else {
            modulepath.append(mp);
        }
    }

    /**
     * Gets the modulepath to be used for this compilation.
     * @return the modulepath
     * @since 1.9.7
     */
    public Path getModulepath() {
        return modulepath;
    }

    /**
     * Adds a path to the modulepath.
     * @return a modulepath to be configured
     * @since 1.9.7
     */
    public Path createModulepath() {
        if (modulepath == null) {
            modulepath = new Path(getProject());
        }
        return modulepath.createPath();
    }

    /**
     * Adds a reference to a modulepath defined elsewhere.
     * @param r a reference to a modulepath
     * @since 1.9.7
     */
    public void setModulepathRef(final Reference r) {
        createModulepath().setRefid(r);
    }

    /**
     * Set the upgrademodulepath to be used for this compilation.
     * @param ump an Ant Path object containing the upgrademodulepath.
     * @since 1.9.7
     */
    public void setUpgrademodulepath(final Path ump) {
        if (upgrademodulepath == null) {
            upgrademodulepath = ump;
        } else {
            upgrademodulepath.append(ump);
        }
    }

    /**
     * Gets the upgrademodulepath to be used for this compilation.
     * @return the upgrademodulepath
     * @since 1.9.7
     */
    public Path getUpgrademodulepath() {
        return upgrademodulepath;
    }

    /**
     * Adds a path to the upgrademodulepath.
     * @return an upgrademodulepath to be configured
     * @since 1.9.7
     */
    public Path createUpgrademodulepath() {
        if (upgrademodulepath == null) {
            upgrademodulepath = new Path(getProject());
        }
        return upgrademodulepath.createPath();
    }

    /**
     * Adds a reference to the upgrademodulepath defined elsewhere.
     * @param r a reference to an upgrademodulepath
     * @since 1.9.7
     */
    public void setUpgrademodulepathRef(final Reference r) {
        createUpgrademodulepath().setRefid(r);
    }

    /**
     * Sets the bootclasspath that will be used to compile the classes
     * against.
     * @param bootclasspath a path to use as a boot class path (may be more
     *                      than one)
     */
    public void setBootclasspath(final Path bootclasspath) {
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
    public void setBootClasspathRef(final Reference r) {
        createBootclasspath().setRefid(r);
    }

    /**
     * Sets the extension directories that will be used during the
     * compilation.
     * @param extdirs a path
     */
    public void setExtdirs(final Path extdirs) {
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
    public void setListfiles(final boolean list) {
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
    public void setFailonerror(final boolean fail) {
        failOnError = fail;
    }

    /**
     * @ant.attribute ignore="true"
     * @param proceed inverse of failoferror
     */
    public void setProceed(final boolean proceed) {
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
    public void setDeprecation(final boolean deprecation) {
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
    public void setMemoryInitialSize(final String memoryInitialSize) {
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
    public void setMemoryMaximumSize(final String memoryMaximumSize) {
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
    public void setEncoding(final String encoding) {
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
    public void setDebug(final boolean debug) {
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
    public void setOptimize(final boolean optimize) {
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
    public void setDepend(final boolean depend) {
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
    public void setVerbose(final boolean verbose) {
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
     * "1.1", "1.2", "1.3", "1.4", "1.5", "1.6", "1.7", "1.8", "1.9" and any integral number bigger than 4
     * @param target the target VM
     */
    public void setTarget(final String target) {
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
     * Sets the version to use for the {@code --release} switch that
     * combines {@code source}, {@code target} and setting the
     * bootclasspath.
     *
     * Values depend on the compiler, for jdk 9 the valid values are
     * "6", "7", "8", "9".
     * @param release the value of the release attribute
     * @since Ant 1.9.8
     */
    public void setRelease(final String release) {
        this.release = release;
    }

    /**
     * Gets the version to use for the {@code --release} switch that
     * combines {@code source}, {@code target} and setting the
     * bootclasspath.
     *
     * @return the value of the release attribute
     * @since Ant 1.9.8
     */
    public String getRelease() {
        return release;
    }

    /**
     * If true, includes Ant's own classpath in the classpath.
     * @param include if true, includes Ant's own classpath in the classpath
     */
    public void setIncludeantruntime(final boolean include) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the classpath.
     * @return whether or not the ant classpath is to be included in the classpath
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime == null || includeAntRuntime;
    }

    /**
     * If true, includes the Java runtime libraries in the classpath.
     * @param include if true, includes the Java runtime libraries in the classpath
     */
    public void setIncludejavaruntime(final boolean include) {
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
    public void setFork(final boolean f) {
        fork = f;
    }

    /**
     * Sets the name of the javac executable.
     *
     * <p>Ignored unless fork is true or extJavac has been specified
     * as the compiler.</p>
     * @param forkExec the name of the executable
     */
    public void setExecutable(final String forkExec) {
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
        return fork || CompilerAdapterFactory.isForkedJavac(getCompiler());
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
    public void setNowarn(final boolean flag) {
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
        final ImplementationSpecificArgument arg =
            new ImplementationSpecificArgument();
        facade.addImplementationArgument(arg);
        return arg;
    }

    /**
     * Get the additional implementation specific command line arguments.
     * @return array of command line arguments, guaranteed to be non-null.
     */
    public String[] getCurrentCompilerArgs() {
        final String chosen = facade.getExplicitChoice();
        try {
            // make sure facade knows about magic properties and fork setting
            final String appliedCompiler = getCompiler();
            facade.setImplementation(appliedCompiler);

            String[] result = facade.getArgs();

            final String altCompilerName = getAltCompilerName(facade.getImplementation());

            if (result.length == 0 && altCompilerName != null) {
                facade.setImplementation(altCompilerName);
                result = facade.getArgs();
            }

            return result;

        } finally {
            facade.setImplementation(chosen);
        }
    }

    private String getAltCompilerName(final String anImplementation) {
        if (CompilerAdapterFactory.isModernJdkCompiler(anImplementation)) {
            return CompilerAdapterFactory.COMPILER_MODERN;
        }
        if (CompilerAdapterFactory.isClassicJdkCompiler(anImplementation)) {
            return CompilerAdapterFactory.COMPILER_CLASSIC;
        }
        if (CompilerAdapterFactory.COMPILER_MODERN.equalsIgnoreCase(anImplementation)) {
            final String nextSelected = assumedJavaVersion();
            if (CompilerAdapterFactory.isModernJdkCompiler(nextSelected)) {
                return nextSelected;
            }
        }
        if (CompilerAdapterFactory.COMPILER_CLASSIC.equalsIgnoreCase(anImplementation)) {
            return assumedJavaVersion();
        }
        if (CompilerAdapterFactory.isForkedJavac(anImplementation)) {
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
    public void setTempdir(final File tmpDir) {
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
     * The property to set on compilation success.
     * This property will not be set if the compilation
     * fails, or if there are no files to compile.
     * @param updatedProperty the property name to use.
     * @since Ant 1.7.1.
     */
    public void setUpdatedProperty(final String updatedProperty) {
        this.updatedProperty = updatedProperty;
    }

    /**
     * The property to set on compilation failure.
     * This property will be set if the compilation
     * fails.
     * @param errorProperty the property name to use.
     * @since Ant 1.7.1.
     */
    public void setErrorProperty(final String errorProperty) {
        this.errorProperty = errorProperty;
    }

    /**
     * This property controls whether to include the
     * destination classes directory in the classpath
     * given to the compiler.
     * The default value is "true".
     * @param includeDestClasses the value to use.
     */
    public void setIncludeDestClasses(final boolean includeDestClasses) {
        this.includeDestClasses = includeDestClasses;
    }

    /**
     * Get the value of the includeDestClasses property.
     * @return the value.
     */
    public boolean isIncludeDestClasses() {
        return includeDestClasses;
    }

    /**
     * Get the result of the javac task (success or failure).
     * @return true if compilation succeeded, or
     *         was not necessary, false if the compilation failed.
     */
    public boolean getTaskSuccess() {
        return taskSuccess;
    }

    /**
     * The classpath to use when loading the compiler implementation
     * if it is not a built-in one.
     *
     * @return Path
     * @since Ant 1.8.0
     */
    public Path createCompilerClasspath() {
        return facade.getImplementationClasspath(getProject());
    }

    /**
     * Set the compiler adapter explicitly.
     *
     * @param adapter CompilerAdapter
     * @since Ant 1.8.0
     */
    public void add(final CompilerAdapter adapter) {
        if (nestedAdapter != null) {
            throw new BuildException(
                "Can't have more than one compiler adapter");
        }
        nestedAdapter = adapter;
    }

    /**
     * Whether package-info.class files will be created by Ant
     * matching package-info.java files that have been compiled but
     * didn't create class files themselves.
     *
     * @param b boolean
     * @since Ant 1.8.3
     */
    public void setCreateMissingPackageInfoClass(final boolean b) {
        createMissingPackageInfoClass = b;
    }

    /**
     * Executes the task.
     * @exception BuildException if an error occurs
     */
    @Override
    public void execute() throws BuildException {
        checkParameters();
        resetFileLists();

        // scan source directories and dest directory to build up
        // compile list
        if (hasPath(src)) {
            collectFileListFromSourcePath();
        } else {
            assert hasPath(moduleSourcepath) : "Either srcDir or moduleSourcepath must be given";
            collectFileListFromModulePath();
        }

        compile();
        if (updatedProperty != null
            && taskSuccess
            && compileList.length != 0) {
            getProject().setNewProperty(updatedProperty, "true");
        }
    }

    /**
     * Clear the list of files to be compiled and copied..
     */
    protected void resetFileLists() {
        compileList = new File[0];
        packageInfos = new HashMap<>();
    }

    /**
     * Scans the directory looking for source files to be compiled.
     * The results are returned in the class variable compileList
     *
     * @param srcDir   The source directory
     * @param destDir  The destination directory
     * @param files    An array of filenames
     */
    protected void scanDir(final File srcDir, final File destDir, final String[] files) {
        final GlobPatternMapper m = new GlobPatternMapper();

        for (String extension : findSupportedFileExtensions()) {
            m.setFrom(extension);
            m.setTo("*.class");
            final SourceFileScanner sfs = new SourceFileScanner(this);
            final File[] newFiles = sfs.restrictAsFiles(files, srcDir, destDir, m);

            if (newFiles.length > 0) {
                lookForPackageInfos(srcDir, newFiles);
                final File[] newCompileList
                    = new File[compileList.length + newFiles.length];
                System.arraycopy(compileList, 0, newCompileList, 0,
                                 compileList.length);
                System.arraycopy(newFiles, 0, newCompileList,
                                 compileList.length, newFiles.length);
                compileList = newCompileList;
            }
        }
    }

    private void collectFileListFromSourcePath() {
        for (String filename : src.list()) {
            final File srcDir = getProject().resolveFile(filename);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir \""
                                         + srcDir.getPath()
                                         + "\" does not exist!", getLocation());
            }

            final DirectoryScanner ds = this.getDirectoryScanner(srcDir);

            scanDir(srcDir, destDir != null ? destDir : srcDir, ds.getIncludedFiles());
        }
    }

    private void collectFileListFromModulePath() {
        final FileUtils fu = FileUtils.getFileUtils();
        for (String pathElement : moduleSourcepath.list()) {
            boolean valid = false;
            for (Map.Entry<String, Collection<File>> modules : resolveModuleSourcePathElement(
                getProject().getBaseDir(), pathElement).entrySet()) {
                final String moduleName = modules.getKey();
                for (File srcDir : modules.getValue()) {
                    if (srcDir.exists()) {
                        valid = true;
                        final DirectoryScanner ds = getDirectoryScanner(srcDir);
                        final String[] files = ds.getIncludedFiles();
                        scanDir(srcDir, fu.resolveFile(destDir, moduleName), files);
                    }
                }
            }
            if (!valid) {
                throw new BuildException("modulesourcepath \""
                                         + pathElement
                                         + "\" does not exist!", getLocation());
            }
        }
    }

    private String[] findSupportedFileExtensions() {
        final String compilerImpl = getCompiler();
        final CompilerAdapter adapter =
            nestedAdapter != null ? nestedAdapter :
            CompilerAdapterFactory.getCompiler(compilerImpl, this,
                                               createCompilerClasspath());
        String[] extensions = null;
        if (adapter instanceof CompilerAdapterExtension) {
            extensions =
                ((CompilerAdapterExtension) adapter).getSupportedFileExtensions();
        }

        if (extensions == null) {
            extensions = new String[] {"java"};
        }

        // now process the extensions to ensure that they are the
        // right format
        for (int i = 0; i < extensions.length; i++) {
            if (!extensions[i].startsWith("*.")) {
                extensions[i] = "*." + extensions[i];
            }
        }
        return extensions;
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
     * "javac1.1", "javac1.2", "javac1.3", "javac1.4", "javac1.5",
     * "javac1.6", "javac1.7", "javac1.8", "javac1.9", "javac9" or "javac10+".
     */
    protected boolean isJdkCompiler(final String compilerImpl) {
        return CompilerAdapterFactory.isJdkCompiler(compilerImpl);
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
    public void setCompiler(final String compiler) {
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
                compilerImpl = CompilerAdapterFactory.COMPILER_EXTJAVAC;
            } else {
                log("Since compiler setting isn't classic or modern, ignoring fork setting.",
                    Project.MSG_WARN);
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
        if (hasPath(src)) {
            if (hasPath(moduleSourcepath)) {
                throw new BuildException("modulesourcepath cannot be combined with srcdir attribute!",
                    getLocation());
            }
        } else if (hasPath(moduleSourcepath)) {
            if (hasPath(src) || hasPath(compileSourcepath)) {
                throw new BuildException("modulesourcepath cannot be combined with srcdir or sourcepath !",
                    getLocation());
            }
            if (destDir == null) {
                throw new BuildException("modulesourcepath requires destdir attribute to be set!",
                                     getLocation());
            }
        } else {
            throw new BuildException("either srcdir or modulesourcepath attribute must be set!",
                    getLocation());
        }

        if (destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory \""
                                     + destDir
                                     + "\" does not exist or is not a directory", getLocation());
        }
        if (includeAntRuntime == null && getProject().getProperty(MagicNames.BUILD_SYSCLASSPATH) == null) {
            log(getLocation() + "warning: 'includeantruntime' was not set, defaulting to "
                            + MagicNames.BUILD_SYSCLASSPATH + "=last; set to false for repeatable builds",
                Project.MSG_WARN);
        }
    }

    /**
     * Perform the compilation.
     *
     * @since Ant 1.5
     */
    protected void compile() {
        final String compilerImpl = getCompiler();

        if (compileList.length > 0) {
            log("Compiling " + compileList.length + " source file"
                + (compileList.length == 1 ? "" : "s")
                + (destDir != null ? " to " + destDir : ""));

            if (listFiles) {
                for (File element : compileList) {
                  log(element.getAbsolutePath());
                }
            }

            final CompilerAdapter adapter =
                nestedAdapter != null ? nestedAdapter :
                CompilerAdapterFactory.getCompiler(compilerImpl, this,
                                                   createCompilerClasspath());

            // now we need to populate the compiler adapter
            adapter.setJavac(this);

            // finally, lets execute the compiler!!
            if (adapter.execute()) {
                // Success
                if (createMissingPackageInfoClass) {
                    try {
                        generateMissingPackageInfoClasses(destDir != null
                                                          ? destDir
                                                          : getProject()
                                                          .resolveFile(src.list()[0]));
                    } catch (final IOException x) {
                        // Should this be made a nonfatal warning?
                        throw new BuildException(x, getLocation());
                    }
                }
            } else {
                // Fail path
                this.taskSuccess = false;
                if (errorProperty != null) {
                    getProject().setNewProperty(
                        errorProperty, "true");
                }
                if (failOnError) {
                    throw new BuildException(FAIL_MSG, getLocation());
                }
                log(FAIL_MSG, Project.MSG_ERR);
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
        public void setCompiler(final String impl) {
            super.setImplementation(impl);
        }
    }

    private void lookForPackageInfos(final File srcDir, final File[] newFiles) {
        for (File f : newFiles) {
            if (!"package-info.java".equals(f.getName())) {
                continue;
            }
            final String path = FILE_UTILS.removeLeadingPath(srcDir, f)
                    .replace(File.separatorChar, '/');
            final String suffix = "/package-info.java";
            if (!path.endsWith(suffix)) {
                log("anomalous package-info.java path: " + path, Project.MSG_WARN);
                continue;
            }
            final String pkg = path.substring(0, path.length() - suffix.length());
            packageInfos.put(pkg, f.lastModified());
        }
    }

    /**
     * Ensure that every {@code package-info.java} produced a {@code package-info.class}.
     * Otherwise this task's up-to-date tracking mechanisms do not work.
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=43114">Bug #43114</a>
     */
    private void generateMissingPackageInfoClasses(final File dest) throws IOException {
        for (final Map.Entry<String, Long> entry : packageInfos.entrySet()) {
            final String pkg = entry.getKey();
            final Long sourceLastMod = entry.getValue();
            final File pkgBinDir = new File(dest, pkg.replace('/', File.separatorChar));
            pkgBinDir.mkdirs();
            final File pkgInfoClass = new File(pkgBinDir, "package-info.class");
            if (pkgInfoClass.isFile() && pkgInfoClass.lastModified() >= sourceLastMod) {
                continue;
            }
            log("Creating empty " + pkgInfoClass);
            try (OutputStream os = Files.newOutputStream(pkgInfoClass.toPath())) {
                os.write(PACKAGE_INFO_CLASS_HEADER);
                final byte[] name = pkg.getBytes(StandardCharsets.UTF_8);
                final int length = name.length + /* "/package-info" */ 13;
                os.write((byte) length / 256);
                os.write((byte) length % 256);
                os.write(name);
                os.write(PACKAGE_INFO_CLASS_FOOTER);
            }
        }
    }

    /**
     * Checks if a path exists and is non empty.
     * @param path to be checked
     * @return true if the path is non <code>null</code> and non empty.
     * @since 1.9.7
     */
    private static boolean hasPath(final Path path) {
        return path != null && !path.isEmpty();
    }

    /**
     * Resolves the modulesourcepath element possibly containing groups
     * and module marks to module names and source roots.
     * @param projectDir the project directory
     * @param element the modulesourcepath elemement
     * @return a mapping from module name to module source roots
     * @since 1.9.7
     */
    private static Map<String, Collection<File>> resolveModuleSourcePathElement(
            final File projectDir,
            final String element) {
        final Map<String, Collection<File>> result = new TreeMap<>();
        for (CharSequence resolvedElement : expandGroups(element)) {
            findModules(projectDir, resolvedElement.toString(), result);
        }
        return result;
    }

    /**
     * Expands the groups in the modulesourcepath entry to alternatives.
     * <p>
     * The <code>'*'</code> is a token representing the name of any of the modules in the compilation module set.
     * The <code>'{' ... ',' ... '}'</code> express alternates for expansion.
     * An example of the modulesourcepath entry is <code>src/&#42;/{linux,share}/classes</code>
     * </p>
     * @param element the entry to expand groups in
     * @return the possible alternatives
     * @since 1.9.7
     */
    private static Collection<? extends CharSequence> expandGroups(
            final CharSequence element) {
        List<StringBuilder> result = new ArrayList<>();
        result.add(new StringBuilder());
        StringBuilder resolved = new StringBuilder();
        for (int i = 0; i < element.length(); i++) {
            final char c = element.charAt(i);
            switch (c) {
                case GROUP_START_MARK:
                    final int end = getGroupEndIndex(element, i);
                    if (end < 0) {
                        throw new BuildException(String.format(
                                "Unclosed group %s, starting at: %d",
                                element,
                                i));
                    }
                    final Collection<? extends CharSequence> parts = resolveGroup(element.subSequence(i + 1, end));
                    switch (parts.size()) {
                        case 0:
                            break;
                        case 1:
                            resolved.append(parts.iterator().next());
                            break;
                        default:
                            final List<StringBuilder> oldRes = result;
                            result = new ArrayList<>(oldRes.size() * parts.size());
                            for (CharSequence part : parts) {
                                for (CharSequence prefix : oldRes) {
                                    result.add(new StringBuilder(prefix).append(resolved).append(part));
                                }
                            }
                            resolved = new StringBuilder();
                    }
                    i = end;
                    break;
                default:
                    resolved.append(c);
            }
        }
        for (StringBuilder prefix : result) {
            prefix.append(resolved);
        }
        return result;
    }

    /**
     * Resolves the group to alternatives.
     * @param group the group to resolve
     * @return the possible alternatives
     * @since 1.9.7
     */
    private static Collection<? extends CharSequence> resolveGroup(final CharSequence group) {
        final Collection<CharSequence> result = new ArrayList<>();
        int start = 0;
        int depth = 0;
        for (int i = 0; i < group.length(); i++) {
            final char c = group.charAt(i);
            switch (c) {
                case GROUP_START_MARK:
                    depth++;
                    break;
                case GROUP_END_MARK:
                    depth--;
                    break;
                case GROUP_SEP_MARK:
                    if (depth == 0) {
                        result.addAll(expandGroups(group.subSequence(start, i)));
                        start = i + 1;
                    }
                    break;
            }
        }
        result.addAll(expandGroups(group.subSequence(start, group.length())));
        return result;
    }

    /**
     * Finds the index of an enclosing brace of the group.
     * @param element the element to find the enclosing brace in
     * @param start the index of the opening brace.
     * @return return the index of an enclosing brace of the group or -1 if not found
     * @since 1.9.7
     */
    private static int getGroupEndIndex(
            final CharSequence element,
            final int start) {
        int depth = 0;
        for (int i = start; i < element.length(); i++) {
            final char c = element.charAt(i);
            switch (c) {
                case GROUP_START_MARK:
                    depth++;
                    break;
                case GROUP_END_MARK:
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                    break;
            }
        }
        return -1;
    }

    /**
     * Finds modules in the expanded modulesourcepath entry.
     * @param root the project root
     * @param pattern the expanded modulesourcepath entry
     * @param collector the map to put modules into
     * @since 1.9.7
     */
    private static void findModules(
            final File root,
            String pattern,
            final Map<String, Collection<File>> collector) {
        pattern = pattern
                .replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);
        final int startIndex = pattern.indexOf(MODULE_MARKER);
        if (startIndex == -1) {
            findModules(root, pattern, null, collector);
            return;
        }
        if (startIndex == 0) {
            throw new BuildException("The modulesourcepath entry must be a folder.");
        }
        final int endIndex = startIndex + MODULE_MARKER.length();
        if (pattern.charAt(startIndex - 1) != File.separatorChar) {
                throw new BuildException("The module mark must be preceded by separator");
        }
        if (endIndex < pattern.length() && pattern.charAt(endIndex) != File.separatorChar) {
            throw new BuildException("The module mark must be followed by separator");
        }
        if (pattern.indexOf(MODULE_MARKER, endIndex) != -1) {
            throw new BuildException("The modulesourcepath entry must contain at most one module mark");
        }
        final String pathToModule = pattern.substring(0, startIndex);
        final String pathInModule = endIndex == pattern.length()
                ? null : pattern.substring(endIndex + 1);  //+1 the separator
        findModules(root, pathToModule, pathInModule, collector);
    }

    /**
     * Finds modules in the expanded modulesourcepath entry.
     * @param root the project root
     * @param pathToModule the path to modules folder
     * @param pathInModule the path in module to source folder
     * @param collector the map to put modules into
     * @since 1.9.7
     */
    private static void findModules(
        final File root,
        final String pathToModule,
        final String pathInModule,
        final Map<String,Collection<File>> collector) {
        final File f = FileUtils.getFileUtils().resolveFile(root, pathToModule);
        if (!f.isDirectory()) {
            return;
        }
        for (File module : f.listFiles(File::isDirectory)) {
            final String moduleName = module.getName();
            final File moduleSourceRoot = pathInModule == null
                    ? module : new File(module, pathInModule);
            Collection<File> moduleRoots = collector.computeIfAbsent(moduleName, k -> new ArrayList<>());
            moduleRoots.add(moduleSourceRoot);
        }
    }

    private static final byte[] PACKAGE_INFO_CLASS_HEADER = {
        (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0x00, 0x00, 0x00,
        0x31, 0x00, 0x07, 0x07, 0x00, 0x05, 0x07, 0x00, 0x06, 0x01, 0x00, 0x0a,
        0x53, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x46, 0x69, 0x6c, 0x65, 0x01, 0x00,
        0x11, 0x70, 0x61, 0x63, 0x6b, 0x61, 0x67, 0x65, 0x2d, 0x69, 0x6e, 0x66,
        0x6f, 0x2e, 0x6a, 0x61, 0x76, 0x61, 0x01
    };

    private static final byte[] PACKAGE_INFO_CLASS_FOOTER = {
        0x2f, 0x70, 0x61, 0x63, 0x6b, 0x61, 0x67, 0x65, 0x2d, 0x69, 0x6e, 0x66,
        0x6f, 0x01, 0x00, 0x10, 0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c, 0x61, 0x6e,
        0x67, 0x2f, 0x4f, 0x62, 0x6a, 0x65, 0x63, 0x74, 0x02, 0x00, 0x00, 0x01,
        0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x03,
        0x00, 0x00, 0x00, 0x02, 0x00, 0x04
    };

}
