/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.compilers;

import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is the default implementation for the CompilerAdapter interface.
 * Currently, this is a cut-and-paste of the original javac task.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green 
 *         <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 *
 * @since Ant 1.3
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
    protected Path compileSourcepath;
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

    /**
     * Set the Javac instance which contains the configured compilation
     * attributes.
     *
     * @param attributes a configured Javac task.
     */
    public void setJavac(Javac attributes) {
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
        compileSourcepath = attributes.getSourcepath();
        project = attributes.getProject();
        location = attributes.getLocation();
        includeAntRuntime = attributes.getIncludeantruntime();
        includeJavaRuntime = attributes.getIncludejavaruntime();
        memoryInitialSize = attributes.getMemoryInitialSize();
        memoryMaximumSize = attributes.getMemoryMaximumSize();
    }

    /**
     * Get the Javac task instance associated with this compiler adapter
     *
     * @return the configured Javac task instance used by this adapter.
     */
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
        // order determined by the value of build.sysclasspath

        Path cp = compileClasspath;
        if (cp == null) {
            cp = new Path(project);
        }
        if (includeAntRuntime) {
            classpath.addExisting(cp.concatSystemClasspath("last"));
        } else {
            classpath.addExisting(cp.concatSystemClasspath("ignore"));
        }

        if (includeJavaRuntime) {
            classpath.addJavaRuntime();
        }

        return classpath;
    }

    protected Commandline setupJavacCommandlineSwitches(Commandline cmd) {
        return setupJavacCommandlineSwitches(cmd, false);
    }

    /**
     * Does the command line argument processing common to classic and
     * modern.  Doesn't add the files to compile.
     */
    protected Commandline setupJavacCommandlineSwitches(Commandline cmd,
                                                        boolean useDebugLevel) {
        Path classpath = getCompileClasspath();
        // For -sourcepath, use the "sourcepath" value if present.
        // Otherwise default to the "srcdir" value.
        Path sourcepath = null;
        if (compileSourcepath != null) {
            sourcepath = compileSourcepath;
        } else {
            sourcepath = src;
        }

        String memoryParameterPrefix = assumeJava11() ? "-J-" : "-J-X";
        if (memoryInitialSize != null) {
            if (!attributes.isForkedJavac()) {
                attributes.log("Since fork is false, ignoring "
                               + "memoryInitialSize setting.", 
                               Project.MSG_WARN);
            } else {
                cmd.createArgument().setValue(memoryParameterPrefix
                                              + "ms" + memoryInitialSize);
            }
        }

        if (memoryMaximumSize != null) {
            if (!attributes.isForkedJavac()) {
                attributes.log("Since fork is false, ignoring "
                               + "memoryMaximumSize setting.",
                               Project.MSG_WARN);
            } else {
                cmd.createArgument().setValue(memoryParameterPrefix
                                              + "mx" + memoryMaximumSize);
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
        if (assumeJava11()) {
            Path cp = new Path(project);
            /*
             * XXX - This doesn't mix very well with build.systemclasspath,
             */
            if (bootclasspath != null) {
                cp.append(bootclasspath);
            }
            if (extdirs != null) {
                cp.addExtdirs(extdirs);
            }
            cp.append(classpath);
            cp.append(sourcepath);
            cmd.createArgument().setPath(cp);
        } else {
            cmd.createArgument().setPath(classpath);
            // If the buildfile specifies sourcepath="", then don't
            // output any sourcepath.
            if (sourcepath.size() > 0) {
                cmd.createArgument().setValue("-sourcepath");
                cmd.createArgument().setPath(sourcepath);
            }
            if (target != null) {
                cmd.createArgument().setValue("-target");
                cmd.createArgument().setValue(target);
            }
            if (bootclasspath != null && bootclasspath.size() > 0) {
                cmd.createArgument().setValue("-bootclasspath");
                cmd.createArgument().setPath(bootclasspath);
            }
            if (extdirs != null && extdirs.size() > 0) {
                cmd.createArgument().setValue("-extdirs");
                cmd.createArgument().setPath(extdirs);
            }
        }

        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }
        if (debug) {
            if (useDebugLevel && !assumeJava11()) {
                String debugLevel = attributes.getDebugLevel();
                if (debugLevel != null) {
                    cmd.createArgument().setValue("-g:" + debugLevel);
                } else {
                    cmd.createArgument().setValue("-g");
                }
            } else {
                cmd.createArgument().setValue("-g");
            }
        } else if (!assumeJava11()) {
            cmd.createArgument().setValue("-g:none");
        }
        if (optimize) {
            cmd.createArgument().setValue("-O");
        }

        if (depend) {
            if (assumeJava11()) {
                cmd.createArgument().setValue("-depend");
            } else if (assumeJava12()) {
                cmd.createArgument().setValue("-Xdepend");
            } else {
                attributes.log("depend attribute is not supported by the "
                               + "modern compiler", Project.MSG_WARN);
            }
        }

        if (verbose) {
            cmd.createArgument().setValue("-verbose");
        }

        addCurrentCompilerArgs(cmd);

        return cmd;
    }

    /**
     * Does the command line argument processing for modern.  Doesn't
     * add the files to compile.
     */
    protected Commandline setupModernJavacCommandlineSwitches(Commandline cmd) {
        setupJavacCommandlineSwitches(cmd, true);
        if (attributes.getSource() != null && !assumeJava13()) {
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

    protected Commandline setupJavacCommand() {
        return setupJavacCommand(false);
    }

    /**
     * Does the command line argument processing for classic and adds
     * the files to compile as well.
     */
    protected Commandline setupJavacCommand(boolean debugLevelCheck) {
        Commandline cmd = new Commandline();
        setupJavacCommandlineSwitches(cmd, debugLevelCheck);
        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &qout;niceSourceList&quot;
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        attributes.log("Compilation " + cmd.describeArguments(),
                       Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.length != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        niceSourceList.append(lSep);

        for (int i = 0; i < compileList.length; i++) {
            String arg = compileList[i].getAbsolutePath();
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    " + arg + lSep);
        }

        attributes.log(niceSourceList.toString(), Project.MSG_VERBOSE);
    }

    /**
     * Do the compile with the specified arguments.
     * @param args - arguments to pass to process on command line
     * @param firstFileName - index of the first source file in args,
     * if the index is negative, no temporary file will ever be
     * created, but this may hit the command line length limit on your
     * system.
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
            if (Commandline.toString(args).length() > 4096 
                && firstFileName >= 0) {
                PrintWriter out = null;
                try {
                    String userDirName = System.getProperty("user.dir");
                    File userDir = new File(userDirName);
                    tmpFile = fileUtils.createTempFile("files", "", userDir);
                    out = new PrintWriter(new FileWriter(tmpFile));
                    for (int i = firstFileName; i < args.length; i++) {
                        out.println(args[i]);
                    }
                    out.flush();
                    commandArray = new String[firstFileName + 1];
                    System.arraycopy(args, 0, commandArray, 0, firstFileName);
                    commandArray[firstFileName] = "@" + tmpFile;
                } catch (IOException e) {
                    throw new BuildException("Error creating temporary file", 
                                             e, location);
                } finally {
                    if (out != null) {
                        try {out.close();} catch (Throwable t) {}
                    }
                }
            } else {
                commandArray = args;
            }

            try {
                Execute exe = new Execute(
                                  new LogStreamHandler(attributes,
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
     * @deprecated use org.apache.tools.ant.types.Path#addExtdirs instead
     */
    protected void addExtdirsToClasspath(Path classpath) {
        classpath.addExtdirs(extdirs);
    }

    /**
     * Adds the command line arguments specifc to the current implementation.
     */
    protected void addCurrentCompilerArgs(Commandline cmd) {
        cmd.addArguments(getJavac().getCurrentCompilerArgs());
    }

    /**
     * Shall we assume JDK 1.1 command line switches?
     * @since Ant 1.5
     */
    protected boolean assumeJava11() {
        return "javac1.1".equals(attributes.getCompilerVersion()) ||
            ("classic".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) ||
            ("extJavac".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1));
    }

    /**
     * Shall we assume JDK 1.2 command line switches?
     * @since Ant 1.5
     */
    protected boolean assumeJava12() {
        return "javac1.2".equals(attributes.getCompilerVersion()) ||
            ("classic".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)) ||
            ("extJavac".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2));
    }

    /**
     * Shall we assume JDK 1.3 command line switches?
     * @since Ant 1.5
     */
    protected boolean assumeJava13() {
        return "javac1.3".equals(attributes.getCompilerVersion()) ||
            ("classic".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) ||
            ("modern".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) ||
            ("extJavac".equals(attributes.getCompilerVersion()) 
             && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3));
    }

}

