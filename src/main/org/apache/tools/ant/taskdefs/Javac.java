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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;
import org.apache.tools.ant.taskdefs.compilers.*;

import java.io.File;

/**
 * Task to compile Java source files. This task can take the following
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
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * When this task executes, it will recursively scan the sourcedir and
 * destdir looking for Java source files to compile. This task makes its
 * compile decision based on timestamp. 
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */

public class Javac extends MatchingTask {

    private static final String FAIL_MSG 
        = "Compile failed, messages should have been provided.";

    private Path src;
    private File destDir;
    private Path compileClasspath;
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

    protected boolean failOnError = true;
    protected File[] compileList = new File[0];

    /**
     * Create a nested <src ...> element for multiple source path
     * support.
     *
     * @return a nexted src element.
     */
    public Path createSrc() {
        if (src == null) {
            src = new Path(project);
        }
        return src.createPath();
    }

    /**
     * Set the source dirs to find the source Java files.
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
     * Set the classpath to be used for this compilation.
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
     * Maybe creates a nested classpath element.
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(project);
        }
        return compileClasspath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
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
     * Maybe creates a nested classpath element.
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(project);
        }
        return bootclasspath.createPath();
    }

    /**
     * Adds a reference to a CLASSPATH defined elsewhere.
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
     * Maybe creates a nested classpath element.
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path(project);
        }
        return extdirs.createPath();
    }

    /**
     * Throw a BuildException if compilation fails
     */
    public void setFailonerror(boolean fail) {
        failOnError = fail;
    }

    /**
     * Proceed if compilation fails
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
     * Set the deprecation flag.
     */
    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

    /** Gets the deprecation flag. */
    public boolean getDeprecation() {
        return deprecation;
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
     * Set the debug flag.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Gets the debug flag. */
    public boolean getDebug() {
        return debug;
    }

    /**
     * Set the optimize flag.
     */
    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    /** Gets the optimize flag. */
    public boolean getOptimize() {
        return optimize;
    }

    /** 
     * Set the depend flag.
     */ 
    public void setDepend(boolean depend) {
        this.depend = depend;
    }  

    /** Gets the depend flag. */
    public boolean getDepend() {
        return depend;
    }

    /** 
     * Set the verbose flag.
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
     * Include ant's own classpath in this task's classpath?
     */
    public void setIncludeantruntime( boolean include ) {
        includeAntRuntime = include;
    }

    /**
     * Gets whether or not the ant classpath is to be included in the
     * task's classpath.
     */
    public boolean getIncludeantruntime() {
        return includeAntRuntime;
    }

    /**
     * Sets whether or not to include the java runtime libraries to this
     * task's classpath.
     */
    public void setIncludejavaruntime( boolean include ) {
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
     * Executes the task.
     */
    public void execute() throws BuildException {
        // first off, make sure that we've got a srcdir

        if (src == null) {
            throw new BuildException("srcdir attribute must be set!", location);
        }
        String [] list = src.list();
        if (list.length == 0) {
            throw new BuildException("srcdir attribute must be set!", location);
        }
        
        if (destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory \"" + destDir + "\" does not exist or is not a directory", location);
        }

        // scan source directories and dest directory to build up 
        // compile lists
        resetFileLists();
        for (int i=0; i<list.length; i++) {
            File srcDir = (File)project.resolveFile(list[i]);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir \"" + srcDir.getPath() + "\" does not exist!", location);
            }

            DirectoryScanner ds = this.getDirectoryScanner(srcDir);

            String[] files = ds.getIncludedFiles();

            scanDir(srcDir, destDir != null ? destDir : srcDir, files);
        }
        
        // compile the source files

        String compiler = project.getProperty("build.compiler");
        if (compiler == null) {
            if (Project.getJavaVersion().startsWith("1.3")) {
                compiler = "modern";
            } else {
                compiler = "classic";
            }
        }

        if (compileList.length > 0) {

            CompilerAdapter adapter = CompilerAdapterFactory.getCompiler(
              compiler, this );
            log("Compiling " + compileList.length + 
                " source file"
                + (compileList.length == 1 ? "" : "s")
                + (destDir != null ? " to " + destDir : ""));

            // now we need to populate the compiler adapter
            adapter.setJavac( this );
            
            // finally, lets execute the compiler!!
            if (!adapter.execute()) {
                if (failOnError) {
                    throw new BuildException(FAIL_MSG, location);
                }
                else {
                    log(FAIL_MSG, Project.MSG_ERR);
                }
            }
        }
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
    protected void scanDir(File srcDir, File destDir, String files[]) {
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

}
