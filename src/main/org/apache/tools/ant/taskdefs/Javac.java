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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;

import java.io.File;

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
 * <li>vebose
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
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green 
 *         <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 *
 * @version $Revision$
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */

public class Javac extends MatchingTask {

    private static final String FAIL_MSG
        = "Compile failed; see the compiler error output for details.";

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
    private String target;
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

    protected boolean failOnError = true;
    protected boolean listFiles = false;
    protected File[] compileList = new File[0];

    private String source;
    private String debugLevel;

    /**
     * Javac task for compilation of Java files.
     */
    public Javac() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            facade = new FacadeTaskHelper("javac1.1");
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)) {
            facade = new FacadeTaskHelper("javac1.2");
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) {
            facade = new FacadeTaskHelper("javac1.3");
        } else if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4)) {
            facade = new FacadeTaskHelper("javac1.4");
        } else {
            facade = new FacadeTaskHelper("classic");
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
        return source;
    }

    /**
     * Value of the -source command-line switch; will be ignored
     * by all implementations except modern and jikes.
     *
     * If you use this attribute together with jikes, you must
     * make sure that your version of jikes supports the -source switch.
     * Legal values are 1.3 and 1.4 – by default, no -source argument
     * will be used at all.
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
            src = new Path(project);
        }
        return src.createPath();
    }

    /**
     * Recreate src
     *
     * @return a nested src element.
     */
    protected Path recreateSrc() {
        src = null;
        return createSrc();
    }

    /**
     * Set the source directories to find the source Java files.
     */
    public void setSrcdir(Path srcDir) {
        if (src == null) {
            src = srcDir;
        } else {
            src.append(srcDir);
        }
    }

    /** Gets the source dirs to find the source java files. */
    public Path getSrcdir() {
        return src;
    }

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Gets the destination directory into which the java source files
     * should be compiled.
     */
    public File getDestdir() {
        return destDir;
    }

    /**
     * Set the sourcepath to be used for this compilation.
     */
    public void setSourcepath(Path sourcepath) {
        if (compileSourcepath == null) {
            compileSourcepath = sourcepath;
        } else {
            compileSourcepath.append(sourcepath);
        }
    }

    /** Gets the sourcepath to be used for this compilation. */
    public Path getSourcepath() {
        return compileSourcepath;
    }

    /**
     * Adds a path to sourcepath.
     */
    public Path createSourcepath() {
        if (compileSourcepath == null) {
            compileSourcepath = new Path(project);
        }
        return compileSourcepath.createPath();
    }

    /**
     * Adds a reference to a source path defined elsewhere.
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

    /** Gets the classpath to be used for this compilation. */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Adds a path to the classpath.
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(project);
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     */
    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    /**
     * Sets the bootclasspath that will be used to compile the classes
     * against.
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
     */
    public Path getBootclasspath() {
        return bootclasspath;
    }

    /**
     * Adds a path to the bootclasspath.
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(project);
        }
        return bootclasspath.createPath();
    }

    /**
     * Adds a reference to a classpath defined elsewhere.
     */
    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    /**
     * Sets the extension directories that will be used during the
     * compilation.
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
     */
    public Path getExtdirs() {
        return extdirs;
    }

    /**
     * Adds a path to extdirs.
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path(project);
        }
        return extdirs.createPath();
    }

    /**
     * If true, list the source files being handed off to the compiler.
     */
    public void setListfiles(boolean list) {
        listFiles = list;
    }

    /** Get the listfiles flag. */
    public boolean getListfiles() {
        return listFiles;
    }

    /**
     * Indicates whether the build will continue
     * even if there are compilation errors; defaults to true.
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * Proceed if compilation fails, but use
     * failonerror instead.
     */
    public void setProceed(boolean proceed) {
        failOnError = !proceed;
    }

    /**
     * Gets the failonerror flag.
     */
    public boolean getFailonerror() {
        return failOnError;
    }

    /**
     * Indicates whether source should be
     * compiled with deprecation information; defaults to off.
     */
    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

    /** Gets the deprecation flag. */
    public boolean getDeprecation() {
        return deprecation;
    }

    /**
     * The initial size of the memory for the underlying VM
     * if javac is run externally; ignored otherwise.
     * Defaults to the standard VM memory setting.
     * (Examples: 83886080, 81920k, or 80m)
     */
    public void setMemoryInitialSize(String memoryInitialSize) {
        this.memoryInitialSize = memoryInitialSize;
    }

    /** Gets the memoryInitialSize flag. */
    public String getMemoryInitialSize() {
        return memoryInitialSize;
    }

    /**
     * The maximum size of the memory for the underlying VM
     * if javac is run externally; ignored otherwise.
     * Defaults to the standard VM memory setting.
     * (Examples: 83886080, 81920k, or 80m)
     */
    public void setMemoryMaximumSize(String memoryMaximumSize) {
        this.memoryMaximumSize = memoryMaximumSize;
    }

    /** Gets the memoryMaximumSize flag. */
    public String getMemoryMaximumSize() {
        return memoryMaximumSize;
    }

    /**
     * Set the Java source file encoding name.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /** Gets the java source file encoding name. */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Indicates whether source should be compiled
     * with debug information; defaults to off.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Gets the debug flag. */
    public boolean getDebug() {
        return debug;
    }

    /**
     * If true, compiles with optimization enabled.
     */
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    /** Gets the optimize flag. */
    public boolean getOptimize() {
        return optimize;
    }

    /**
     * Enables dependency-tracking for compilers
     * that support this (jikes and classic).
     */
    public void setDepend(boolean depend) {
        this.depend = depend;
    }

    /** Gets the depend flag. */
    public boolean getDepend() {
        return depend;
    }

    /**
     * If true, asks the compiler for verbose output.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /** Gets the verbose flag. */
    public boolean getVerbose() {
        return verbose;
    }

    /**
     * Sets the target VM that the classes will be compiled for. Valid
     * strings are "1.1", "1.2", and "1.3".
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /** Gets the target VM that the classes will be compiled for. */
    public String getTarget() {
        return target;
    }

    /**
     * If true, includes Ant's own classpath in the classpath.
     */
    public void setIncludeantruntime(boolean include) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the classpath.
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime;
    }

    /**
     * If true, includes the Java runtime libraries in the classpath.
     */
    public void setIncludejavaruntime(boolean include) {
        includeJavaRuntime = include;
    }

    /**
     * Gets whether or not the java runtime should be included in this
     * task's classpath.
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
     * Sets the the name of the javac executable.
     *
     * <p>Ignored unless fork is true or extJavac has been specified
     * as the compiler.</p>
     */
    public void setExecutable(String forkExec) {
        forkedExecutable = forkExec;
    }

    /**
     * Is this a forked invocation of JDK's javac?
     */
    public boolean isForkedJavac() {
        return fork || "extJavac".equals(getCompiler());
    }

    /**
     * The name of the javac executable to use in fork-mode.
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
     */
    public void setNowarn(boolean flag) {
        this.nowarn = flag;
    }

    /**
     * Should the -nowarn option be used.
     */
    public boolean getNowarn() {
        return nowarn;
    }

    /**
     * Adds an implementation specific command-line argument.
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
        // make sure facade knows about magic properties and fork setting
        facade.setImplementation(getCompiler());
        try {
            return facade.getArgs();
        } finally {
            facade.setImplementation(chosen);
        }
    }
        

    /**
     * Executes the task.
     */
    public void execute() throws BuildException {
        checkParameters();
        resetFileLists();

        // scan source directories and dest directory to build up
        // compile lists
        String[] list = src.list();
        for (int i = 0; i < list.length; i++) {
            File srcDir = project.resolveFile(list[i]);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir \"" 
                                         + srcDir.getPath() 
                                         + "\" does not exist!", location);
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
     */
    protected void scanDir(File srcDir, File destDir, String[] files) {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("*.java");
        m.setTo("*.class");
        SourceFileScanner sfs = new SourceFileScanner(this);
        File[] newFiles = sfs.restrictAsFiles(files, srcDir, destDir, m);

        if (newFiles.length > 0) {
            File[] newCompileList = new File[compileList.length +
                newFiles.length];
            System.arraycopy(compileList, 0, newCompileList, 0,
                    compileList.length);
            System.arraycopy(newFiles, 0, newCompileList,
                    compileList.length, newFiles.length);
            compileList = newCompileList;
        }
    }

    /** Gets the list of files to be compiled. */
    public File[] getFileList() {
        return compileList;
    }

    protected boolean isJdkCompiler(String compilerImpl) {
        return "modern".equals(compilerImpl) ||
            "classic".equals(compilerImpl) ||
            "javac1.1".equals(compilerImpl) ||
            "javac1.2".equals(compilerImpl) ||
            "javac1.3".equals(compilerImpl) ||
            "javac1.4".equals(compilerImpl);
    }

    protected String getSystemJavac() {
        return JavaEnvUtils.getJdkExecutable("javac");
    }

    /**
     * Choose the implementation for this particular task.
     *
     * @since Ant 1.5
     */
    public void setCompiler(String compiler) {
        facade.setImplementation(compiler);
    }

    /**
     * The implementation for this particular task.
     *
     * <p>Defaults to the build.compiler property but can be overriden
     * via the compiler and fork attributes.</p>
     *
     * <p>If fork has been set to true, the result will be extJavac
     * and not classic or java1.2 - no matter what the compiler
     * attribute looks like.</p>
     * 
     * @see #getCompilerVersion
     * 
     * @since Ant 1.5
     */
    public String getCompiler() {
        String compilerImpl = getCompilerVersion();
        if (fork) {
            if (isJdkCompiler(compilerImpl)) {
                log("Since fork is true, ignoring compiler setting.",
                    Project.MSG_WARN);
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
     * <p>Defaults to the build.compiler property but can be overriden
     * via the compiler attribute.</p>
     *
     * <p>This method does not take the fork attribute into
     * account.</p>
     * 
     * @see #getCompiler
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
     */
    protected void checkParameters() throws BuildException {
        if (src == null) {
            throw new BuildException("srcdir attribute must be set!", 
                                     location);
        }
        if (src.size() == 0) {
            throw new BuildException("srcdir attribute must be set!", 
                                     location);
        }

        if (destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory \"" 
                                     + destDir 
                                     + "\" does not exist "
                                     + "or is not a directory", location);
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
            log("Compiling " + compileList.length +
                " source file"
                + (compileList.length == 1 ? "" : "s")
                + (destDir != null ? " to " + destDir : ""));

            if (listFiles) {
                for (int i = 0 ; i < compileList.length ; i++) {
                  String filename = compileList[i].getAbsolutePath();
                  log(filename) ;
                }
            }

            CompilerAdapter adapter = 
                CompilerAdapterFactory.getCompiler(compilerImpl, this);

            // now we need to populate the compiler adapter
            adapter.setJavac(this);

            // finally, lets execute the compiler!!
            if (!adapter.execute()) {
                if (failOnError) {
                    throw new BuildException(FAIL_MSG, location);
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

        public void setCompiler(String impl) {
            super.setImplementation(impl);
        }
    }

}
