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
 * Of these arguments, the <b>base</b> and <b>class</b> are required.
 * <p>
 *
 * @author duncan@x180.com
 * @author ludovic.claude@websitewatchers.co.uk
 */

public class Rmic extends Task {

    private String base;
    private String classname;
    private String sourceBase;
    private String stubVersion;
    private String compileClasspath;
    private boolean filtering = false;

    public void setBase(String base) {
        this.base = base;
    }

    public void setClass(String classname) {
        project.log("The class attribute is deprecated. " +
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
    public void setClasspath(String classpath) {
        compileClasspath = project.translatePath(classpath);
    }

    public void execute() throws BuildException {
        File baseFile = project.resolveFile(base);
        File sourceBaseFile = null;
        if (null != sourceBase)
            sourceBaseFile = project.resolveFile(sourceBase);
        String classpath = getCompileClasspath(baseFile);
        // XXX
        // need to provide an input stream that we read in from!

        sun.rmi.rmic.Main compiler = new sun.rmi.rmic.Main(System.out, "rmic");
            int argCount = 5;
        int i = 0;
        if (null != stubVersion) argCount++;
        if (null != sourceBase) argCount++;
        String[] args = new String[argCount];
        args[i++] = "-d";
        args[i++] = baseFile.getAbsolutePath();
        args[i++] = "-classpath";
        args[i++] = classpath;
        args[i++] = classname;
        if (null != stubVersion) {
            if ("1.1".equals(stubVersion))
                args[i++] = "-v1.1";
            else if ("1.2".equals(stubVersion))
                args[i++] = "-v1.2";
            else
                args[i++] = "-vcompat";
        }
        if (null != sourceBase) args[i++] = "-keepgenerated";

        compiler.compile(args);

        // Move the generated source file to the base directory
        if (null != sourceBase) {
                String stubFileName = classname.replace('.', '/') + "_Stub.java";
            File oldStubFile = new File(baseFile, stubFileName);
            File newStubFile = new File(sourceBaseFile, stubFileName);
            try {
                project.copyFile(oldStubFile, newStubFile, filtering);
                oldStubFile.delete();
            } catch (IOException ioe) {
                String msg = "Failed to copy " + oldStubFile + " to " +
                             newStubFile + " due to " + ioe.getMessage();
                throw new BuildException(msg);
            }
            if (!"1.2".equals(stubVersion)) {
                String skelFileName = classname.replace('.', '/') + "_Skel.java";
                File oldSkelFile = new File(baseFile, skelFileName);
                File newSkelFile = new File(sourceBaseFile, skelFileName);
                try {
                    project.copyFile(oldSkelFile, newSkelFile, filtering);
                    oldSkelFile.delete();
                } catch (IOException ioe) {
                    String msg = "Failed to copy " + oldSkelFile + " to " +
                                  newSkelFile + " due to " + ioe.getMessage();
                    throw new BuildException(msg);
                }
            }
        }
    }

    /**
     * Builds the compilation classpath.
     */

    // XXX
    // we need a way to not use the current classpath.

    private String getCompileClasspath(File baseFile) {
        StringBuffer classpath = new StringBuffer();

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath
        classpath.append(baseFile.getAbsolutePath());

        // add our classpath to the mix

        if (compileClasspath != null) {
            addExistingToClasspath(classpath,compileClasspath);
        }

        // add the system classpath

        addExistingToClasspath(classpath,System.getProperty("java.class.path"));
        // in jdk 1.2, the system classes are not on the visible classpath.

        if (Project.getJavaVersion().startsWith("1.2")) {
            String bootcp = System.getProperty("sun.boot.class.path");
            if (bootcp != null) {
                addExistingToClasspath(classpath, bootcp);
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
    private void addExistingToClasspath(StringBuffer target,String source) {
       StringTokenizer tok = new StringTokenizer(source,
                             System.getProperty("path.separator"), false);
       while (tok.hasMoreTokens()) {
           File f = project.resolveFile(tok.nextToken());

           if (f.exists()) {
               target.append(File.pathSeparator);
               target.append(f.getAbsolutePath());
           } else {
               project.log("Dropping from classpath: "+
                   f.getAbsolutePath(),project.MSG_VERBOSE);
           }
       }

    }
}

