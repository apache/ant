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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import org.apache.tools.ant.*;
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
 */

public class Rmic extends MatchingTask {

    private String base;
    private String classname;
    private String sourceBase;
    private String stubVersion;
    private Path compileClasspath;
    private boolean verify = false;
    private boolean filtering = false;

    private Vector compileList = new Vector();

    public void setBase(String base) {
        this.base = base;
    }

    public void setClass(String classname) {
        log("The class attribute is deprecated. " +
            "Please use the classname attribute.",
            Project.MSG_WARN);
        this.classname = classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public void setSourceBase(String sourceBase) {
        this.sourceBase = sourceBase;
    }

    public void setStubVersion(String stubVersion) {
        this.stubVersion = stubVersion;
    }

    public void setFiltering(String filter) {
        filtering = Project.toBoolean(filter);
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
     * Maybe creates a nesetd classpath element.
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path();
        }
        return compileClasspath;
    }

    /**
     * Indicates that the classes found by the directory match should be
     * checked to see if they implement java.rmi.Remote.
     * This defaults to false if not set.
     */
    public void setVerify(String verify) {
        this.verify = Project.toBoolean(verify);
    }

    public void execute() throws BuildException {
        File baseDir = project.resolveFile(base);
        if (baseDir == null) {
            throw new BuildException("base attribute must be set!", location);
        }
        if (!baseDir.exists()) {
            throw new BuildException("base does not exist!", location);
        }

        if (verify) {
            log("Verify has been turned on.", Project.MSG_INFO);
        }
        File sourceBaseFile = null;
        if (null != sourceBase) {
            sourceBaseFile = project.resolveFile(sourceBase);
        }
        String classpath = getCompileClasspath(baseDir);

        // scan base dirs to build up compile lists only if a
        // specific classname is not given
        if (classname == null) {
            DirectoryScanner ds = this.getDirectoryScanner(baseDir);
            String[] files = ds.getIncludedFiles();
            scanDir(baseDir, files, verify);
        }
        
        // XXX
        // need to provide an input stream that we read in from!

        sun.rmi.rmic.Main compiler = new sun.rmi.rmic.Main(System.out, "rmic");
        int argCount = 5;
        int i = 0;
        if (null != stubVersion) argCount++;
        if (null != sourceBase) argCount++;
        if (compileList.size() > 0) argCount += compileList.size() - 1;
        String[] args = new String[argCount];
        args[i++] = "-d";
        args[i++] = baseDir.getAbsolutePath();
        args[i++] = "-classpath";
        args[i++] = classpath;
        if (null != stubVersion) {
            if ("1.1".equals(stubVersion))
                args[i++] = "-v1.1";
            else if ("1.2".equals(stubVersion))
                args[i++] = "-v1.2";
            else
                args[i++] = "-vcompat";
        }
        if (null != sourceBase) args[i++] = "-keepgenerated";

        if (classname != null) {
            if (shouldCompile(new File(baseDir, classname.replace('.', File.separatorChar) + ".class"))) {
                args[i++] = classname;
                compiler.compile(args);
            }
        } else {
            if (compileList.size() > 0) {
                log("RMI Compiling " + compileList.size() +
                    " classes to " + baseDir, Project.MSG_INFO);

                for (int j = 0; j < compileList.size(); j++) {
                    args[i++] = (String) compileList.elementAt(j);
                }
                compiler.compile(args);
            }
        }

        // Move the generated source file to the base directory
        if (null != sourceBase) {
            if (classname != null) {
                moveGeneratedFile(baseDir, sourceBaseFile, classname);
            } else {
                for (int j = 0; j < compileList.size(); j++) {
                    moveGeneratedFile(baseDir, sourceBaseFile, (String) compileList.elementAt(j));
                }
            }
        }
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

    protected void scanDir(File baseDir, String files[], boolean shouldVerify) {
        compileList.removeAllElements();
        for (int i = 0; i < files.length; i++) {
            File baseFile = new File(baseDir, files[i]);
            if (files[i].endsWith(".class") &&
                !files[i].endsWith("_Stub.class") &&
                !files[i].endsWith("_Skel.class")) {
                if (shouldCompile(baseFile)) {
                    String classname = files[i].replace(File.separatorChar, '.');
                    classname = classname.substring(0, classname.indexOf(".class"));
                    boolean shouldAdd = true;
                    if (shouldVerify) {
                        try {
                            Class testClass = Class.forName(classname);
                            // One cannot RMIC an interface
                            if (testClass.isInterface() || !isValidRmiRemote(testClass)) {
                                shouldAdd = false;
                            }
                        } catch (ClassNotFoundException e) {
                            log("Unable to verify class " + classname + 
                                    ". It could not be found.", Project.MSG_WARN);
                        } catch (NoClassDefFoundError e) {
                            log("Unable to verify class " + classname + 
                                        ". It is not defined.", Project.MSG_WARN);
                        }
                    }
                    if (shouldAdd) {
                        log("Adding: " + classname + " to compile list",
                            Project.MSG_VERBOSE);
                        compileList.addElement(classname);
                    }
                }
            }
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

    /**
     * Determine whether the class needs to be RMI compiled. It looks at the _Stub.class
     * and _Skel.class files' last modification date and compares it with the class' class file.
     */
    private boolean shouldCompile (File classFile) {
        long now = (new Date()).getTime();
        File stubFile = new File(classFile.getAbsolutePath().substring(0,
                classFile.getAbsolutePath().indexOf(".class")) + "_Stub.class");
        File skelFile = new File(classFile.getAbsolutePath().substring(0,
                classFile.getAbsolutePath().indexOf(".class")) + "_Skel.class");
        if (classFile.exists()) {
            if (classFile.lastModified() > now) {
                log("Warning: file modified in the future: " +
                    classFile, Project.MSG_WARN);
            }

            if (classFile.lastModified() > stubFile.lastModified()) {
                return true;
            } else if (classFile.lastModified() > skelFile.lastModified()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the compilation classpath.
     */

    // XXX
    // we need a way to not use the current classpath.

    private String getCompileClasspath(File baseFile) {
        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        Path classpath = new Path(baseFile.getAbsolutePath());

        // add our classpath to the mix

        if (compileClasspath != null) {
            addExistingToClasspath(classpath,compileClasspath);
        }

        // add the system classpath
        addExistingToClasspath(classpath, Path.systemClasspath);

        // in jdk 1.2, the system classes are not on the visible classpath.
        if (Project.getJavaVersion().startsWith("1.2")) {
            String bootcp = System.getProperty("sun.boot.class.path");
            if (bootcp != null) {
                addExistingToClasspath(classpath, new Path(bootcp));
            }
        }
        return classpath.toString();
    }

     /**
     * Takes a classpath-like string, and adds each element of
     * this string to a new classpath, if the components exist.
     * Components that don't exist, aren't added.
     * We do this, because jikes issues warnings for non-existant
     * files/dirs in his classpath, and these warnings are pretty
     * annoying.
     * @param target - target classpath
     * @param source - source classpath
     * to get file objects.
     */
    private void addExistingToClasspath(Path target, Path source) {
        String[] list = source.list();
        for (int i=0; i<list.length; i++) {
            File f = project.resolveFile(list[i]);

            if (f.exists()) {
                target.setLocation(f.getAbsolutePath());
           } else {
               log("Dropping from classpath: "+
                   f.getAbsolutePath(), Project.MSG_VERBOSE);
           }
        }
    }
}

