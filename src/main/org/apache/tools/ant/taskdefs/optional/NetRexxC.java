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
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import netrexx.lang.Rexx;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

// CheckStyle:InnerAssignmentCheck OFF - used too much in the file to be removed
/**
 * Compiles NetRexx source files.
 * This task can take the following
 * arguments:
 * <ul>
 * <li>binary</li>
 * <li>classpath</li>
 * <li>comments</li>
 * <li>compile</li>
 * <li>console</li>
 * <li>crossref</li>
 * <li>decimal</li>
 * <li>destdir</li>
 * <li>diag</li>
 * <li>explicit</li>
 * <li>format</li>
 * <li>keep</li>
 * <li>logo</li>
 * <li>replace</li>
 * <li>savelog</li>
 * <li>srcdir</li>
 * <li>sourcedir</li>
 * <li>strictargs</li>
 * <li>strictassign</li>
 * <li>strictcase</li>
 * <li>strictimport</li>
 * <li>symbols</li>
 * <li>time</li>
 * <li>trace</li>
 * <li>utf8</li>
 * <li>verbose</li>
 * <li>suppressMethodArgumentNotUsed</li>
 * <li>suppressPrivatePropertyNotUsed</li>
 * <li>suppressVariableNotUsed</li>
 * <li>suppressExceptionNotSignalled</li>
 * <li>suppressDeprecation</li>
 * <li>removeKeepExtension</li>
 * </ul>
 * Of these arguments, the <b>srcdir</b> argument is required.
 *
 * <p>When this task executes, it will recursively scan the srcdir
 * looking for NetRexx source files to compile. This task makes its
 * compile decision based on timestamp.</p>
 * <p>Before files are compiled they and any other file in the
 * srcdir will be copied to the destdir allowing support files to be
 * located properly in the classpath. The reason for copying the source files
 * before the compile is that NetRexxC has only two destinations for classfiles:</p>
 * <ol>
 * <li>The current directory, and,</li>
 * <li>The directory the source is in (see sourcedir option)
 * </ol>
 */
public class NetRexxC extends MatchingTask {

    // variables to hold arguments
    private boolean binary;
    private String classpath;
    private boolean comments;
    private boolean compact = true; // should be the default, as it integrates better in ant.
    private boolean compile = true;
    private boolean console;
    private boolean crossref;
    private boolean decimal = true;
    private File destDir;
    private boolean diag;
    private boolean explicit;
    private boolean format;
    private boolean keep;
    private boolean logo = true;
    private boolean replace;
    private boolean savelog;
    private File srcDir;
    private boolean sourcedir = true; // ?? Should this be the default for ant?
    private boolean strictargs;
    private boolean strictassign;
    private boolean strictcase;
    private boolean strictimport;
    private boolean strictprops;
    private boolean strictsignal;
    private boolean symbols;
    private boolean time;
    private String trace = "trace2";
    private boolean utf8;
    private String verbose = "verbose3";
    private boolean suppressMethodArgumentNotUsed = false;
    private boolean suppressPrivatePropertyNotUsed = false;
    private boolean suppressVariableNotUsed = false;
    private boolean suppressExceptionNotSignalled = false;
    private boolean suppressDeprecation = false;
    private boolean removeKeepExtension = false;

    // constants for the messages to suppress by flags and their corresponding properties
    static final String MSG_METHOD_ARGUMENT_NOT_USED
        = "Warning: Method argument is not used";
    static final String MSG_PRIVATE_PROPERTY_NOT_USED
        = "Warning: Private property is defined but not used";
    static final String MSG_VARIABLE_NOT_USED
        = "Warning: Variable is set but not used";
    static final String MSG_EXCEPTION_NOT_SIGNALLED
        = "is in SIGNALS list but is not signalled within the method";
    static final String MSG_DEPRECATION = "has been deprecated";

    // other implementation variables
    private Vector<String> compileList = new Vector<>();
    private Hashtable<String, String> filecopyList = new Hashtable<>();

    /**
     * Set whether literals are treated as binary, rather than NetRexx types.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default is false.
     * @param binary a <code>boolean</code> value.
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    /**
     * Set the classpath used for NetRexx compilation.
     * @param classpath the classpath to use.
     */
    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    /**
     * Set whether comments are passed through to the generated java source.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param comments a <code>boolean</code> value.
     */
    public void setComments(boolean comments) {
        this.comments = comments;
    }

    /**
     * Set whether error messages come out in compact or verbose format.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     * @param compact a <code>boolean</code> value.
     */
    public void setCompact(boolean compact) {
        this.compact = compact;
    }

    /**
     * Set whether the NetRexx compiler should compile the generated java code.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     * Setting this flag to false, will automatically set the keep flag to true.
     * @param compile a <code>boolean</code> value.
     */
    public void setCompile(boolean compile) {
        this.compile = compile;
        if (!this.compile && !this.keep) {
            this.keep = true;
        }
    }

    /**
     * Set whether or not compiler messages should be displayed on the 'console'.
     * Note that this task will rely on the default value for filtering compile messages.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param console a <code>boolean</code> value.
     */
    public void setConsole(boolean console) {
        this.console = console;
    }

    /**
     * Whether variable cross references are generated.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param crossref a <code>boolean</code> value.
     */
    public void setCrossref(boolean crossref) {
        this.crossref = crossref;
    }

    /**
     * Set whether decimal arithmetic should be used for the netrexx code.
     * Setting this to off will report decimal arithmetic as an error, for
     * performance critical applications.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     * @param decimal a <code>boolean</code> value.
     */
    public void setDecimal(boolean decimal) {
        this.decimal = decimal;
    }

    /**
     * Set the destination directory into which the NetRexx source files
     * should be copied and then compiled.
     * @param destDirName the destination directory.
     */
    public void setDestDir(File destDirName) {
        destDir = destDirName;
    }

    /**
     * Whether diagnostic information about the compile is generated
     * @param diag a <code>boolean</code> value.
     */
    public void setDiag(boolean diag) {
        this.diag = diag;
    }

    /**
     * Sets whether variables must be declared explicitly before use.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param explicit a <code>boolean</code> value.
     */
    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    /**
     * Whether the generated java code is formatted nicely or left to match
     * NetRexx line numbers for call stack debugging.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value false.
     * @param format a <code>boolean</code> value.
     */
    public void setFormat(boolean format) {
        this.format = format;
    }

    /**
     * Whether the generated java code is produced.
     * This is not implemented yet.
     * @param java a <code>boolean</code> value.
     */
    public void setJava(boolean java) {
        log("The attribute java is currently unused.", Project.MSG_WARN);
    }

    /**
     * Sets whether the generated java source file should be kept after
     * compilation. The generated files will have an extension of .java.keep,
     * <b>not</b> .java. See setRemoveKeepExtension
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param keep a <code>boolean</code> value.
     * @see #setRemoveKeepExtension(boolean)
     */
    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    /**
     * Whether the compiler text logo is displayed when compiling.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param logo a <code>boolean</code> value.
     */
    public void setLogo(boolean logo) {
        this.logo = logo;
    }

    /**
     * Whether the generated .java file should be replaced when compiling.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param replace a <code>boolean</code> value.
     */
    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    /**
     * Sets whether the compiler messages will be written to NetRexxC.log as
     * well as to the console.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param savelog a <code>boolean</code> value.
     */
    public void setSavelog(boolean savelog) {
        this.savelog = savelog;
    }

    /**
     * Tells the NetRexx compiler to store the class files in the same
     * directory as the source files. The alternative is the working directory.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is true.
     * @param sourcedir a <code>boolean</code> value.
     */
    public void setSourcedir(boolean sourcedir) {
        this.sourcedir = sourcedir;
    }

    /**
     * Set the source dir to find the source Java files.
     * @param srcDirName the source directory.
     */
    public void setSrcDir(File srcDirName) {
        srcDir = srcDirName;
    }

    /**
     * Tells the NetRexx compiler that method calls always need parentheses,
     * even if no arguments are needed, e.g. <code>aStringVar.getBytes</code>
     * vs. <code>aStringVar.getBytes()</code>.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param strictargs a <code>boolean</code> value.
     */
    public void setStrictargs(boolean strictargs) {
        this.strictargs = strictargs;
    }

    /**
     * Tells the NetRexx compile that assignments must match exactly on type.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param strictassign a <code>boolean</code> value.
     */
    public void setStrictassign(boolean strictassign) {
        this.strictassign = strictassign;
    }

    /**
     * Specifies whether the NetRexx compiler should be case sensitive or not.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param strictcase a <code>boolean</code> value.
     */
    public void setStrictcase(boolean strictcase) {
        this.strictcase = strictcase;
    }

    /**
     * Sets whether classes need to be imported explicitly using an <code>import</code>
     * statement. By default the NetRexx compiler will import certain packages
     * automatically.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param strictimport a <code>boolean</code> value.
     */
    public void setStrictimport(boolean strictimport) {
        this.strictimport = strictimport;
    }

    /**
     * Sets whether local properties need to be qualified explicitly using
     * <code>this</code>.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param strictprops a <code>boolean</code> value.
     */
    public void setStrictprops(boolean strictprops) {
        this.strictprops = strictprops;
    }

    /**
     * Whether the compiler should force catching of exceptions by explicitly
     * named types.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false
     * @param strictsignal a <code>boolean</code> value.
     */
    public void setStrictsignal(boolean strictsignal) {
        this.strictsignal = strictsignal;
    }

    /**
     * Sets whether debug symbols should be generated into the class file.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param symbols a <code>boolean</code> value.
     */
    public void setSymbols(boolean symbols) {
        this.symbols = symbols;
    }

    /**
     * Asks the NetRexx compiler to print compilation times to the console
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param time a <code>boolean</code> value.
     */
    public void setTime(boolean time) {
        this.time = time;
    }

    /**
     * Turns on or off tracing and directs the resultant trace output Valid
     * values are: "trace", "trace1", "trace2" and "notrace". "trace" and
     * "trace2".
     * @param trace the value to set.
     */
    public void setTrace(TraceAttr trace) {
        this.trace = trace.getValue();
    }

    /**
     * Turns on or off tracing and directs the resultant trace output Valid
     * values are: "trace", "trace1", "trace2" and "notrace". "trace" and
     * "trace2".
     * @param trace the value to set.
     */
    public void setTrace(String trace) {
        TraceAttr t = new TraceAttr();
        t.setValue(trace);
        setTrace(t);
    }

    /**
     * Tells the NetRexx compiler that the source is in UTF8.
     * Valid true values are "yes", "on" or "true". Anything else sets the flag to false.
     * The default value is false.
     * @param utf8 a <code>boolean</code> value.
     */
    public void setUtf8(boolean utf8) {
        this.utf8 = utf8;
    }

    /**
     * Whether lots of warnings and error messages should be generated
     * @param verbose the value to set - verbose&lt;level&gt; or noverbose.
     */
    public void setVerbose(VerboseAttr verbose) {
        this.verbose = verbose.getValue();
    }

    /**
     * Whether lots of warnings and error messages should be generated
     * @param verbose the value to set - verbose&lt;level&gt; or noverbose.
     */
    public void setVerbose(String verbose) {
        VerboseAttr v = new VerboseAttr();
        v.setValue(verbose);
        setVerbose(v);
    }

    /**
     * Whether the task should suppress the "Method argument is not used" in
     * strictargs-Mode, which can not be suppressed by the compiler itself.
     * The warning is logged as verbose message, though.
     * @param suppressMethodArgumentNotUsed a <code>boolean</code> value.
     */
    public void setSuppressMethodArgumentNotUsed(boolean suppressMethodArgumentNotUsed) {
        this.suppressMethodArgumentNotUsed = suppressMethodArgumentNotUsed;
    }

    /**
     * Whether the task should suppress the "Private property is defined but
     * not used" in strictargs-Mode, which can be quite annoying while
     * developing. The warning is logged as verbose message, though.
     * @param suppressPrivatePropertyNotUsed a <code>boolean</code> value.
     */
    public void setSuppressPrivatePropertyNotUsed(boolean suppressPrivatePropertyNotUsed) {
        this.suppressPrivatePropertyNotUsed = suppressPrivatePropertyNotUsed;
    }

    /**
     * Whether the task should suppress the "Variable is set but not used" in
     * strictargs-Mode. Be careful with this one! The warning is logged as
     * verbose message, though.
     * @param suppressVariableNotUsed a <code>boolean</code> value.
     */
    public void setSuppressVariableNotUsed(boolean suppressVariableNotUsed) {
        this.suppressVariableNotUsed = suppressVariableNotUsed;
    }

    /**
     * Whether the task should suppress the "FooException is in SIGNALS list
     * but is not signalled within the method", which is sometimes rather
     * useless. The warning is logged as verbose message, though.
     * @param suppressExceptionNotSignalled a <code>boolean</code> value.
     */
    public void setSuppressExceptionNotSignalled(boolean suppressExceptionNotSignalled) {
        this.suppressExceptionNotSignalled = suppressExceptionNotSignalled;
    }

    /**
     * Tells whether we should filter out any deprecation-messages
     * of the compiler out.
     * @param suppressDeprecation a <code>boolean</code> value.
     */
    public void setSuppressDeprecation(boolean suppressDeprecation) {
        this.suppressDeprecation = suppressDeprecation;
    }

    /**
     * Tells whether the trailing .keep in nocompile-mode should be removed
     * so that the resulting java source really ends on .java.
     * This facilitates the use of the javadoc tool later on.
     * @param removeKeepExtension boolean
     */
    public void setRemoveKeepExtension(boolean removeKeepExtension) {
        this.removeKeepExtension = removeKeepExtension;
    }

    /**
     * init-Method sets defaults from Properties. That way, when ant is called
     * with arguments like -Dant.netrexxc.verbose=verbose5 one can easily take
     * control of all netrexxc-tasks.
     */
    @Override
    public void init() {
        String p;

        if ((p = getProject().getProperty("ant.netrexxc.binary")) != null) {
            this.binary = Project.toBoolean(p);
        }
        // classpath makes no sense
        if ((p = getProject().getProperty("ant.netrexxc.comments")) != null) {
            this.comments = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.compact")) != null) {
            this.compact = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.compile")) != null) {
            this.compile = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.console")) != null) {
            this.console = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.crossref")) != null) {
            this.crossref = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.decimal")) != null) {
            this.decimal = Project.toBoolean(p);
            // destDir
        }
        if ((p = getProject().getProperty("ant.netrexxc.diag")) != null) {
            this.diag = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.explicit")) != null) {
            this.explicit = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.format")) != null) {
            this.format = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.keep")) != null) {
            this.keep = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.logo")) != null) {
            this.logo = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.replace")) != null) {
            this.replace = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.savelog")) != null) {
            this.savelog = Project.toBoolean(p);
            // srcDir
        }
        if ((p = getProject().getProperty("ant.netrexxc.sourcedir")) != null) {
            this.sourcedir = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.strictargs")) != null) {
            this.strictargs = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.strictassign")) != null) {
            this.strictassign = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.strictcase")) != null) {
            this.strictcase = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.strictimport")) != null) {
            this.strictimport = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.strictprops")) != null) {
            this.strictprops = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.strictsignal")) != null) {
            this.strictsignal = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.symbols")) != null) {
            this.symbols = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.time")) != null) {
            this.time = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.trace")) != null) {
            setTrace(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.utf8")) != null) {
            this.utf8 = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.verbose")) != null) {
            setVerbose(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.suppressMethodArgumentNotUsed")) != null) {
            this.suppressMethodArgumentNotUsed = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.suppressPrivatePropertyNotUsed")) != null) {
            this.suppressPrivatePropertyNotUsed = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.suppressVariableNotUsed")) != null) {
            this.suppressVariableNotUsed = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.suppressExceptionNotSignalled")) != null) {
            this.suppressExceptionNotSignalled = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.suppressDeprecation")) != null) {
            this.suppressDeprecation = Project.toBoolean(p);
        }
        if ((p = getProject().getProperty("ant.netrexxc.removeKeepExtension")) != null) {
            this.removeKeepExtension = Project.toBoolean(p);
        }
    }

    /**
     * Executes the task - performs the actual compiler call.
     * @throws BuildException on error.
     */
    @Override
    public void execute() throws BuildException {

        // first off, make sure that we've got a srcdir and destdir
        if (srcDir == null || destDir == null) {
            throw new BuildException("srcDir and destDir attributes must be set!");
        }

        // scan source and dest dirs to build up both copy lists and
        // compile lists
        DirectoryScanner ds = getDirectoryScanner(srcDir);

        scanDir(srcDir, destDir, ds.getIncludedFiles());

        // copy the source and support files
        copyFilesToDestination();

        // compile the source files
        if (!compileList.isEmpty()) {
            log("Compiling " + compileList.size() + " source file"
                 + (compileList.size() == 1 ? "" : "s")
                 + " to " + destDir);
            doNetRexxCompile();
            if (removeKeepExtension && (!compile || keep)) {
                removeKeepExtensions();
            }
        }
    }

    /**
     * Scans the directory looking for source files to be compiled and support
     * files to be copied.
     */
    private void scanDir(File srcDir, File destDir, String[] files) {
        for (String filename : files) {
            File srcFile = new File(srcDir, filename);
            File destFile = new File(destDir, filename);
            // if it's a non source file, copy it if a later date than the
            // dest
            // if it's a source file, see if the destination class file
            // needs to be recreated via compilation
            if (filename.toLowerCase().endsWith(".nrx")) {
                File classFile =
                    new File(destDir,
                    filename.substring(0, filename.lastIndexOf('.')) + ".class");
                File javaFile =
                    new File(destDir,
                    filename.substring(0, filename.lastIndexOf('.'))
                    + (removeKeepExtension ? ".java" : ".java.keep"));

                // nocompile case tests against .java[.keep] file
                if (!compile && srcFile.lastModified() > javaFile.lastModified()) {
                    filecopyList.put(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
                    compileList.addElement(destFile.getAbsolutePath());
                } else if (compile && srcFile.lastModified() > classFile.lastModified()) {
                    // compile case tests against .class file
                    filecopyList.put(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
                    compileList.addElement(destFile.getAbsolutePath());
                }
            } else if (srcFile.lastModified() > destFile.lastModified()) {
                filecopyList.put(srcFile.getAbsolutePath(), destFile.getAbsolutePath());
            }
        }
    }

    /** Copy eligible files from the srcDir to destDir  */
    private void copyFilesToDestination() {
        if (!filecopyList.isEmpty()) {
            log("Copying " + filecopyList.size() + " file"
                 + (filecopyList.size() == 1 ? "" : "s")
                 + " to " + destDir.getAbsolutePath());

            filecopyList.forEach((fromFile, toFile) -> {
                try {
                    FileUtils.getFileUtils().copyFile(fromFile, toFile);
                } catch (IOException ioe) {
                    throw new BuildException("Failed to copy " + fromFile
                        + " to " + toFile + " due to " + ioe.getMessage(), ioe);
                }
            });
        }
    }

    /**
     * Rename .java.keep files (back) to .java. The netrexxc renames all
     * .java files to .java.keep if either -keep or -nocompile option is set.
     */
    private void removeKeepExtensions() {
        if (!compileList.isEmpty()) {
            log("Removing .keep extension on " + compileList.size() + " file"
                 + (compileList.size() == 1 ? "" : "s"));
            compileList.forEach(nrxName -> {
                String baseName =
                    nrxName.substring(0, nrxName.lastIndexOf('.'));
                File fromFile = new File(baseName + ".java.keep");
                File toFile = new File(baseName + ".java");
                if (fromFile.renameTo(toFile)) {
                    log("Successfully renamed " + fromFile + " to " + toFile,
                        Project.MSG_VERBOSE);
                } else {
                    log("Failed to rename " + fromFile + " to " + toFile);
                }
            });
        }
    }

    /** Performs a compile using the NetRexx 1.1.x compiler  */
    private void doNetRexxCompile() throws BuildException {
        log("Using NetRexx compiler", Project.MSG_VERBOSE);

        String classpath = getCompileClasspath();

        // create an array of strings for input to the compiler: one array
        // comes from the compile options, the other from the compileList
        String[] compileOptionsArray = getCompileOptionsAsArray();

        // print nice output about what we are doing for the log
        log(Stream.of(compileOptionsArray)
            .collect(Collectors.joining(" ", "Compilation args: ", "")),
            Project.MSG_VERBOSE);

        log("Files to be compiled:", Project.MSG_VERBOSE);

        log(compileList.stream().map(s -> String.format("    %s%n", s))
                        .collect(Collectors.joining("")), Project.MSG_VERBOSE);

        // create a single array of arguments for the compiler
        String[] compileArgs =
                Stream.concat(Stream.of(compileOptionsArray), compileList.stream())
                .toArray(String[]::new);

        // need to set java.class.path property and restore it later
        // since the NetRexx compiler has no option for the classpath
        String currentClassPath = System.getProperty("java.class.path");
        Properties currentProperties = System.getProperties();

        currentProperties.put("java.class.path", classpath);

        try {
            StringWriter out = new StringWriter();
            PrintWriter w;
            int rc =
                COM.ibm.netrexx.process.NetRexxC.main(new Rexx(compileArgs),
                                                      w = new PrintWriter(out)); //NOSONAR
            String sdir = srcDir.getAbsolutePath();
            String ddir = destDir.getAbsolutePath();
            boolean doReplace = !(sdir.equals(ddir));
            int dlen = ddir.length();
            BufferedReader in = new BufferedReader(new StringReader(out.toString()));

            log("replacing destdir '" + ddir + "' through sourcedir '"
                + sdir + "'", Project.MSG_VERBOSE);

            String l;
            while ((l = in.readLine()) != null) {
                int idx;

                while (doReplace && ((idx = l.indexOf(ddir)) != -1)) {
                    // path is mentioned in the message
                    l = new StringBuilder(l).replace(idx, idx + dlen, sdir).toString();
                }
                // verbose level logging for suppressed messages
                if (suppressMethodArgumentNotUsed
                    && l.contains(MSG_METHOD_ARGUMENT_NOT_USED)) {
                    log(l, Project.MSG_VERBOSE);
                } else if (suppressPrivatePropertyNotUsed
                    && l.contains(MSG_PRIVATE_PROPERTY_NOT_USED)) {
                    log(l, Project.MSG_VERBOSE);
                } else if (suppressVariableNotUsed
                    && l.contains(MSG_VARIABLE_NOT_USED)) {
                    log(l, Project.MSG_VERBOSE);
                } else if (suppressExceptionNotSignalled
                    && l.contains(MSG_EXCEPTION_NOT_SIGNALLED)) {
                    log(l, Project.MSG_VERBOSE);
                } else if (suppressDeprecation
                    && l.contains(MSG_DEPRECATION)) {
                    log(l, Project.MSG_VERBOSE);
                } else if (l.contains("Error:")) {
                    // error level logging for compiler errors
                    log(l, Project.MSG_ERR);
                } else if (l.contains("Warning:")) {
                    // warning for all warning messages
                    log(l, Project.MSG_WARN);
                } else {
                    log(l, Project.MSG_INFO); // info level for the rest.
                }
            }
            if (rc > 1) {
                throw new BuildException(
                    "Compile failed, messages should have been provided.");
            }
            if (w.checkError()) {
                throw new IOException("Encountered an error");
            }
        } catch (IOException ioe) {
            throw new BuildException(
                "Unexpected IOException while playing with Strings", ioe);
        } finally {
            // need to reset java.class.path property
            // since the NetRexx compiler has no option for the classpath
            currentProperties = System.getProperties();
            currentProperties.put("java.class.path", currentClassPath);
        }
    }

    /** Builds the compilation classpath.  */
    private String getCompileClasspath() {
        StringBuilder classpath = new StringBuilder();

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        classpath.append(destDir.getAbsolutePath());

        // add our classpath to the mix
        if (this.classpath != null) {
            addExistingToClasspath(classpath, this.classpath);
        }

        // add the system classpath
        return classpath.toString();
    }

    /** This  */
    private String[] getCompileOptionsAsArray() {
        List<String> options = new ArrayList<>();

        options.add(binary ? "-binary" : "-nobinary");
        options.add(comments ? "-comments" : "-nocomments");
        options.add(compile ? "-compile" : "-nocompile");
        options.add(compact ? "-compact" : "-nocompact");
        options.add(console ? "-console" : "-noconsole");
        options.add(crossref ? "-crossref" : "-nocrossref");
        options.add(decimal ? "-decimal" : "-nodecimal");
        options.add(diag ? "-diag" : "-nodiag");
        options.add(explicit ? "-explicit" : "-noexplicit");
        options.add(format ? "-format" : "-noformat");
        options.add(keep ? "-keep" : "-nokeep");
        options.add(logo ? "-logo" : "-nologo");
        options.add(replace ? "-replace" : "-noreplace");
        options.add(savelog ? "-savelog" : "-nosavelog");
        options.add(sourcedir ? "-sourcedir" : "-nosourcedir");
        options.add(strictargs ? "-strictargs" : "-nostrictargs");
        options.add(strictassign ? "-strictassign" : "-nostrictassign");
        options.add(strictcase ? "-strictcase" : "-nostrictcase");
        options.add(strictimport ? "-strictimport" : "-nostrictimport");
        options.add(strictprops ? "-strictprops" : "-nostrictprops");
        options.add(strictsignal ? "-strictsignal" : "-nostrictsignal");
        options.add(symbols ? "-symbols" : "-nosymbols");
        options.add(time ? "-time" : "-notime");
        options.add("-" + trace);
        options.add(utf8 ? "-utf8" : "-noutf8");
        options.add("-" + verbose);

        return options.toArray(new String[0]);
    }

    /**
     * Takes a classpath-like string, and adds each element of this string to
     * a new classpath, if the components exist. Components that don't exist,
     * aren't added. We do this, because jikes issues warnings for
     * non-existent files/dirs in his classpath, and these warnings are pretty
     * annoying.
     *
     * @param target - target classpath
     * @param source - source classpath to get file objects.
     */
    private void addExistingToClasspath(StringBuilder target, String source) {
        StringTokenizer tok = new StringTokenizer(source,
            File.pathSeparator, false);

        while (tok.hasMoreTokens()) {
            File f = getProject().resolveFile(tok.nextToken());

            if (f.exists()) {
                target.append(File.pathSeparator);
                target.append(f.getAbsolutePath());
            } else {
                log("Dropping from classpath: "
                    + f.getAbsolutePath(), Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Enumerated class corresponding to the trace attribute.
     */
    public static class TraceAttr extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {"trace", "trace1", "trace2", "notrace"};
        }
    }

    /**
     * Enumerated class corresponding to the verbose attribute.
     */
    public static class VerboseAttr extends EnumeratedAttribute {
        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {"verbose", "verbose0", "verbose1", "verbose2",
                "verbose3", "verbose4", "verbose5", "noverbose"};
        }
    }
}
