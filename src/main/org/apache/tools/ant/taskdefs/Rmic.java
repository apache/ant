/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.rmic.*;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.*;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.util.Vector;

/**
 * Task to compile RMI stubs and skeletons. This task can take the following
 * arguments:
 * <ul>
 * <li>base: The base directory for the compiled stubs and skeletons
 * <li>class: The name of the class to generate the stubs from
 * <li>stubVersion: The version of the stub prototol to use (1.1, 1.2, compat)
 * <li>sourceBase: The base directory for the generated stubs and skeletons
 * <li>classpath: Additional classpath, appended before the system classpath
 * <li>iiop: Generate IIOP compatable output 
 * <li>iiopopts: Include IIOP options 
 * <li>idl: Generate IDL output 
 * <li>idlopts: Include IDL options 
 * <li>includeantruntime
 * <li>includejavaruntime
 * <li>extdirs
 * </ul>
 * Of these arguments, <b>base</b> is required.
 * <p>
 * If classname is specified then only that classname will be compiled. If it
 * is absent, then <b>base</b> is traversed for classes according to patterns.
 * <p>
 *
 * @author duncan@x180.com
 * @author ludovic.claude@websitewatchers.co.uk
 * @author David Maclean <a href="mailto:david@cm.co.za">david@cm.co.za</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
 * @author Takashi Okamoto tokamoto@rd.nttdata.co.jp
 */

public class Rmic extends MatchingTask {

    private static final String FAIL_MSG 
        = "Rmic failed, messages should have been provided.";

    private File baseDir;
    private String classname;
    private File sourceBase;
    private String stubVersion;
    private Path compileClasspath;
    private Path extdirs;
    private boolean verify = false;
    private boolean filtering = false;

    private boolean iiop = false;
    private String  iiopopts;
    private boolean idl  = false;
    private String  idlopts;
    private boolean debug  = false;
    private boolean includeAntRuntime = true;
    private boolean includeJavaRuntime = false;

    private Vector compileList = new Vector();

    private ClassLoader loader = null;

    /** Sets the base directory to output generated class. */
    public void setBase(File base) {
        this.baseDir = base;
    }

    /** Gets the base directory to output generated class. */
    public File getBase() {
        return this.baseDir;
    }

    /** Sets the class name to compile. */
    public void setClassname(String classname) {
        this.classname = classname;
    }

    /** Gets the class name to compile. */
    public String getClassname() {
        return classname;
    }

    /** Sets the source dirs to find the source java files. */
    public void setSourceBase(File sourceBase) {
        this.sourceBase = sourceBase;
    }

    /** Gets the source dirs to find the source java files. */
    public File getSourceBase() {
        return sourceBase;
    }

    /** Sets the stub version. */
    public void setStubVersion(String stubVersion) {
        this.stubVersion = stubVersion;
    }

    public String getStubVersion() {
        return stubVersion;
    }

    public void setFiltering(boolean filter) {
        filtering = filter;
    }

    public boolean getFiltering() {
        return filtering;
    }

    /** Sets the debug flag. */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Gets the debug flag. */
    public boolean getDebug() {
        return debug;
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

    /**
     * Creates a nested classpath element.
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
     * Gets the classpath. 
     */
    public Path getClasspath() {
        return compileClasspath; 
    }

    /**
     * Indicates that the classes found by the directory match should be
     * checked to see if they implement java.rmi.Remote.
     * This defaults to false if not set.  */
    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    /** Get verify flag. */
    public boolean getVerify() {
        return verify;
    }

    /**
     * Indicates that IIOP compatible stubs should
     * be generated.  This defaults to false 
     * if not set.  
     */
    public void setIiop(boolean iiop) {
        this.iiop = iiop;
    }

    /** Gets iiop flags. */
    public boolean getIiop() {
        return iiop;
    }

    /**
     * pass additional arguments for iiop 
     */
    public void setIiopopts(String iiopopts) {
        this.iiopopts = iiopopts;
    }

    /** Gets additional arguments for iiop. */
    public String getIiopopts() {
        return iiopopts;
    }

    /**
     * Indicates that IDL output should be 
     * generated.  This defaults to false 
     * if not set.  
     */
    public void setIdl(boolean idl) {
        this.idl = idl;
    }

    /* Gets IDL flags. */
    public boolean getIdl() {
        return idl;
    }

    /**
     * pass additional arguments for idl compile 
     */
    public void setIdlopts(String idlopts) {
        this.idlopts = idlopts;
    }

    /**
     * Gets additional arguments for idl compile. 
     */
    public String getIdlopts() {
        return idlopts;
    }

    /** Gets file list to compile. */
    public Vector getFileList() {
        return compileList;
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
     * Maybe creates a nested extdirs element.
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path(project);
        }
        return extdirs.createPath();
    }

    /**
     * Gets the extension directories that will be used during the
     * compilation.
     */
    public Path getExtdirs() {
        return extdirs;
    }

    public Vector getCompileList() {
        return compileList;
    }

    public void execute() throws BuildException {
        if (baseDir == null) {
            throw new BuildException("base attribute must be set!", location);
        }
        if (!baseDir.exists()) {
            throw new BuildException("base does not exist!", location);
        }

        if (verify) {
            log("Verify has been turned on.", Project.MSG_INFO);
        }

        String compiler = project.getProperty("build.rmic");
        RmicAdapter adapter = RmicAdapterFactory.getRmic(compiler, this );
            
        // now we need to populate the compiler adapter
        adapter.setRmic( this );

        Path classpath = adapter.getClasspath();
        loader = new AntClassLoader(project, classpath);

        // scan base dirs to build up compile lists only if a
        // specific classname is not given
        if (classname == null) {
            DirectoryScanner ds = this.getDirectoryScanner(baseDir);
            String[] files = ds.getIncludedFiles();
            scanDir(baseDir, files, adapter.getMapper());
        } else {
            // otherwise perform a timestamp comparison - at least
            scanDir(baseDir, 
                    new String[] {classname.replace('.', File.separatorChar) + ".class"},
                    adapter.getMapper());
        }
        
        int fileCount = compileList.size();
        if (fileCount > 0) {
            log("RMI Compiling " + fileCount +
                " class"+ (fileCount > 1 ? "es" : "")+" to " + baseDir, 
                Project.MSG_INFO);

            // finally, lets execute the compiler!!
            if (!adapter.execute()) {
                throw new BuildException(FAIL_MSG, location);
            }
        }

        /* 
         * Move the generated source file to the base directory.  If
         * base directory and sourcebase are the same, the generated
         * sources are already in place.
         */
        if (null != sourceBase && !baseDir.equals(sourceBase)) {
            if (idl) {
                log("Cannot determine sourcefiles in idl mode, ", 
                    Project.MSG_WARN);
                log("sourcebase attribute will be ignored.", Project.MSG_WARN);
            } else {
                for (int j = 0; j < fileCount; j++) {
                    moveGeneratedFile(baseDir, sourceBase,
                                      (String) compileList.elementAt(j),
                                      adapter);
                }
            }
        }
        compileList.removeAllElements();
    }

    /**
     * Move the generated source file(s) to the base directory
     *
     * @exception org.apache.tools.ant.BuildException When error copying/removing files.
     */
    private void moveGeneratedFile (File baseDir, File sourceBaseFile,
                                    String classname,
                                    RmicAdapter adapter)
        throws BuildException {

        String classFileName = 
            classname.replace('.', File.separatorChar) + ".class";
        String[] generatedFiles = 
            adapter.getMapper().mapFileName(classFileName);

        for (int i=0; i<generatedFiles.length; i++) {
            String sourceFileName = 
                classFileName.substring(0, classFileName.length()-6) + ".java";
            File oldFile = new File(baseDir, sourceFileName);
            File newFile = new File(sourceBaseFile, sourceFileName);
            try {
                project.copyFile(oldFile, newFile, filtering);
                oldFile.delete();
            } catch (IOException ioe) {
                String msg = "Failed to copy " + oldFile + " to " +
                    newFile + " due to " + ioe.getMessage();
                throw new BuildException(msg, ioe, location);
            }
        }
    }

    /**
     * Scans the directory looking for class files to be compiled.
     * The result is returned in the class variable compileList.
     */
    protected void scanDir(File baseDir, String files[],
                           FileNameMapper mapper) {

        String[] newFiles = files;
        if (idl) {
            log("will leave uptodate test to rmic implementation in idl mode.",
                Project.MSG_VERBOSE);
        } else if (iiop 
                   && iiopopts != null && iiopopts.indexOf("-always") > -1) {
            log("no uptodate test as -always option has been specified",
                Project.MSG_VERBOSE);
        } else {
            SourceFileScanner sfs = new SourceFileScanner(this);
            newFiles = sfs.restrict(files, baseDir, baseDir, mapper);
        }

        for (int i = 0; i < newFiles.length; i++) {
            String classname = newFiles[i].replace(File.separatorChar, '.');
            classname = classname.substring(0, classname.lastIndexOf(".class"));
            compileList.addElement(classname);
        }
    }

    /**
     * Load named class and test whether it can be rmic'ed
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
            log("Unable to verify class " + classname + 
                ". It could not be found.", Project.MSG_WARN);
        } catch (NoClassDefFoundError e) {
            log("Unable to verify class " + classname + 
                ". It is not defined.", Project.MSG_WARN);
        } catch (Throwable t) {
            log("Unable to verify class " + classname + 
                ". Loading caused Exception: " +
                t.getMessage(), Project.MSG_WARN);
        }
        // we only get here if an exception has been thrown
        return false;
    }

    /**
     * Returns the topmost interface that extends Remote for a given
     * class - if one exists.
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
     */
    public ClassLoader getLoader() {return loader;}

}

