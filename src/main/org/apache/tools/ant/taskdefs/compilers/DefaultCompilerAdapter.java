/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is the default implementation for the CompilerAdapter interface.
 * Currently, this is a cut-and-paste of the original javac task.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public abstract class DefaultCompilerAdapter implements CompilerAdapter {

    /* jdg - TODO - all these attributes are currently protected, but they
     * should probably be private in the near future.
     */

    protected Path src;
    protected File destDir;
    protected String encoding;
    protected boolean debug = false;
    protected boolean optimize = false;
    protected boolean deprecation = false;
    protected boolean depend = false;
    protected boolean verbose = false;
    protected String target;
    protected Path bootclasspath;
    protected Path extdirs;
    protected Path compileClasspath;
    protected Project project;
    protected Location location;
    protected boolean includeAntRuntime;
    protected boolean includeJavaRuntime;
    protected String memoryInitialSize;
    protected String memoryMaximumSize;

    protected File[] compileList;
    protected static String lSep = System.getProperty("line.separator");
    protected Javac attributes;

    private FileUtils fileUtils = FileUtils.newFileUtils();

    public void setJavac( Javac attributes ) {
        this.attributes = attributes;
        src = attributes.getSrcdir();
        destDir = attributes.getDestdir();
        encoding = attributes.getEncoding();
        debug = attributes.getDebug();
        optimize = attributes.getOptimize();
        deprecation = attributes.getDeprecation();
        depend = attributes.getDepend();
        verbose = attributes.getVerbose();
        target = attributes.getTarget();
        bootclasspath = attributes.getBootclasspath();
        extdirs = attributes.getExtdirs();
        compileList = attributes.getFileList();
        compileClasspath = attributes.getClasspath();
        project = attributes.getProject();
        location = attributes.getLocation();
        includeAntRuntime = attributes.getIncludeantruntime();
        includeJavaRuntime = attributes.getIncludejavaruntime();
        memoryInitialSize = attributes.getMemoryInitialSize();
        memoryMaximumSize = attributes.getMemoryMaximumSize();
    }

    public Javac getJavac() {
        return attributes;
    }

    /**
     * Builds the compilation classpath.
     *
     */
    protected Path getCompileClasspath() {
        Path classpath = new Path(project);

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        if (destDir != null) {
            classpath.setLocation(destDir);
        }

        // Combine the build classpath with the system classpath, in an
        // order determined by the value of build.classpath

        if (compileClasspath == null) {
            if ( includeAntRuntime ) {
                classpath.addExisting(Path.systemClasspath);
            }
        } else {
            if ( includeAntRuntime ) {
                classpath.addExisting(compileClasspath.concatSystemClasspath("last"));
            } else {
                classpath.addExisting(compileClasspath.concatSystemClasspath("ignore"));
            }
        }

        if (includeJavaRuntime) {
            classpath.addJavaRuntime();
        }

        return classpath;
    }

    /**
     * Does the command line argument processing common to classic and
     * modern.  Doesn't add the files to compile.
     */
    protected Commandline setupJavacCommandlineSwitches(Commandline cmd) {
        Path classpath = getCompileClasspath();

        // we cannot be using Java 1.0 when forking, so we only have to
        // distinguish between Java 1.1, and Java 1.2 and higher, as Java 1.1
        // has its own parameter format
        boolean usingJava1_1 = Project.getJavaVersion().equals(Project.JAVA_1_1);
        String memoryParameterPrefix = usingJava1_1 ? "-J-" : "-J-X";
        if (memoryInitialSize != null) {
            if (!attributes.isForkedJavac()) {
                attributes.log("Since fork is false, ignoring memoryInitialSize setting.",
                               Project.MSG_WARN);
            } else {
                cmd.createArgument().setValue(memoryParameterPrefix+"ms"+memoryInitialSize);
            }
        }

        if (memoryMaximumSize != null) {
            if (!attributes.isForkedJavac()) {
                attributes.log("Since fork is false, ignoring memoryMaximumSize setting.",
                               Project.MSG_WARN);
            } else {
                cmd.createArgument().setValue(memoryParameterPrefix+"mx"+memoryMaximumSize);
            }
        }

        if (attributes.getNowarn()) {
            cmd.createArgument().setValue("-nowarn");
        }

        if (deprecation == true) {
            cmd.createArgument().setValue("-deprecation");
        }

        if (destDir != null) {
            cmd.createArgument().setValue("-d");
            cmd.createArgument().setFile(destDir);
        }

        cmd.createArgument().setValue("-classpath");

        // Just add "sourcepath" to classpath ( for JDK1.1 )
        // as well as "bootclasspath" and "extdirs"
        if (Project.getJavaVersion().startsWith("1.1")) {
            Path cp = new Path(project);
            /*
             * XXX - This doesn't mix very well with build.systemclasspath,
             */
            if (bootclasspath != null) {
                cp.append(bootclasspath);
            }
            if (extdirs != null) {
                addExtdirsToClasspath(cp);
            }
            cp.append(classpath);
            cp.append(src);
            cmd.createArgument().setPath(cp);
        } else {
            cmd.createArgument().setPath(classpath);
            cmd.createArgument().setValue("-sourcepath");
            cmd.createArgument().setPath(src);
            if (target != null) {
                cmd.createArgument().setValue("-target");
                cmd.createArgument().setValue(target);
            }
            if (bootclasspath != null) {
                cmd.createArgument().setValue("-bootclasspath");
                cmd.createArgument().setPath(bootclasspath);
            }
            if (extdirs != null) {
                cmd.createArgument().setValue("-extdirs");
                cmd.createArgument().setPath(extdirs);
            }
        }

        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }
        if (debug) {
            cmd.createArgument().setValue("-g");
        } else if (Project.getJavaVersion() != Project.JAVA_1_0 &&
                   Project.getJavaVersion() != Project.JAVA_1_1) {
            cmd.createArgument().setValue("-g:none");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }

        if (depend) {
            if (Project.getJavaVersion().startsWith("1.1")) {
                cmd.createArgument().setValue("-depend");
            } else if (Project.getJavaVersion().startsWith("1.2")) {
                cmd.createArgument().setValue("-Xdepend");
            } else {
                attributes.log("depend attribute is not supported by the modern compiler",
                    Project.MSG_WARN);
            }
        }

        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }
        return cmd;
    }

    /**
     * Does the command line argument processing for modern.  Doesn't
     * add the files to compile.
     */
    protected Commandline setupModernJavacCommandlineSwitches(Commandline cmd) {
        setupJavacCommandlineSwitches(cmd);
        if (attributes.getSource() != null) {
            cmd.createArgument().setValue("-source");
            cmd.createArgument().setValue(attributes.getSource());
        }
        return cmd;
    }

    /**
     * Does the command line argument processing for modern and adds
     * the files to compile as well.
     */
    protected Commandline setupModernJavacCommand() {
        Commandline cmd = new Commandline();
        setupModernJavacCommandlineSwitches(cmd);

        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Does the command line argument processing for classic and adds
     * the files to compile as well.
     */
    protected Commandline setupJavacCommand() {
        Commandline cmd = new Commandline();
        setupJavacCommandlineSwitches(cmd);
        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &qout;niceSourceList&quot;
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        attributes.log("Compilation args: " + cmd.toString(),
            Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.length != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        niceSourceList.append(lSep);

        for (int i=0; i < compileList.length; i++) {
            String arg = compileList[i].getAbsolutePath();
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    " + arg + lSep);
        }

        attributes.log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    /**
     * Do the compile with the specified arguments.
     * @param args - arguments to pass to process on command line
     * @param firstFileName - index of the first source file in args
     */
    protected int executeExternalCompile(String[] args, int firstFileName) {
        String[] commandArray = null;
        File tmpFile = null;

        try {
            /*
             * Many system have been reported to get into trouble with
             * long command lines - no, not only Windows ;-).
             *
             * POSIX seems to define a lower limit of 4k, so use a temporary
             * file if the total length of the command line exceeds this limit.
             */
            if (Commandline.toString(args).length() > 4096) {
                PrintWriter out = null;
                try {
                    tmpFile = fileUtils.createTempFile("jikes", "", null);
                    out = new PrintWriter(new FileWriter(tmpFile));
                    for (int i = firstFileName; i < args.length; i++) {
                        out.println(args[i]);
                    }
                    out.flush();
                    commandArray = new String[firstFileName+1];
                    System.arraycopy(args, 0, commandArray, 0, firstFileName);
                    commandArray[firstFileName] = "@" + tmpFile.getAbsolutePath();
                } catch (IOException e) {
                    throw new BuildException("Error creating temporary file", e, location);
                } finally {
                    if (out != null) {
                        try {out.close();} catch (Throwable t) {}
                    }
                }
            } else {
                commandArray = args;
            }

            try {
                Execute exe = new Execute(new LogStreamHandler(attributes,
                                                               Project.MSG_INFO,
                                                               Project.MSG_WARN));
                exe.setAntRun(project);
                exe.setWorkingDirectory(project.getBaseDir());
                exe.setCommandline(commandArray);
                exe.execute();
                return exe.getExitValue();
            } catch (IOException e) {
                throw new BuildException("Error running " + args[0]
                        + " compiler", e, location);
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
    protected void addExtdirsToClasspath(Path classpath) {
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

}

