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
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.*;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;

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
 */

public class Rmic extends MatchingTask {

    private File baseDir;
    private String classname;
    private File sourceBase;
    private String stubVersion;
    private Path compileClasspath;
    private boolean verify = false;
    private boolean filtering = false;

    private boolean iiop = false;
    private String  iiopopts;
    private boolean idl  = false;
    private String  idlopts;
    private boolean debug  = false;

    private Vector compileList = new Vector();

    private ClassLoader loader = null;

    public void setBase(File base) {
        this.baseDir = base;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public void setSourceBase(File sourceBase) {
        this.sourceBase = sourceBase;
    }

    public void setStubVersion(String stubVersion) {
        this.stubVersion = stubVersion;
    }

    public void setFiltering(boolean filter) {
        filtering = filter;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
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
     * Indicates that the classes found by the directory match should be
     * checked to see if they implement java.rmi.Remote.
     * This defaults to false if not set.  */
    public void setVerify(boolean verify) {
        this.verify = verify;
    }

    /**
     * Indicates that IIOP compatible stubs should
     * be generated.  This defaults to false 
     * if not set.  
     */
    public void setIiop(boolean iiop) {
        this.iiop = iiop;
    }

    /**
     * pass additional arguments for iiop 
     */
    public void setIiopopts(String iiopopts) {
        this.iiopopts = iiopopts;
    }

    /**
     * Indicates that IDL output should be 
     * generated.  This defaults to false 
     * if not set.  
     */
    public void setIdl(boolean idl) {
        this.idl = idl;
    }

    /**
     * pass additional arguments for idl compile 
     */
    public void setIdlopts(String idlopts) {
        this.idlopts = idlopts;
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
        if (iiop) {
            log("IIOP has been turned on.", Project.MSG_INFO);
            if( iiopopts != null ) {
                log("IIOP Options: " + iiopopts, Project.MSG_INFO );
            }
        }
        if (idl) {
            log("IDL has been turned on.", Project.MSG_INFO);
            if( idlopts != null ) {
                log("IDL Options: " + idlopts, Project.MSG_INFO );
            }
        }

        Path classpath = getCompileClasspath(baseDir);
        loader = new AntClassLoader(project, classpath);

        // scan base dirs to build up compile lists only if a
        // specific classname is not given
        if (classname == null) {
            DirectoryScanner ds = this.getDirectoryScanner(baseDir);
            String[] files = ds.getIncludedFiles();
            scanDir(baseDir, files);
        } else {
            // otherwise perform a timestamp comparison - at least
            scanDir(baseDir, 
                    new String[] {classname.replace('.', File.separatorChar) + ".class"});
        }
        
        // XXX
        // need to provide an input stream that we read in from!

        OutputStream logstr = new LogOutputStream(this, Project.MSG_WARN);
        sun.rmi.rmic.Main compiler = new sun.rmi.rmic.Main(logstr, "rmic");
        Commandline cmd = new Commandline();
        
        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(baseDir);
        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);
        if (null != stubVersion) {
            if ("1.1".equals(stubVersion))
                cmd.createArgument().setValue("-v1.1");
            else if ("1.2".equals(stubVersion))
                cmd.createArgument().setValue("-v1.2");
            else
                cmd.createArgument().setValue("-vcompat");
        }
        if (null != sourceBase)
            cmd.createArgument().setValue("-keepgenerated");

        if( iiop ) {
            cmd.createArgument().setValue("-iiop");
            if( iiopopts != null ) 
                cmd.createArgument().setValue(iiopopts);
        }

        if( idl )  {
            cmd.createArgument().setValue("-idl");
            if( idlopts != null ) 
                cmd.createArgument().setValue(idlopts);
        }
        if( debug )  {
            cmd.createArgument().setValue("-g");
        }

        int fileCount = compileList.size();
        if (fileCount > 0) {
            log("RMI Compiling " + fileCount +
                " class"+ (fileCount > 1 ? "es" : "")+" to " + baseDir, 
                Project.MSG_INFO);
            
            for (int j = 0; j < fileCount; j++) {
                cmd.createArgument().setValue((String) compileList.elementAt(j));
            }
            log("Compilation args: " + cmd.toString(), Project.MSG_VERBOSE);
            compiler.compile(cmd.getArguments());
        }

        // Move the generated source file to the base directory
        if (null != sourceBase) {
            for (int j = 0; j < fileCount; j++) {
                moveGeneratedFile(baseDir, sourceBase, (String) compileList.elementAt(j));
            }
        }
        compileList.removeAllElements();
    }

    /**
     * Move the generated source file(s) to the base directory
     *
     * @exception org.apache.tools.ant.BuildException When error copying/removing files.
     */
    private void moveGeneratedFile (File baseDir, File sourceBaseFile, String classname)
            throws BuildException {
        String stubFileName = classname.replace('.', File.separatorChar) + "_Stub.java";
        File oldStubFile = new File(baseDir, stubFileName);
        File newStubFile = new File(sourceBaseFile, stubFileName);
        try {
            project.copyFile(oldStubFile, newStubFile, filtering);
            oldStubFile.delete();
        } catch (IOException ioe) {
            String msg = "Failed to copy " + oldStubFile + " to " +
                newStubFile + " due to " + ioe.getMessage();
            throw new BuildException(msg, ioe, location);
        }
        if (!"1.2".equals(stubVersion)) {
            String skelFileName = classname.replace('.', '/') + "_Skel.java";
            File oldSkelFile = new File(baseDir, skelFileName);
            File newSkelFile = new File(sourceBaseFile, skelFileName);
            try {
                project.copyFile(oldSkelFile, newSkelFile, filtering);
                oldSkelFile.delete();
            } catch (IOException ioe) {
                String msg = "Failed to copy " + oldSkelFile + " to " +
                              newSkelFile + " due to " + ioe.getMessage();
                throw new BuildException(msg, ioe, location);
            }
        }
    }

    /**
     * Scans the directory looking for class files to be compiled.
     * The result is returned in the class variable compileList.
     */

    protected void scanDir(File baseDir, String files[]) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        String[] newFiles = sfs.restrict(files, baseDir, baseDir,
                                         new RmicFileNameMapper());
        for (int i = 0; i < newFiles.length; i++) {
            String classname = newFiles[i].replace(File.separatorChar, '.');
            classname = classname.substring(0, classname.indexOf(".class"));
            compileList.addElement(classname);
        }
    }
 
    /**
     * Builds the compilation classpath.
     */

    // XXX
    // we need a way to not use the current classpath.

    private Path getCompileClasspath(File baseFile) {
        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        Path classpath = new Path(project, baseFile.getAbsolutePath());

        // Combine the build classpath with the system classpath, in an 
        // order determined by the value of build.classpath
        if (compileClasspath == null) {
            classpath.addExisting(Path.systemClasspath);
        } else {
            classpath.addExisting(compileClasspath.concatSystemClasspath());
        }

        // in jdk 1.2, the system classes are not on the visible classpath.
        if (Project.getJavaVersion().startsWith("1.2")) {
            String bootcp = System.getProperty("sun.boot.class.path");
            if (bootcp != null) {
                classpath.addExisting(new Path(project, bootcp));
            }
        }
        return classpath;
    }

    /**
     * Mapper that possibly returns two file names, *_Stub and *_Skel.
     */
    private class RmicFileNameMapper implements FileNameMapper {

        private GlobPatternMapper stubMapper;
        private GlobPatternMapper skelMapper;

        RmicFileNameMapper() {
            stubMapper = new GlobPatternMapper();
            stubMapper.setFrom("*.class");
            stubMapper.setTo("*_Stub.class");

            // no _Skel file in stub version 1.2
            if (!"1.2".equals(stubVersion)) {
                skelMapper = new GlobPatternMapper();
                skelMapper.setFrom("*.class");
                skelMapper.setTo("*_Skel.class");
            }
        }

        /**
         * Empty implementation.
         */
        public void setFrom(String s) {}
        /**
         * Empty implementation.
         */
        public void setTo(String s) {}

        public String[] mapFileName(String name) {
            String[] stubName = stubMapper.mapFileName(name);

            if (stubName == null || name.endsWith("_Stub.class") 
                || name.endsWith("_Skel.class")) {
                // Not a .class file
                return null;
            }

            String classname = name.replace(File.separatorChar, '.');
            classname = classname.substring(0, classname.indexOf(".class"));
            if (verify) {
                try {
                    Class testClass = loader.loadClass(classname);
                    // One cannot RMIC an interface
                    if (testClass.isInterface() || 
                        !isValidRmiRemote(testClass)) {
                        return null;
                    }
                } catch (ClassNotFoundException e) {
                    log("Unable to verify class " + classname + 
                        ". It could not be found.", Project.MSG_WARN);
                } catch (NoClassDefFoundError e) {
                    log("Unable to verify class " + classname + 
                        ". It is not defined.", Project.MSG_WARN);
                }
            }

            if (skelMapper != null) {
                return new String[] {
                    stubName[0], 
                    skelMapper.mapFileName(name)[0]
                };
            } else {
                return stubName;
            }
        }

        /**
         * Check to see if the class or superclasses/interfaces implement
         * java.rmi.Remote.
         */
        private boolean isValidRmiRemote (Class testClass) {
            Class rmiRemote = java.rmi.Remote.class;
            
            if (rmiRemote.equals(testClass)) {
                // This class is java.rmi.Remote
                return true;
            }
            
            Class [] interfaces = testClass.getInterfaces();
            if (interfaces != null) {
                for (int i = 0; i < interfaces.length; i++) {
                    if (rmiRemote.equals(interfaces[i])) {
                        // This class directly implements java.rmi.Remote
                        return true;
                    }
                    if (isValidRmiRemote(interfaces[i])) {
                        return true;
                    }
                }
            }
            return false;
        }
        
    }

}

