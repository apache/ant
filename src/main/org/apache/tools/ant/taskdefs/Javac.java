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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;

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
 * <li>encoding
 * <li>target
 * <li>depend
 * <li>vebose
 * </ul>
 * Of these arguments, the <b>sourcedir</b> and <b>destdir</b> are required.
 * <p>
 * When this task executes, it will recursively scan the sourcedir and
 * destdir looking for Java source files to compile. This task makes its
 * compile decision based on timestamp. 
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
    private String encoding;
    private boolean debug = false;
    private boolean optimize = false;
    private boolean deprecation = false;
    private boolean depend = false;
    private boolean verbose = false;
    private String target;
    private Path bootclasspath;
    private Path extdirs;
    private static String lSep = System.getProperty("line.separator");

    protected Vector compileList = new Vector();

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
     * Maybe creates a nested classpath element.
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path(project);
        }
        return extdirs.createPath();
    }

    /**
     * Set the deprecation flag.
     */
    public void setDeprecation(boolean deprecation) {
        this.deprecation = deprecation;
    }

    /**
     * Set the Java source file encoding name.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
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
     * Set the depend flag.
     */ 
     public void setDepend(boolean depend) {
         this.depend = depend;
     }  

    /** 
     * Set the verbose flag.
     */ 
     public void setVerbose(boolean verbose) {
         this.verbose = verbose;
     }  

    /**
     * Sets the target VM that the classes will be compiled for. Valid
     * strings are "1.1", "1.2", and "1.3".
     */
    public void setTarget(String target) {
        this.target = target;
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
                " source file"
                + (compileList.size() == 1 ? "" : "s")
                + " to " + destDir);

            if (compiler.equalsIgnoreCase("classic")) {
                doClassicCompile();
            } else if (compiler.equalsIgnoreCase("modern")) {
                doModernCompile();
            } else if (compiler.equalsIgnoreCase("jikes")) {
                doJikesCompile();
            } else if (compiler.equalsIgnoreCase("jvc")) {
                doJvcCompile();
            } else {
                String msg = "Don't know how to use compiler " + compiler;
                throw new BuildException(msg);
            }
        }
    }

    /**
     * Clear the list of files to be compiled and copied.. 
     */
    protected void resetFileLists() {
        compileList.removeAllElements();
    }

    /**
     * Scans the directory looking for source files to be compiled.  
     * The results are returned in the class variable compileList
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

                if (!classFile.exists() || srcFile.lastModified() > classFile.lastModified()) {
                    if (!classFile.exists()) {
                        log("Compiling " + srcFile.getPath() + " because class file " 
                                + classFile.getPath() + " does not exist", Project.MSG_DEBUG);
                    }
                    else {
                        log("Compiling " + srcFile.getPath() + " because it is out of date with respect to " 
                                + classFile.getPath(), Project.MSG_DEBUG);
                    }                                                        
                    compileList.addElement(srcFile.getAbsolutePath());
                }
            }
        }
    }

    // XXX
    // we need a way to not use the current classpath.

    /**
     * Builds the compilation classpath.
     *
     * @param addRuntime Shall <code>rt.jar</code> or
     * <code>classes.zip</code> be added to the classpath.  
     */
    private Path getCompileClasspath(boolean addRuntime) {
        Path classpath = new Path(project);

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        classpath.setLocation(destDir);

        // add our classpath to the mix

        if (compileClasspath != null) {
            classpath.addExisting(compileClasspath);
        }

        // add the system classpath

        classpath.addExisting(Path.systemClasspath);
        if (addRuntime) {
            if (Project.getJavaVersion() == Project.JAVA_1_1) {
                classpath.addExisting(new Path(null,
                                                System.getProperty("java.home")
                                                + File.separator + "lib"
                                                + File.separator 
                                                + "classes.zip"));
            } else {
                // JDK > 1.1 seems to set java.home to the JRE directory.
                classpath.addExisting(new Path(null,
                                                System.getProperty("java.home")
                                                + File.separator + "lib"
                                                + File.separator + "rt.jar"));
                // Just keep the old version as well and let addExistingToPath
                // sort it out.
                classpath.addExisting(new Path(null,
                                                System.getProperty("java.home")
                                                + File.separator +"jre"
                                                + File.separator + "lib"
                                                + File.separator + "rt.jar"));
            }
        }
            
        return classpath;
    }

    /**
     * Peforms a compile using the classic compiler that shipped with
     * JDK 1.1 and 1.2.
     */

    private void doClassicCompile() throws BuildException {
        log("Using classic compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupJavacCommand();

        // provide the compiler a different message sink - namely our own
        sun.tools.javac.Main compiler =
                new sun.tools.javac.Main(new LogOutputStream(this, Project.MSG_WARN), "javac");

        if (!compiler.compile(cmd.getArguments())) {
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
        Commandline cmd = setupJavacCommand();

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
                          (compiler, new Object[] {cmd.getArguments()})) .intValue ();
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
     * Does the command line argument processing common to classic and
     * modern.  
     */
    private Commandline setupJavacCommand() {
        Commandline cmd = new Commandline();
        Path classpath = getCompileClasspath(false);

        if (deprecation == true) {
            cmd.createArgument().setValue("-deprecation");
        }

        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(destDir);
        cmd.createArgument().setValue("-classpath");
        // Just add "sourcepath" to classpath ( for JDK1.1 )
        if (Project.getJavaVersion().startsWith("1.1")) {
            cmd.createArgument().setValue(classpath.toString() 
                                          + File.pathSeparator 
                                          + src.toString());
        } else {
            cmd.createArgument().setPath(classpath);
            cmd.createArgument().setValue("-sourcepath");
            cmd.createArgument().setPath(src);
            if (target != null) {
                cmd.createArgument().setValue("-target");
                cmd.createArgument().setValue(target);
            }
        }
        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }
        if (debug) {
            cmd.createArgument().setValue("-g");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }
        if (bootclasspath != null) {
            cmd.createArgument().setValue("-bootclasspath");
            cmd.createArgument().setPath(bootclasspath);
        }
        if (extdirs != null) {
            cmd.createArgument().setValue("-extdirs");
            cmd.createArgument().setPath(extdirs);
        }

        if (depend) {
            if (Project.getJavaVersion().startsWith("1.1")) {
                cmd.createArgument().setValue("-depend");
            } else if (Project.getJavaVersion().startsWith("1.2")) {
                cmd.createArgument().setValue("-Xdepend");
            } else {
                log("depend attribute is not supported by the modern compiler",
                    Project.MSG_WARN);
            }
        }

        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }

        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the 
     * &qout;niceSourceList&quot;
     */
    private void logAndAddFilesToCompile(Commandline cmd) {
        log("Compilation args: " + cmd.toString(),
            Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.size() != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        niceSourceList.append(lSep);

        Enumeration enum = compileList.elements();
        while (enum.hasMoreElements()) {
            String arg = (String)enum.nextElement();
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    " + arg + lSep);
        }

        log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    /**
     * Performs a compile using the Jikes compiler from IBM..
     * Mostly of this code is identical to doClassicCompile()
     * However, it does not support all options like
     * bootclasspath, extdirs, deprecation and so on, because
     * there is no option in jikes and I don't understand
     * what they should do.
     *
     * It has been successfully tested with jikes >1.10
     *
     * @author skanthak@muehlheim.de
     */

    private void doJikesCompile() throws BuildException {
        log("Using jikes compiler", Project.MSG_VERBOSE);

        Path classpath = new Path(project);

        // Jikes doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        // Jikes doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        addExtdirsToClasspath(classpath);

        classpath.append(getCompileClasspath(true));

        // Jikes has no option for source-path so we
        // will add it to classpath.
        classpath.append(src);

        // if the user has set JIKESPATH we should add the contents as well
        String jikesPath = System.getProperty("jikes.class.path");
        if (jikesPath != null) {
            classpath.append(new Path(project, jikesPath));
        }
        
        Commandline cmd = new Commandline();
        cmd.setExecutable("jikes");

        if (deprecation == true)
            cmd.createArgument().setValue("-deprecation");

        cmd.createArgument().setValue("-d");
        cmd.createArgument().setFile(destDir);
        cmd.createArgument().setValue("-classpath");
        cmd.createArgument().setPath(classpath);

        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }
        if (debug) {
            cmd.createArgument().setValue("-g");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }
        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }
        if (depend) {
            cmd.createArgument().setValue("-depend");
            String fullDependProperty = project.getProperty("build.compiler.fulldepend");
            if (fullDependProperty != null 
                && Project.toBoolean(fullDependProperty)) {
                cmd.createArgument().setValue("+F");
            }
        } 
        /**
         * XXX
         * Perhaps we shouldn't use properties for these
         * three options (emacs mode, warnings and pedantic),
         * but include it in the javac directive?
         */

        /**
         * Jikes has the nice feature to print error
         * messages in a form readable by emacs, so
         * that emacs can directly set the cursor
         * to the place, where the error occured.
         */
        String emacsProperty = project.getProperty("build.compiler.emacs");
        if (emacsProperty != null && Project.toBoolean(emacsProperty)) {
            cmd.createArgument().setValue("+E");
        }

        /**
         * Jikes issues more warnings that javac, for
         * example, when you have files in your classpath
         * that don't exist. As this is often the case, these
         * warning can be pretty annoying.
         */
        String warningsProperty = project.getProperty("build.compiler.warnings");
        if (warningsProperty != null && !Project.toBoolean(warningsProperty)) {
            cmd.createArgument().setValue("-nowarn");
        }

        /**
         * Jikes can issue pedantic warnings. 
         */
        String pedanticProperty = project.getProperty("build.compiler.pedantic");
        if (pedanticProperty != null && Project.toBoolean(pedanticProperty)) {
            cmd.createArgument().setValue("+P");
        }
 
        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        if (executeJikesCompile(cmd.getCommandline(), firstFileName) != 0) {
            String msg = "Compile failed, messages should have been provided.";
            throw new BuildException(msg);
        }
    }

    /**
     * Do the compile with the specified arguments.
     * @param args - arguments to pass to process on command line
     * @param firstFileName - index of the first source file in args
     */
    private int executeJikesCompile(String[] args, int firstFileName) {
        String[] commandArray = null;
        File tmpFile = null;

        try {
            String myos = System.getProperty("os.name");

            // Windows has a 32k limit on total arg size, so
            // create a temporary file to store all the arguments

            // There have been reports that 300 files could be compiled
            // so 250 is a conservative approach
            if (myos.toLowerCase().indexOf("windows") >= 0 
                && args.length > 250) {
                PrintWriter out = null;
                try {
                    tmpFile = new File("jikes"+(new Random(System.currentTimeMillis())).nextLong());
                    out = new PrintWriter(new FileWriter(tmpFile));
                    for (int i = firstFileName; i < args.length; i++) {
                        out.println(args[i]);
                    }
                    out.flush();
                    commandArray = new String[firstFileName+1];
                    System.arraycopy(args, 0, commandArray, 0, firstFileName);
                    commandArray[firstFileName] = "@" + tmpFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new BuildException("Error creating temporary file", e);
                } finally {
                    if (out != null) {
                        try {out.close();} catch (Throwable t) {}
                    }
                }
            } else {
                commandArray = args;
            }
            
            try {
                Execute exe = new Execute(new LogStreamHandler(this, 
                                                               Project.MSG_INFO,
                                                               Project.MSG_WARN));
                exe.setAntRun(project);
                exe.setWorkingDirectory(project.getBaseDir());
                exe.setCommandline(commandArray);
                exe.execute();
                return exe.getExitValue();
            } catch (IOException e) {
                throw new BuildException("Error running Jikes compiler", e);
            }
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Emulation of extdirs feature in java >= 1.2.
     * This method adds all files in the given
     * directories (but not in sub-directories!) to the classpath,
     * so that you don't have to specify them all one by one.
     * @param classpath - Path to append files to
     */
    private void addExtdirsToClasspath(Path classpath) {
        if (extdirs == null) {
            String extProp = System.getProperty("java.ext.dirs");
            if (extProp != null) {
                extdirs = new Path(project, extProp);
            } else {
                return;
            }
        }

        String[] dirs = extdirs.list();
        for (int i=0; i<dirs.length; i++) {
            if (!dirs[i].endsWith(File.separator)) {
                dirs[i] += File.separator;
            }
            File dir = project.resolveFile(dirs[i]);
            FileSet fs = new FileSet();
            fs.setDir(dir);
            fs.setIncludes("*");
            classpath.addFileset(fs);
        }
    }

    private void doJvcCompile() throws BuildException {
        log("Using jvc compiler", Project.MSG_VERBOSE);

        Path classpath = new Path(project);

        // jvc doesn't support bootclasspath dir (-bootclasspath)
        // so we'll emulate it for compatibility and convenience.
        if (bootclasspath != null) {
            classpath.append(bootclasspath);
        }

        // jvc doesn't support an extension dir (-extdir)
        // so we'll emulate it for compatibility and convenience.
        addExtdirsToClasspath(classpath);

        classpath.append(getCompileClasspath(true));

        // jvc has no option for source-path so we
        // will add it to classpath.
        classpath.append(src);

        Commandline cmd = new Commandline();
        cmd.setExecutable("jvc");

        cmd.createArgument().setValue("/d");
        cmd.createArgument().setFile(destDir);
        // Add the Classpath before the "internal" one.
        cmd.createArgument().setValue("/cp:p");
        cmd.createArgument().setPath(classpath);

        // Enable MS-Extensions and ...
        cmd.createArgument().setValue("/x-");
        // ... do not display a Message about this.
        cmd.createArgument().setValue("/nomessage");
        // Do not display Logo
        cmd.createArgument().setValue("/nologo");

        if (debug) {
            cmd.createArgument().setValue("/g");
        }
        if (optimize) {
            cmd.createArgument().setValue("/O");
        }

        int firstFileName = cmd.size();
        logAndAddFilesToCompile(cmd);

        if (executeJikesCompile(cmd.getCommandline(), firstFileName) != 0) {
            String msg = "Compile failed, messages should have been provided.";
            throw new BuildException(msg);
        }
    }
}

