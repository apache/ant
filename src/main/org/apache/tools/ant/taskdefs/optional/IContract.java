/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Instruments Java classes with iContract DBC preprocessor.
 * <br/>
 * The task can generate a properties file for <a href="http://hjem.sol.no/hellesoy/icontrol.html">iControl</a>,
 * a graphical user interface that lets you turn on/off assertions. iControl generates a control file that you can refer to
 * from this task using the controlfile attribute.
 * iContract is at <a href="http://www.reliable-systems.com/tools/">http://www.reliable-systems.com/tools/</a>
 * <p/>
 * Thanks to Rainer Schmitz for enhancements and comments.
 *
 * @author <a href="mailto:aslak.hellesoy@bekk.no">Aslak Helles�a>
 *
 * <p/>
 * <table border="1" cellpadding="2" cellspacing="0">
 *   <tr>
 *     <td valign="top"><b>Attribute</b></td>
 *     <td valign="top"><b>Description</b></td>
 *     <td align="center" valign="top"><b>Required</b></td>
 *   </tr>
 *   <tr>
 *     <td valign="top">srcdir</td>
 *     <td valign="top">Location of the java files.</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">instrumentdir</td>
 *     <td valign="top">Indicates where the instrumented source files should go.</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">repositorydir</td>
 *     <td valign="top">Indicates where the repository source files should go.</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">builddir</td>
 *     <td valign="top">Indicates where the compiled instrumented classes should go.
 *       Defaults to the value of instrumentdir.
 *       </p>
 *       <em>NOTE:</em> Don't use the same directory for compiled instrumented classes
 *       and uninstrumented classes. It will break the dependency checking. (Classes will
 *       not be reinstrumented if you change them).</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">repbuilddir</td>
 *     <td valign="top">Indicates where the compiled repository classes should go.
 *       Defaults to the value of repositorydir.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">pre</td>
 *     <td valign="top">Indicates whether or not to instrument for preconditions.
 *       Defaults to <code>true</code> unless controlfile is specified, in which case it
 *       defaults to <code>false</code>.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">post</td>
 *     <td valign="top">Indicates whether or not to instrument for postconditions.
 *       Defaults to <code>true</code> unless controlfile is specified, in which case it
 *       defaults to <code>false</code>.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">invariant</td>
 *     <td valign="top">Indicates whether or not to instrument for invariants.
 *       Defaults to <code>true</code> unless controlfile is specified, in which case it
 *       defaults to <code>false</code>.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">failthrowable</td>
 *     <td valign="top">The full name of the Throwable (Exception) that should be
 *       thrown when an assertion is violated. Defaults to <code>java.lang.Error</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">verbosity</td>
 *     <td valign="top">Indicates the verbosity level of iContract. Any combination
 *       of <code>error*,warning*,note*,info*,progress*,debug*</code> (comma separated) can be
 *       used. Defaults to <code>error*</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">quiet</td>
 *     <td valign="top">Indicates if iContract should be quiet. Turn it off if many your classes extend uninstrumented classes
 *     and you don't want warnings about this. Defaults to <code>false</code></td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">updateicontrol</td>
 *     <td valign="top">If set to true, it indicates that the properties file for
 *       iControl in the current directory should be updated (or created if it doesn't exist).
 *       Defaults to <code>false</code>.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">controlfile</td>
 *     <td valign="top">The name of the control file to pass to iContract. Consider using iControl to generate the file.
 *       Default is not to pass a file. </td>
 *     <td valign="top" align="center">Only if <code>updateicontrol=true</code></td>
 *   </tr>
 *   <tr>
 *     <td valign="top">classdir</td>
 *     <td valign="top">Indicates where compiled (unistrumented) classes are located.
 *       This is required in order to properly update the icontrol.properties file, not
 *       for instrumentation.</td>
 *     <td valign="top" align="center">Only if <code>updateicontrol=true</code></td>
 *   </tr>
 *   <tr>
 *     <td valign="top">targets</td>
 *     <td valign="top">Name of the file that will be generated by this task, which lists all the
 *        classes that iContract will instrument. If specified, the file will not be deleted after execution.
 *        If not specified, a file will still be created, but it will be deleted after execution.</td>
 *     <td valign="top" align="center">No</td>
 *   </tr>
 * </table>
 *
 * <p/>
 * <b>Note:</b> iContract will use the java compiler indicated by the project's
 * <code>build.compiler</code> property. See documentation of the Javac task for
 * more information.
 * <p/>
 * Nested includes and excludes are also supported.
 *
 * <p><b>Example:</b></p>
 * <pre>
 * &lt;icontract
 *    srcdir="${build.src}"
 *    instrumentdir="${build.instrument}"
 *    repositorydir="${build.repository}"
 *    builddir="${build.instrclasses}"
 *    updateicontrol="true"
 *    classdir="${build.classes}"
 *    controlfile="control"
 *    targets="targets"
 *    verbosity="error*,warning*"
 *    quiet="true"
 * >
 *    &lt;classpath refid="compile-classpath"/>
 * &lt;/icontract>
 * </pre>
 *
 */
public class IContract extends MatchingTask {

    private static final String ICONTROL_PROPERTIES_HEADER =
        " You might want to set classRoot to point to your normal compilation class root directory.";

    /** compiler to use for instrumenation */
    private String icCompiler = "javac";

    /** temporary file with file names of all java files to be instrumented */
    private File targets = null;

    /**
     * will be set to true if any of the sourca files are newer than the
     * instrumented files
     */
    private boolean dirty = false;

    /** set to true if the iContract jar is missing */
    private boolean iContractMissing = false;

    /** source file root */
    private File srcDir = null;

    /** instrumentation src root */
    private File instrumentDir = null;

    /** instrumentation build root */
    private File buildDir = null;

    /** repository src root */
    private File repositoryDir = null;

    /** repository build root */
    private File repBuildDir = null;

    /** classpath */
    private Path classpath = null;

    /** The class of the Throwable to be thrown on failed assertions */
    private String failThrowable = "java.lang.Error";

    /** The -v option */
    private String verbosity = "error*";

    /** The -q option */
    private boolean quiet = false;

    /** The -m option */
    private File controlFile = null;

    /** Indicates whether or not to instrument for preconditions */
    private boolean pre = true;
    private boolean preModified = false;

    /** Indicates whether or not to instrument for postconditions */
    private boolean post = true;
    private boolean postModified = false;

    /** Indicates whether or not to instrument for invariants */
    private boolean invariant = true;
    private boolean invariantModified = false;

    /** Indicates whether or not to instrument all files regardless of timestamp */
    // can't be explicitly set, is set if control file exists and is newer than any source file
    private boolean instrumentall = false;

    /**
     * Indicates the name of a properties file (intentionally for iControl)
     * where the classpath property should be updated.
     */
    private boolean updateIcontrol = false;

    /** Regular compilation class root  */
    private File classDir = null;

    /**
     * Sets the source directory.
     *
     * @param srcDir the source directory
     */
    public void setSrcdir(File srcDir) {
        this.srcDir = srcDir;
    }


    /**
     * Sets the class directory (uninstrumented classes).
     *
     * @param classDir the source directory
     */
    public void setClassdir(File classDir) {
        this.classDir = classDir;
    }


    /**
     * Sets the instrumentation directory.
     *
     * @param instrumentDir the source directory
     */
    public void setInstrumentdir(File instrumentDir) {
        this.instrumentDir = instrumentDir;
        if (this.buildDir == null) {
            setBuilddir(instrumentDir);
        }
    }


    /**
     * Sets the build directory for instrumented classes.
     *
     * @param buildDir the build directory
     */
    public void setBuilddir(File buildDir) {
        this.buildDir = buildDir;
    }


    /**
     * Sets the build directory for repository classes.
     *
     * @param repositoryDir the source directory
     */
    public void setRepositorydir(File repositoryDir) {
        this.repositoryDir = repositoryDir;
        if (this.repBuildDir == null) {
            setRepbuilddir(repositoryDir);
        }
    }


    /**
     * Sets the build directory for instrumented classes.
     *
     * @param repBuildDir the build directory
     */
    public void setRepbuilddir(File repBuildDir) {
        this.repBuildDir = repBuildDir;
    }


    /**
     * Turns on/off precondition instrumentation.
     *
     * @param pre true turns it on
     */
    public void setPre(boolean pre) {
        this.pre = pre;
        preModified = true;
    }


    /**
     * Turns on/off postcondition instrumentation.
     *
     * @param post true turns it on
     */
    public void setPost(boolean post) {
        this.post = post;
        postModified = true;
    }


    /**
     * Turns on/off invariant instrumentation.
     *
     * @param invariant true turns it on
     */
    public void setInvariant(boolean invariant) {
        this.invariant = invariant;
        invariantModified = true;
    }


    /**
     * Sets the Throwable (Exception) to be thrown on assertion violation.
     *
     * @param clazz the fully qualified Throwable class name
     */
    public void setFailthrowable(String clazz) {
        this.failThrowable = clazz;
    }


    /**
     * Sets the verbosity level of iContract. Any combination of
     * error*,warning*,note*,info*,progress*,debug* (comma separated) can be
     * used. Defaults to error*,warning*
     *
     * @param verbosity verbosity level
     */
    public void setVerbosity(String verbosity) {
        this.verbosity = verbosity;
    }


    /**
     * Tells iContract to be quiet.
     *
     * @param quiet true if iContract should be quiet.
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }


    /**
     * Sets the name of the file where targets will be written. That is the
     * file that tells iContract what files to process.
     *
     * @param targets the targets file name
     */
    public void setTargets(File targets) {
        this.targets = targets;
    }


    /**
     * Sets the control file to pass to iContract.
     *
     * @param controlFile the control file
     */
    public void setControlfile(File controlFile) {
        if (!controlFile.exists()) {
            log("WARNING: Control file " + controlFile.getAbsolutePath()
                 + " doesn't exist. iContract will be run "
                 + "without control file.");
        }
        this.controlFile = controlFile;
    }


    /**
     * Sets the classpath to be used for invocation of iContract.
     *
     * @param path the classpath
     */
    public void setClasspath(Path path) {
        createClasspath().append(path);
    }


    /**
     * Sets the classpath.
     *
     * @return the nested classpath element
     * @todo this overwrites the classpath so only one
     *       effective classpath element would work. This
     *       is not how we do this elsewhere.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath;
    }


    /**
     * Adds a reference to a classpath defined elsewhere.
     *
     * @param reference referenced classpath
     */
    public void setClasspathRef(Reference reference) {
        createClasspath().setRefid(reference);
    }


    /**
     * If true, updates iControl properties file
     *
     * @param updateIcontrol true if iControl properties file should be
     *      updated
     */
    public void setUpdateicontrol(boolean updateIcontrol) {
        this.updateIcontrol = updateIcontrol;
    }


    /**
     * Executes the task
     *
     * @exception BuildException if the instrumentation fails
     */
    public void execute() throws BuildException {
        preconditions();
        scan();
        if (dirty) {

            // turn off assertions if we're using controlfile, unless they are not explicitly set.
            boolean useControlFile = (controlFile != null) && controlFile.exists();

            if (useControlFile && !preModified) {
                pre = false;
            }
            if (useControlFile && !postModified) {
                post = false;
            }
            if (useControlFile && !invariantModified) {
                invariant = false;
            }
            // issue warning if pre,post or invariant is used together with controlfile
            if ((pre || post || invariant) && controlFile != null) {
                log("WARNING: specifying pre,post or invariant will "
                     + "override control file settings");
            }


            // We want to be notified if iContract jar is missing. This makes life easier for the user
            // who didn't understand that iContract is a separate library (duh!)
            getProject().addBuildListener(new IContractPresenceDetector());

            // Prepare the directories for iContract. iContract will make them if they
            // don't exist, but for some reason I don't know, it will complain about the REP files
            // afterwards
            Mkdir mkdir = (Mkdir) getProject().createTask("mkdir");

            mkdir.setDir(instrumentDir);
            mkdir.execute();
            mkdir.setDir(buildDir);
            mkdir.execute();
            mkdir.setDir(repositoryDir);
            mkdir.execute();

            // Set the classpath that is needed for regular Javac compilation
            Path baseClasspath = createClasspath();

            // Might need to add the core classes if we're not using Sun's Javac (like Jikes)
            String compiler = getProject().getProperty("build.compiler");
            ClasspathHelper classpathHelper = new ClasspathHelper(compiler);

            classpathHelper.modify(baseClasspath);

            // Create the classpath required to compile the sourcefiles BEFORE instrumentation
            Path beforeInstrumentationClasspath = ((Path) baseClasspath.clone());

            beforeInstrumentationClasspath.append(new Path(getProject(),
                srcDir.getAbsolutePath()));

            // Create the classpath required to compile the sourcefiles AFTER instrumentation
            Path afterInstrumentationClasspath = ((Path) baseClasspath.clone());

            afterInstrumentationClasspath.append(new Path(getProject(), instrumentDir.getAbsolutePath()));
            afterInstrumentationClasspath.append(new Path(getProject(), repositoryDir.getAbsolutePath()));
            afterInstrumentationClasspath.append(new Path(getProject(), srcDir.getAbsolutePath()));
            afterInstrumentationClasspath.append(new Path(getProject(), buildDir.getAbsolutePath()));

            // Create the classpath required to automatically compile the repository files
            Path repositoryClasspath = ((Path) baseClasspath.clone());

            repositoryClasspath.append(new Path(getProject(), instrumentDir.getAbsolutePath()));
            repositoryClasspath.append(new Path(getProject(), srcDir.getAbsolutePath()));
            repositoryClasspath.append(new Path(getProject(), repositoryDir.getAbsolutePath()));
            repositoryClasspath.append(new Path(getProject(), buildDir.getAbsolutePath()));

            // Create the classpath required for iContract itself
            Path iContractClasspath = ((Path) baseClasspath.clone());

            iContractClasspath.append(new Path(getProject(), System.getProperty("java.home") + File.separator + ".." + File.separator + "lib" + File.separator + "tools.jar"));
            iContractClasspath.append(new Path(getProject(), srcDir.getAbsolutePath()));
            iContractClasspath.append(new Path(getProject(), repositoryDir.getAbsolutePath()));
            iContractClasspath.append(new Path(getProject(), instrumentDir.getAbsolutePath()));
            iContractClasspath.append(new Path(getProject(), buildDir.getAbsolutePath()));

            // Create a forked java process
            Java iContract = (Java) getProject().createTask("java");

            iContract.setTaskName(getTaskName());
            iContract.setFork(true);
            iContract.setClassname("com.reliablesystems.iContract.Tool");
            iContract.setClasspath(iContractClasspath);

            // Build the arguments to iContract
            StringBuffer args = new StringBuffer();

            args.append(directiveString());
            args.append("-v").append(verbosity).append(" ");
            args.append("-b").append("\"").append(icCompiler).append(" -classpath ").append(beforeInstrumentationClasspath).append("\" ");
            args.append("-c").append("\"").append(icCompiler).append(" -classpath ").append(afterInstrumentationClasspath).append(" -d ").append(buildDir).append("\" ");
            args.append("-n").append("\"").append(icCompiler).append(" -classpath ").append(repositoryClasspath).append("\" ");
            args.append("-d").append(failThrowable).append(" ");
            args.append("-o").append(instrumentDir).append(File.separator).append("@p").append(File.separator).append("@f.@e ");
            args.append("-k").append(repositoryDir).append(File.separator).append("@p ");
            args.append(quiet ? "-q " : "");
            args.append(instrumentall ? "-a " : "");// reinstrument everything if controlFile exists and is newer than any class
            args.append("@").append(targets.getAbsolutePath());
            iContract.createArg().setLine(args.toString());

//System.out.println( "JAVA -classpath " + iContractClasspath + " com.reliablesystems.iContract.Tool " + args.toString() );

            // update iControlProperties if it's set.
            if (updateIcontrol) {
                Properties iControlProps = new Properties();

                try {// to read existing propertiesfile
                    iControlProps.load(new FileInputStream("icontrol.properties"));
                } catch (IOException e) {
                    log("File icontrol.properties not found. That's ok. Writing a default one.");
                }
                iControlProps.setProperty("sourceRoot", srcDir.getAbsolutePath());
                iControlProps.setProperty("classRoot", classDir.getAbsolutePath());
                iControlProps.setProperty("classpath", afterInstrumentationClasspath.toString());
                iControlProps.setProperty("controlFile", controlFile.getAbsolutePath());
                iControlProps.setProperty("targetsFile", targets.getAbsolutePath());

                try {// to read existing propertiesfile
                    iControlProps.store(new FileOutputStream("icontrol.properties"), ICONTROL_PROPERTIES_HEADER);
                    log("Updated icontrol.properties");
                } catch (IOException e) {
                    log("Couldn't write icontrol.properties.");
                }
            }

            // do it!
            int result = iContract.executeJava();

            if (result != 0) {
                if (iContractMissing) {
                    log("iContract can't be found on your classpath. Your classpath is:");
                    log(classpath.toString());
                    log("If you don't have the iContract jar, go get it at http://www.reliable-systems.com/tools/");
                }
                throw new BuildException("iContract instrumentation failed. Code=" + result);
            }
        } else {// not dirty
            //log( "Nothing to do. Everything up to date." );
        }
    }


    /** Checks that the required attributes are set.  */
    private void preconditions() throws BuildException {
        if (srcDir == null) {
            throw new BuildException("srcdir attribute must be set!", location);
        }
        if (!srcDir.exists()) {
            throw new BuildException("srcdir \"" + srcDir.getPath() + "\" does not exist!", location);
        }
        if (instrumentDir == null) {
            throw new BuildException("instrumentdir attribute must be set!", location);
        }
        if (repositoryDir == null) {
            throw new BuildException("repositorydir attribute must be set!", location);
        }
        if (updateIcontrol == true && classDir == null) {
            throw new BuildException("classdir attribute must be specified when updateicontrol=true!", location);
        }
        if (updateIcontrol == true && controlFile == null) {
            throw new BuildException("controlfile attribute must be specified when updateicontrol=true!", location);
        }
    }


    /**
     * Verifies whether any of the source files have changed. Done by
     * comparing date of source/class files. The whole lot is "dirty" if at
     * least one source file or the control file is newer than the
     * instrumented files. If not dirty, iContract will not be executed. <br/>
     * Also creates a temporary file with a list of the source files, that
     * will be deleted upon exit.
     */
    private void scan() throws BuildException {
        long now = (new Date()).getTime();

        DirectoryScanner ds = null;

        ds = getDirectoryScanner(srcDir);

        String[] files = ds.getIncludedFiles();

        FileOutputStream targetOutputStream = null;
        PrintStream targetPrinter = null;
        boolean writeTargets = false;

        try {
            if (targets == null) {
                targets = new File("targets");
                log("Warning: targets file not specified. generating file: " + targets.getName());
                writeTargets = true;
            } else if (!targets.exists()) {
                log("Specified targets file doesn't exist. generating file: " + targets.getName());
                writeTargets = true;
            }
            if (writeTargets) {
                log("You should consider using iControl to create a target file.");
                targetOutputStream = new FileOutputStream(targets);
                targetPrinter = new PrintStream(targetOutputStream);
            }
            for (int i = 0; i < files.length; i++) {
                File srcFile = new File(srcDir, files[i]);

                if (files[i].endsWith(".java")) {
                    // print the target, while we're at here. (Only if generatetarget=true).
                    if (targetPrinter != null) {
                        targetPrinter.println(srcFile.getAbsolutePath());
                    }
                    File classFile = new File(buildDir, files[i].substring(0, files[i].indexOf(".java")) + ".class");

                    if (srcFile.lastModified() > now) {
                        log("Warning: file modified in the future: " +
                            files[i], Project.MSG_WARN);
                    }

                    if (!classFile.exists() || srcFile.lastModified() > classFile.lastModified()) {
                        //log( "Found a file newer than the instrumentDir class file: " + srcFile.getPath() + " newer than " + classFile.getPath() + ". Running iContract again..." );
                        dirty = true;
                    }
                }
            }
            if (targetPrinter != null) {
                targetPrinter.flush();
                targetPrinter.close();
            }
        } catch (IOException e) {
            throw new BuildException("Could not create target file:" + e.getMessage());
        }

        // also, check controlFile timestamp
        long controlFileTime = -1;

        try {
            if (controlFile != null) {
                if (controlFile.exists() && buildDir.exists()) {
                    controlFileTime = controlFile.lastModified();
                    ds = getDirectoryScanner(buildDir);
                    files = ds.getIncludedFiles();
                    for (int i = 0; i < files.length; i++) {
                        File srcFile = new File(srcDir, files[i]);

                        if (files[i].endsWith(".class")) {
                            if (controlFileTime > srcFile.lastModified()) {
                                if (!dirty) {
                                    log("Control file " + controlFile.getAbsolutePath() + " has been updated. Instrumenting all files...");
                                }
                                dirty = true;
                                instrumentall = true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new BuildException("Got an interesting exception:" + t.getMessage());
        }
    }


    /**
     * Creates the -m option based on the values of controlFile, pre, post and
     * invariant.
     */
    private final String directiveString() {
        StringBuffer sb = new StringBuffer();
        boolean comma = false;

        boolean useControlFile = (controlFile != null) && controlFile.exists();

        if (useControlFile || pre || post || invariant) {
            sb.append("-m");
        }
        if (useControlFile) {
            sb.append("@").append(controlFile);
            comma = true;
        }
        if (pre) {
            if (comma) {
                sb.append(",");
            }
            sb.append("pre");
            comma = true;
        }
        if (post) {
            if (comma) {
                sb.append(",");
            }
            sb.append("post");
            comma = true;
        }
        if (invariant) {
            if (comma) {
                sb.append(",");
            }
            sb.append("inv");
        }
        sb.append(" ");
        return sb.toString();
    }


    /**
     * BuildListener that sets the iContractMissing flag to true if a message
     * about missing iContract is missing. Used to indicate a more verbose
     * error to the user, with advice about how to solve the problem
     *
     * @author Conor MacNeill
     */
    private class IContractPresenceDetector implements BuildListener {
        public void buildFinished(BuildEvent event) {
        }


        public void buildStarted(BuildEvent event) {
        }


        public void messageLogged(BuildEvent event) {
            if ("java.lang.NoClassDefFoundError: com/reliablesystems/iContract/Tool".equals(event.getMessage())) {
                iContractMissing = true;
            }
        }


        public void targetFinished(BuildEvent event) {
        }


        public void targetStarted(BuildEvent event) {
        }


        public void taskFinished(BuildEvent event) {
        }


        public void taskStarted(BuildEvent event) {
        }
    }


    /**
     * This class is a helper to set correct classpath for other compilers,
     * like Jikes. It reuses the logic from DefaultCompilerAdapter, which is
     * protected, so we have to subclass it.
     *
     * @author Conor MacNeill
     */
    private class ClasspathHelper extends DefaultCompilerAdapter {
        private final String compiler;


        public ClasspathHelper(String compiler) {
            super();
            this.compiler = compiler;
        }

        // make it public
        public void modify(Path path) {
            // depending on what compiler to use, set the includeJavaRuntime flag
            if ("jikes".equals(compiler)) {
                icCompiler = compiler;
                includeJavaRuntime = true;
                path.append(getCompileClasspath());
            }
        }

        // dummy implementation. Never called
        public void setJavac(Javac javac) {
        }


        public boolean execute() {
            return true;
        }
    }
}

