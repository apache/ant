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
import java.io.IOException;
import java.rmi.Remote;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapter;
import org.apache.tools.ant.taskdefs.rmic.RmicAdapterFactory;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;

/**
 * Runs the rmic compiler against classes.</p>
 * <p>Rmic can be run on a single class (as specified with the classname
 * attribute) or a number of classes at once (all classes below base that
 * are neither _Stub nor _Skel classes).  If you want to rmic a single
 * class and this class is a class nested into another class, you have to
 * specify the classname in the form <code>Outer$$Inner</code> instead of
 * <code>Outer.Inner</code>.</p>
 * <p>It is possible to refine the set of files that are being rmiced. This can
 * be done with the <i>includes</i>, <i>includesfile</i>, <i>excludes</i>,
 * <i>excludesfile</i> and <i>defaultexcludes</i>
 * attributes. With the <i>includes</i> or <i>includesfile</i> attribute you
 * specify the files you want to have included by using patterns. The
 * <i>exclude</i> or <i>excludesfile</i> attribute is used to specify
 * the files you want to have excluded. This is also done with patterns. And
 * finally with the <i>defaultexcludes</i> attribute, you can specify whether
 * you want to use default exclusions or not. See the section on
 * directory based tasks</a>, on how the
 * inclusion/exclusion of files works, and how to write patterns.</p>
 * <p>This task forms an implicit FileSet and
 * supports all attributes of <code>&lt;fileset&gt;</code>
 * (<code>dir</code> becomes <code>base</code>) as well as the nested
 * <code>&lt;include&gt;</code>, <code>&lt;exclude&gt;</code> and
 * <code>&lt;patternset&gt;</code> elements.</p>
 * <p>It is possible to use different compilers. This can be selected
 * with the &quot;build.rmic&quot; property or the <code>compiler</code>
 * attribute. <a name="compilervalues">There are three choices</a>:</p>
 * <ul>
 *   <li>sun (the standard compiler of the JDK)</li>
 *   <li>kaffe (the standard compiler of
 *       {@link <a href="http://www.kaffe.org">Kaffe</a>})</li>
 *   <li>weblogic</li>
 * </ul>
 *
 * <p> The <a href="http://dione.zcu.cz/~toman40/miniRMI/">miniRMI</a>
 * project contains a compiler implementation for this task as well,
 * please consult miniRMI's documentation to learn how to use it.</p>
 *
 * @since Ant 1.1
 *
 * @ant.task category="java"
 */

public class Rmic extends MatchingTask {

    /** rmic failed message */
    public static final String ERROR_RMIC_FAILED
        = "Rmic failed; see the compiler error output for details.";

    private File baseDir;
    private String classname;
    private File sourceBase;
    private String stubVersion;
    private Path compileClasspath;
    private Path extDirs;
    private boolean verify = false;
    private boolean filtering = false;

    private boolean iiop = false;
    private String  iiopOpts;
    private boolean idl  = false;
    private String  idlOpts;
    private boolean debug  = false;
    private boolean includeAntRuntime = true;
    private boolean includeJavaRuntime = false;

    private Vector compileList = new Vector();

    private ClassLoader loader = null;

    private FacadeTaskHelper facade;
    /** unable to verify message */
    public static final String ERROR_UNABLE_TO_VERIFY_CLASS = "Unable to verify class ";
    /** could not be found message */
    public static final String ERROR_NOT_FOUND = ". It could not be found.";
    /** not defined message */
    public static final String ERROR_NOT_DEFINED = ". It is not defined.";
    /** loaded error message */
    public static final String ERROR_LOADING_CAUSED_EXCEPTION = ". Loading caused Exception: ";
    /** base not exists message */
    public static final String ERROR_NO_BASE_EXISTS = "base does not exist: ";
    /** base not a directory message */
    public static final String ERROR_NOT_A_DIR = "base is not a directory:";
    /** base attribute not set message */
    public static final String ERROR_BASE_NOT_SET = "base attribute must be set!";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Constructor for Rmic.
     */
    public Rmic() {
        facade = new FacadeTaskHelper(RmicAdapterFactory.DEFAULT_COMPILER);
    }

    /**
     * Sets the location to store the compiled files; required
     * @param base the location to store the compiled files
     */
    public void setBase(File base) {
        this.baseDir = base;
    }

    /**
     * Gets the base directory to output generated class.
     * @return the location of the compiled files
     */

    public File getBase() {
        return this.baseDir;
    }

    /**
     * Sets the class to run <code>rmic</code> against;
     * optional
     * @param classname the name of the class for rmic to create code for
     */
    public void setClassname(String classname) {
        this.classname = classname;
    }

    /**
     * Gets the class name to compile.
     * @return the name of the class to compile
     */
    public String getClassname() {
        return classname;
    }

    /**
     * optional directory to save generated source files to.
     * @param sourceBase the directory to save source files to.
     */
    public void setSourceBase(File sourceBase) {
        this.sourceBase = sourceBase;
    }

    /**
     * Gets the source dirs to find the source java files.
     * @return sourceBase the directory containing the source files.
     */
    public File getSourceBase() {
        return sourceBase;
    }

    /**
     * Specify the JDK version for the generated stub code.
     * Specify &quot;1.1&quot; to pass the &quot;-v1.1&quot; option to rmic.</td>
     * @param stubVersion the JDK version
     */
    public void setStubVersion(String stubVersion) {
        this.stubVersion = stubVersion;
    }

    /**
     * Gets the JDK version for the generated stub code.
     * @return stubVersion
     */
    public String getStubVersion() {
        return stubVersion;
    }

    /**
     * Sets token filtering [optional], default=false
     * @param filter turn on token filtering
     */
    public void setFiltering(boolean filter) {
        this.filtering = filter;
    }

    /**
     * Gets whether token filtering is set
     * @return filtering
     */
    public boolean getFiltering() {
        return filtering;
    }

    /**
     * Generate debug info (passes -g to rmic);
     * optional, defaults to false
     * @param debug turn on debug info
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Gets the debug flag.
     * @return debug
     */
    public boolean getDebug() {
        return debug;
    }

    /**
     * Set the classpath to be used for this compilation.
     * @param classpath the classpath used for this compilation
     */
    public void setClasspath(Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }

    /**
     * Creates a nested classpath element.
     * @return classpath
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds to the classpath a reference to
     * a &lt;path&gt; defined elsewhere.
     * @param pathRef the reference to add to the classpath
     */
    public void setClasspathRef(Reference pathRef) {
        createClasspath().setRefid(pathRef);
    }

    /**
     * Gets the classpath.
     * @return the classpath
     */
    public Path getClasspath() {
        return compileClasspath;
    }

    /**
     * Flag to enable verification so that the classes
     * found by the directory match are
     * checked to see if they implement java.rmi.Remote.
     * optional; This defaults to false if not set.
     * @param verify turn on verification for classes
     */

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    /**
     * Get verify flag.
     * @return verify
     */
    public boolean getVerify() {
        return verify;
    }

    /**
     * Indicates that IIOP compatible stubs should
     * be generated; optional, defaults to false
     * if not set.
     * @param iiop generate IIOP compatible stubs
     */
    public void setIiop(boolean iiop) {
        this.iiop = iiop;
    }

    /**
     * Gets iiop flags.
     * @return iiop
     */
    public boolean getIiop() {
        return iiop;
    }

    /**
     * Set additional arguments for iiop
     * @param iiopOpts additional arguments for iiop
     */
    public void setIiopopts(String iiopOpts) {
        this.iiopOpts = iiopOpts;
    }

    /**
     * Gets additional arguments for iiop.
     * @return iiopOpts
     */
    public String getIiopopts() {
        return iiopOpts;
    }

    /**
     * Indicates that IDL output should be
     * generated.  This defaults to false
     * if not set.
     * @param idl generate IDL output
     */
    public void setIdl(boolean idl) {
        this.idl = idl;
    }

    /**
     * Gets IDL flags.
     * @return the idl flag
     */
    public boolean getIdl() {
        return idl;
    }

    /**
     * pass additional arguments for IDL compile
     * @param idlOpts additional IDL arguments
     */
    public void setIdlopts(String idlOpts) {
        this.idlOpts = idlOpts;
    }

    /**
     * Gets additional arguments for idl compile.
     * @return the idl options
     */
    public String getIdlopts() {
        return idlOpts;
    }

    /**
     * Gets file list to compile.
     * @return the list of files to compile.
     */
    public Vector getFileList() {
        return compileList;
    }

    /**
     * Sets whether or not to include ant's own classpath in this task's
     * classpath.
     * Optional; default is <code>true</code>.
     * @param include if true include ant's classpath
     */
    public void setIncludeantruntime(boolean include) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the
     * task's classpath.
     * @return true if ant's classpath is to be included
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime;
    }

    /**
     * task's classpath.
     * Enables or disables including the default run-time
     * libraries from the executing VM; optional,
     * defaults to false
     * @param include if true include default run-time libraries
     */
    public void setIncludejavaruntime(boolean include) {
        includeJavaRuntime = include;
    }

    /**
     * Gets whether or not the java runtime should be included in this
     * task's classpath.
     * @return true if default run-time libraries are included
     */
    public boolean getIncludejavaruntime() {
        return includeJavaRuntime;
    }

    /**
     * Sets the extension directories that will be used during the
     * compilation; optional.
     * @param extDirs the extension directories to be used
     */
    public void setExtdirs(Path extDirs) {
        if (this.extDirs == null) {
            this.extDirs = extDirs;
        } else {
            this.extDirs.append(extDirs);
        }
    }

    /**
     * Maybe creates a nested extdirs element.
     * @return path object to be configured with the extension directories
     */
    public Path createExtdirs() {
        if (extDirs == null) {
            extDirs = new Path(getProject());
        }
        return extDirs.createPath();
    }

    /**
     * Gets the extension directories that will be used during the
     * compilation.
     * @return the extension directories to be used
     */
    public Path getExtdirs() {
        return extDirs;
    }

    /**
     * @return the compile list.
     */
    public Vector getCompileList() {
        return compileList;
    }

    /**
     * Sets the compiler implementation to use; optional,
     * defaults to the value of the <code>build.rmic</code> property,
     * or failing that, default compiler for the current VM
     * @param compiler the compiler implemention to use
     * @since Ant 1.5
     */
    public void setCompiler(String compiler) {
        if (compiler.length() > 0) {
            facade.setImplementation(compiler);
        }
    }

    /**
     * get the name of the current compiler
     * @return the name of the compiler
     * @since Ant 1.5
     */
    public String getCompiler() {
        facade.setMagicValue(getProject().getProperty("build.rmic"));
        return facade.getImplementation();
    }

    /**
     * Adds an implementation specific command line argument.
     * @return an object to be configured with a command line argument
     * @since Ant 1.5
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
     * @since Ant 1.5
     */
    public String[] getCurrentCompilerArgs() {
        getCompiler();
        return facade.getArgs();
    }

    /**
     * execute by creating an instance of an implementation
     * class and getting to do the work
     * @throws org.apache.tools.ant.BuildException
     * if there's a problem with baseDir or RMIC
     */
    public void execute() throws BuildException {
        if (baseDir == null) {
            throw new BuildException(ERROR_BASE_NOT_SET, getLocation());
        }
        if (!baseDir.exists()) {
            throw new BuildException(ERROR_NO_BASE_EXISTS + baseDir, getLocation());
        }
        if (!baseDir.isDirectory()) {
            throw new BuildException(ERROR_NOT_A_DIR + baseDir, getLocation());
        }
        if (verify) {
            log("Verify has been turned on.", Project.MSG_VERBOSE);
        }

        RmicAdapter adapter = RmicAdapterFactory.getRmic(getCompiler(), this);

        // now we need to populate the compiler adapter
        adapter.setRmic(this);

        Path classpath = adapter.getClasspath();
        loader = getProject().createClassLoader(classpath);

        try {
            // scan base dirs to build up compile lists only if a
            // specific classname is not given
            if (classname == null) {
                DirectoryScanner ds = this.getDirectoryScanner(baseDir);
                String[] files = ds.getIncludedFiles();
                scanDir(baseDir, files, adapter.getMapper());
            } else {
                // otherwise perform a timestamp comparison - at least
                String path = classname.replace('.', File.separatorChar) + ".class";
                File f = new File(baseDir, path);
                if (f.isFile()) {
                    scanDir(baseDir, new String[] {path}, adapter.getMapper());
                } else {
                    // Does not exist, so checking whether it is up to date makes no sense.
                    // Compilation will fail later anyway, but tests expect a certain output.
                    compileList.add(classname);
                }
            }

            int fileCount = compileList.size();
            if (fileCount > 0) {
                log("RMI Compiling " + fileCount
                    + " class" + (fileCount > 1 ? "es" : "") + " to " + baseDir,
                    Project.MSG_INFO);

                // finally, lets execute the compiler!!
                if (!adapter.execute()) {
                    throw new BuildException(ERROR_RMIC_FAILED, getLocation());
                }
            }

            /*
             * Move the generated source file to the base directory.  If
             * base directory and sourcebase are the same, the generated
             * sources are already in place.
             */
            if (null != sourceBase && !baseDir.equals(sourceBase)
                && fileCount > 0) {
                if (idl) {
                    log("Cannot determine sourcefiles in idl mode, ",
                        Project.MSG_WARN);
                    log("sourcebase attribute will be ignored.",
                        Project.MSG_WARN);
                } else {
                    for (int j = 0; j < fileCount; j++) {
                        moveGeneratedFile(baseDir, sourceBase,
                                          (String) compileList.elementAt(j),
                                          adapter);
                    }
                }
            }
        } finally {
            compileList.removeAllElements();
        }
    }

    /**
     * Move the generated source file(s) to the base directory
     *
     * @throws org.apache.tools.ant.BuildException When error
     * copying/removing files.
     */
    private void moveGeneratedFile (File baseDir, File sourceBaseFile,
                                    String classname,
                                    RmicAdapter adapter)
        throws BuildException {

        String classFileName =
            classname.replace('.', File.separatorChar) + ".class";
        String[] generatedFiles =
            adapter.getMapper().mapFileName(classFileName);

        for (int i = 0; i < generatedFiles.length; i++) {
            final String generatedFile = generatedFiles[i];
            if (!generatedFile.endsWith(".class")) {
                // don't know how to handle that - a IDL file doesn't
                // have a corresponding Java source for example.
                continue;
            }

            final int pos = generatedFile.length() - ".class".length();
            String sourceFileName =
                generatedFile.substring(0, pos) + ".java";

            File oldFile = new File(baseDir, sourceFileName);
            if (!oldFile.exists()) {
                // no source file generated, nothing to move
                continue;
            }

            File newFile = new File(sourceBaseFile, sourceFileName);
            try {
                if (filtering) {
                    FILE_UTILS.copyFile(oldFile, newFile,
                        new FilterSetCollection(getProject()
                                                .getGlobalFilterSet()));
                } else {
                    FILE_UTILS.copyFile(oldFile, newFile);
                }
                oldFile.delete();
            } catch (IOException ioe) {
                String msg = "Failed to copy " + oldFile + " to "
                    + newFile + " due to " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            }
        }
    }

    /**
     * Scans the directory looking for class files to be compiled.
     * The result is returned in the class variable compileList.
     * @param baseDir the base direction
     * @param files   the list of files to scan
     * @param mapper  the mapper of files to target files
     */
    protected void scanDir(File baseDir, String[] files,
                           FileNameMapper mapper) {

        String[] newFiles = files;
        if (idl) {
            log("will leave uptodate test to rmic implementation in idl mode.",
                Project.MSG_VERBOSE);
        } else if (iiop
                   && iiopOpts != null && iiopOpts.indexOf("-always") > -1) {
            log("no uptodate test as -always option has been specified",
                Project.MSG_VERBOSE);
        } else {
            SourceFileScanner sfs = new SourceFileScanner(this);
            newFiles = sfs.restrict(files, baseDir, baseDir, mapper);
        }

        for (int i = 0; i < newFiles.length; i++) {
            String name = newFiles[i].replace(File.separatorChar, '.');
            name = name.substring(0, name.lastIndexOf(".class"));
            compileList.addElement(name);
        }
    }

    /**
     * Load named class and test whether it can be rmic'ed
     * @param classname the name of the class to be tested
     * @return true if the class can be rmic'ed
     */
    public boolean isValidRmiRemote(String classname) {
        try {
            Class testClass = loader.loadClass(classname);
            // One cannot RMIC an interface for "classic" RMI (JRMP)
            if (testClass.isInterface() && !iiop && !idl) {
                return false;
            }
            return isValidRmiRemote(testClass);
        } catch (ClassNotFoundException e) {
            log(ERROR_UNABLE_TO_VERIFY_CLASS + classname
                + ERROR_NOT_FOUND, Project.MSG_WARN);
        } catch (NoClassDefFoundError e) {
            log(ERROR_UNABLE_TO_VERIFY_CLASS + classname
                + ERROR_NOT_DEFINED, Project.MSG_WARN);
        } catch (Throwable t) {
            log(ERROR_UNABLE_TO_VERIFY_CLASS + classname
                + ERROR_LOADING_CAUSED_EXCEPTION
                + t.getMessage(), Project.MSG_WARN);
        }
        // we only get here if an exception has been thrown
        return false;
    }

    /**
     * Returns the topmost interface that extends Remote for a given
     * class - if one exists.
     * @param testClass the class to be tested
     * @return the topmost interface that extends Remote, or null if there
     *         is none.
     */
    public Class getRemoteInterface(Class testClass) {
        if (Remote.class.isAssignableFrom(testClass)) {
            Class [] interfaces = testClass.getInterfaces();
            if (interfaces != null) {
                for (int i = 0; i < interfaces.length; i++) {
                    if (Remote.class.isAssignableFrom(interfaces[i])) {
                        return interfaces[i];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Check to see if the class or (super)interfaces implement
     * java.rmi.Remote.
     */
    private boolean isValidRmiRemote (Class testClass) {
        return getRemoteInterface(testClass) != null;
    }

    /**
     * Classloader for the user-specified classpath.
     * @return the classloader
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Adds an "compiler" attribute to Commandline$Attribute used to
     * filter command line attributes based on the current
     * implementation.
     */
    public class ImplementationSpecificArgument extends
        org.apache.tools.ant.util.facade.ImplementationSpecificArgument {

        /**
         * Only pass the specified argument if the
         * chosen compiler implementation matches the
         * value of this attribute. Legal values are
         * the same as those in the above list of
         * valid compilers.)
         * @param impl the compiler to be used.
         */
        public void setCompiler(String impl) {
            super.setImplementation(impl);
        }
    }

}

