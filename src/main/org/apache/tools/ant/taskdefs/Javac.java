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

import java.lang.reflect.Method;
import java.io.*;
import java.util.*;

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
 * <li>target
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * When this task executes, it will recursively scan the sourcedir and
 * destdir looking for Java source files to compile. This task makes its
 * compile decision based on timestamp. Any other file in the
 * sourcedir will be copied to the destdir allowing support files to be
 * located properly in the classpath.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 */

public class Javac extends MatchingTask {

    /**
     * Integer returned by the "Modern" jdk1.3 compiler to indicate success.
     */
    private static final int
        MODERN_COMPILER_SUCCESS = 0;

    private Path src;
    private File destDir;
    private Path compileClasspath;
    private boolean debug = false;
    private boolean optimize = false;
    private boolean deprecation = false;
    private boolean filtering = false;
    private String target;
    private Path bootclasspath;
    private Path extdirs;

    protected Vector compileList = new Vector();
    protected Hashtable filecopyList = new Hashtable();

    /**
     * Create a nested <src ...> element for multiple source path
     * support.
     *
     * @return a nexted src element.
     */
    public Path createSrc() {
        if (src != null) {
            src = new Path();
        }
        return src;
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

    /**
     * Set the destination directory into which the Java source
     * files should be compiled.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
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
     * Maybe creates a nested classpath element.
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path();
        }
        return compileClasspath;
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
     * Maybe creates a nested classpath element.
     */
    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path();
        }
        return bootclasspath;
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
     * Maybe creates a nested classpath element.
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path();
        }
        return extdirs;
    }

    /**
     * Set the deprecation flag.
     */
    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

    /**
     * Set the debug flag.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Set the optimize flag.
     */
     public void setOptimize(boolean optimize) {
         this.optimize = optimize;
     }

    /**
     * Sets the target VM that the classes will be compiled for. Valid
     * strings are "1.1", "1.2", and "1.3".
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Set the filtering flag.
     */
    public void setFiltering(boolean filter) {
        filtering = filter;
    }

    /**
     * Executes the task.
     */
    public void execute() throws BuildException {
        // first off, make sure that we've got a srcdir and destdir

        if (src == null) {
            throw new BuildException("srcdir attribute must be set!");
        }
        
        String [] list = src.list();
        if (list.length == 0) {
            throw new BuildException("srcdir attribute must be set!");
        }
        
        if (destDir == null) {
            throw new BuildException("destdir attribute must be set!");
        }

        // scan source directories and dest directory to build up both copy lists and
        // compile lists
        resetFileLists();
        for (int i=0; i<list.length; i++) {
            File srcDir = (File)project.resolveFile(list[i]);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir " + srcDir.getPath() + " does not exist!");
            }

            DirectoryScanner ds = this.getDirectoryScanner(srcDir);

            String[] files = ds.getIncludedFiles();

            scanDir(srcDir, destDir, files);
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

        if (compileList.size() > 0) {
            log("Compiling " + compileList.size() + 
                " source files to " + destDir);

            if (compiler.equalsIgnoreCase("classic")) {
                doClassicCompile();
            } else if (compiler.equalsIgnoreCase("modern")) {
                doModernCompile();
            } else if (compiler.equalsIgnoreCase("jikes")) {
                doJikesCompile();
            } else {
                String msg = "Don't know how to use compiler " + compiler;
                throw new BuildException(msg);
            }
        }

        // copy the support files

        if (filecopyList.size() > 0) {
            log("The implicit copying of support files by javac has been deprecated. " +
                "Use the copydir task to copy support files explicitly.",
                Project.MSG_WARN);

            log("Copying " + filecopyList.size() +
                " support files to " + destDir.getAbsolutePath());
            Enumeration enum = filecopyList.keys();
            while (enum.hasMoreElements()) {
                String fromFile = (String) enum.nextElement();
                String toFile = (String) filecopyList.get(fromFile);
                try {
                    project.copyFile(fromFile, toFile, filtering);
                } catch (IOException ioe) {
                    String msg = "Failed to copy " + fromFile + " to " + toFile
                        + " due to " + ioe.getMessage();
                    throw new BuildException(msg);
                }
            }
        }
    }

    /**
     * Clear the list of files to be compiled and copied.. 
     */
    protected void resetFileLists() {
        compileList.removeAllElements();
        filecopyList.clear();
    }

    /**
     * Scans the directory looking for source files to be compiled and
     * support files to be copied.  The results are returned in the
     * class variables compileList and filecopyList.
     */

    protected void scanDir(File srcDir, File destDir, String files[]) {

        long now = (new Date()).getTime();

        for (int i = 0; i < files.length; i++) {
            File srcFile = new File(srcDir, files[i]);
            if (files[i].endsWith(".java")) {
                File classFile = new File(destDir, files[i].substring(0,
                                          files[i].indexOf(".java")) + ".class");

                if (srcFile.lastModified() > now) {
                    log("Warning: file modified in the future: " +
                        files[i], Project.MSG_WARN);
                }

                if (srcFile.lastModified() > classFile.lastModified()) {
                    compileList.addElement(srcFile.getAbsolutePath());
                }
            } else {
                File destFile = new File(destDir, files[i]);
                if (srcFile.lastModified() > destFile.lastModified()) {
                    filecopyList.put(srcFile.getAbsolutePath(),
                                     destFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Builds the compilation classpath.
     */

    // XXX
    // we need a way to not use the current classpath.

    /**
     * @param addRuntime Shall <code>rt.jar</code> or
     * <code>classes.zip</code> be added to the classpath.  
     */
    private Path getCompileClasspath(boolean addRuntime) {
        Path classpath = new Path();

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        classpath.setLocation(destDir.getAbsolutePath());

        // add our classpath to the mix

        if (compileClasspath != null) {
            addExistingToClasspath(classpath,compileClasspath);
        }

        // add the system classpath

        addExistingToClasspath(classpath, Path.systemClasspath);
        if (addRuntime) {
            if (Project.getJavaVersion() == Project.JAVA_1_1) {
                addExistingToClasspath(classpath,
                                       new Path(System.getProperty("java.home")
                                                + File.separator + "lib"
                                                + File.separator 
                                                + "classes.zip"));
            } else {
                // JDK > 1.1 seems to set java.home to the JRE directory.
                addExistingToClasspath(classpath,
                                       new Path(System.getProperty("java.home")
                                                + File.separator + "lib"
                                                + File.separator + "rt.jar"));
                // Just keep the old version as well and let addExistingToPath
                // sort it out.
                addExistingToClasspath(classpath,
                                       new Path(System.getProperty("java.home")
                                                + File.separator +"jre"
                                                + File.separator + "lib"
                                                + File.separator + "rt.jar"));
            }
        }
            
        return classpath;
    }


     /**
     * Takes a Path, and adds each element of
     * another Path to a new classpath, if the components exist.
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

    /**
     * Peforms a copmile using the classic compiler that shipped with
     * JDK 1.1 and 1.2.
     */

    private void doClassicCompile() throws BuildException {
        log("Using classic compiler", Project.MSG_VERBOSE);
        Path classpath = getCompileClasspath(false);
        Vector argList = new Vector();

        if (deprecation == true)
            argList.addElement("-deprecation");

        argList.addElement("-d");
        argList.addElement(destDir.getAbsolutePath());
        argList.addElement("-classpath");
        // Just add "sourcepath" to classpath ( for JDK1.1 )
        if (Project.getJavaVersion().startsWith("1.1")) {
            argList.addElement(classpath.toString() + File.pathSeparator +
                               src.toString());
        } else {
            argList.addElement(classpath.toString());
            argList.addElement("-sourcepath");
            argList.addElement(src.toString());
            if (target != null) {
                argList.addElement("-target");
                argList.addElement(target);
            }
        }
        if (debug) {
            argList.addElement("-g");
        }
        if (optimize) {
            argList.addElement("-O");
        }
        if (bootclasspath != null) {
            argList.addElement("-bootclasspath");
            argList.addElement(bootclasspath.toString());
        }
        if (extdirs != null) {
            argList.addElement("-extdirs");
            argList.addElement(extdirs.toString());
        }

        log("Compilation args: " + argList.toString(),
            Project.MSG_VERBOSE);

        String[] args = new String[argList.size() + compileList.size()];
        int counter = 0;

        for (int i = 0; i < argList.size(); i++) {
            args[i] = (String)argList.elementAt(i);
            counter++;
        }

        // XXX
        // should be using system independent line feed!

        StringBuffer niceSourceList = new StringBuffer("Files to be compiled:"
                                                       + "\r\n");

        Enumeration enum = compileList.elements();
        while (enum.hasMoreElements()) {
            args[counter] = (String)enum.nextElement();
            niceSourceList.append("    " + args[counter] + "\r\n");
            counter++;
        }

        log(niceSourceList.toString(), Project.MSG_VERBOSE);

        // XXX
        // provide the compiler a different message sink - namely our own

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sun.tools.javac.Main compiler =
                new sun.tools.javac.Main(new TaskOutputStream(this, Project.MSG_WARN), "javac");

        if (!compiler.compile(args)) {
            throw new BuildException("Compile failed");
        }
    }

    /**
     * Performs a compile using the newer compiler that ships with JDK 1.3
     */

    private void doModernCompile() throws BuildException {
        try {
            Class.forName("com.sun.tools.javac.Main");
        } catch (ClassNotFoundException cnfe) {
            doClassicCompile();
            return;
        }

        log("Using modern compiler", Project.MSG_VERBOSE);
        Path classpath = getCompileClasspath(false);
        Vector argList = new Vector();

        if (deprecation == true)
            argList.addElement("-deprecation");

        argList.addElement("-d");
        argList.addElement(destDir.getAbsolutePath());
        argList.addElement("-classpath");
        argList.addElement(classpath.toString());
        argList.addElement("-sourcepath");
        argList.addElement(src.toString());
        if (target != null) {
            argList.addElement("-target");
            argList.addElement(target);
        }
        if (debug) {
            argList.addElement("-g");
        }
        if (optimize) {
            argList.addElement("-O");
        }
        if (bootclasspath != null) {
            argList.addElement("-bootclasspath");
            argList.addElement(bootclasspath.toString());
        }
        if (extdirs != null) {
            argList.addElement("-extdirs");
            argList.addElement(extdirs.toString());
        }

        log("Compilation args: " + argList.toString(),
            Project.MSG_VERBOSE);

        String[] args = new String[argList.size() + compileList.size()];
        int counter = 0;

        for (int i = 0; i < argList.size(); i++) {
            args[i] = (String)argList.elementAt(i);
            counter++;
        }

        // XXX
        // should be using system independent line feed!

        StringBuffer niceSourceList = new StringBuffer("Files to be compiled:"
                                                       + "\r\n");

        Enumeration enum = compileList.elements();
        while (enum.hasMoreElements()) {
            args[counter] = (String)enum.nextElement();
            niceSourceList.append("    " + args[counter] + "\r\n");
            counter++;
        }

        log(niceSourceList.toString(), Project.MSG_VERBOSE);

        // This won't build under JDK1.2.2 because the new compiler
        // doesn't exist there.
        //com.sun.tools.javac.Main compiler = new com.sun.tools.javac.Main();
        //if (compiler.compile(args) != 0) {

        // Use reflection to be able to build on all JDKs >= 1.1:
        try {
            Class c = Class.forName ("com.sun.tools.javac.Main");
            Object compiler = c.newInstance ();
            Method compile = c.getMethod ("compile",
                new Class [] {(new String [] {}).getClass ()});
            int result = ((Integer) compile.invoke
                          (compiler, new Object [] {args})) .intValue ();
            if (result != MODERN_COMPILER_SUCCESS) {
                String msg = 
                    "Compile failed, messages should have been provided.";
                throw new BuildException(msg);
            }
        } catch (Exception ex) {
                throw new BuildException (ex);
        }
    }

    /**
     * Performs a compile using the Jikes compiler from IBM..
     * Mostly of this code is identical to doClassicCompile()
     * However, it does not support all options like
     * bootclasspath, extdirs, deprecation and so on, because
     * there is no option in jikes and I don't understand
     * what they should do.
     *
     * It has been successfully tested with jikes 1.10
     *
     * @author skanthak@muehlheim.de
     */

    private void doJikesCompile() throws BuildException {
        log("Using jikes compiler", Project.MSG_VERBOSE);

        Path classpath = new Path();

        // Jikes doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        classpath.append(getCompileClasspath(true));

        // Jikes doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        addExtdirsToClasspath(classpath);

        // Jikes has no option for source-path so we
        // will add it to classpath.
        classpath.append(src);

        Vector argList = new Vector();

        if (deprecation == true)
            argList.addElement("-deprecation");

        // We want all output on stdout to make
        // parsing easier
        argList.addElement("-Xstdout");

        argList.addElement("-d");
        argList.addElement(destDir.getAbsolutePath());
        argList.addElement("-classpath");
        argList.addElement(classpath.toString());

        if (debug) {
            argList.addElement("-g");
        }
        if (optimize) {
            argList.addElement("-O");
        }

       /**
        * XXX
        * Perhaps we shouldn't use properties for these
        * two options (emacs mode and warnings),
        * but include it in the javac directive?
        */

       /**
        * Jikes has the nice feature to print error
        * messages in a form readable by emacs, so
        * that emcas can directly set the cursor
        * to the place, where the error occured.
        */
       boolean emacsMode = false;
       String emacsProperty = project.getProperty("build.compiler.emacs");
       if (emacsProperty != null &&
           (emacsProperty.equalsIgnoreCase("on") ||
            emacsProperty.equalsIgnoreCase("true"))
           ) {
           emacsMode = true;
       }

       /**
        * Jikes issues more warnings that javac, for
        * example, when you have files in your classpath
        * that don't exist. As this is often the case, these
        * warning can be pretty annoying.
        */
       boolean warnings = true;
       String warningsProperty = project.getProperty("build.compiler.warnings");
       if (warningsProperty != null &&
           (warningsProperty.equalsIgnoreCase("off") ||
            warningsProperty.equalsIgnoreCase("false"))
           ) {
           warnings = false;
       }

       if (emacsMode)
           argList.addElement("+E");

       if (!warnings)
           argList.addElement("-nowarn");

        log("Compilation args: " + argList.toString(),
            Project.MSG_VERBOSE);

        String[] args = new String[argList.size() + compileList.size()];
        int counter = 0;

        for (int i = 0; i < argList.size(); i++) {
            args[i] = (String)argList.elementAt(i);
            counter++;
        }

        // XXX
        // should be using system independent line feed!

        StringBuffer niceSourceList = new StringBuffer("Files to be compiled:"
                                                       + "\r\n");

        Enumeration enum = compileList.elements();
        while (enum.hasMoreElements()) {
            args[counter] = (String)enum.nextElement();
            niceSourceList.append("    " + args[counter] + "\r\n");
            counter++;
        }

        log(niceSourceList.toString(), Project.MSG_VERBOSE);

        // XXX
        // provide the compiler a different message sink - namely our own

        JikesOutputParser jop = new JikesOutputParser(this, emacsMode);

        Jikes compiler = new Jikes(jop,"jikes");
        compiler.compile(args);
        if (jop.getErrorFlag()) {
            String msg = "Compile failed, messages should have been provided.";
            throw new BuildException(msg);
        }
    }

    class JarFilenameFilter implements FilenameFilter {
        public boolean accept(File dir,String name) {
            return name.endsWith(".jar");
        }
    }

    /**
     * Emulation of extdirs feature in java >= 1.2.
     * This method adds all jar archives in the given
     * directories (but not in sub-directories!) to the classpath,
     * so that you don't have to specify them all one by one.
     * @param classpath - Path to append jar files to
     */
    private void addExtdirsToClasspath(Path classpath) {
       // FIXME
       // Should we scan files recursively? How does
       // javac handle this?

       if (extdirs != null) {
           String[] list = extdirs.list();
           for (int j=0; j<list.length; j++) {
               File dir = project.resolveFile(list[j]);
               String[] files = dir.list(new JarFilenameFilter());
               for (int i=0 ; i < files.length ; i++) {
                   File f = new File(dir,files[i]);
                   if (f.exists() && f.isFile()) {
                       classpath.setLocation(f.getAbsolutePath());
                   }
               }
           }
       }
    }
}

