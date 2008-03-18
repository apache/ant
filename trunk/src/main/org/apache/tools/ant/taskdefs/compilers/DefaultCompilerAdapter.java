/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.compilers;

//Java5 style
//import static org.apache.tools.ant.util.StringUtils.LINE_SEP;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 * This is the default implementation for the CompilerAdapter interface.
 * Currently, this is a cut-and-paste of the original javac task.
 *
 * @since Ant 1.3
 */
public abstract class DefaultCompilerAdapter implements CompilerAdapter {
    private static final int COMMAND_LINE_LIMIT = 4096;  // 4K
    // CheckStyle:VisibilityModifier OFF - bc

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

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
    protected Javac attributes;

    //must keep for subclass BC, though unused:
    // CheckStyle:ConstantNameCheck OFF - bc
    protected static final String lSep = StringUtils.LINE_SEP;

    // CheckStyle:ConstantNameCheck ON
    // CheckStyle:VisibilityModifier ON

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
     * Get the project this compiler adapter was created in.
     * @return the owner project
     * @since Ant 1.6
     */
    protected Project getProject() {
        return project;
    }

    /**
     * Builds the compilation classpath.
     * @return the compilation class path
     */
    protected Path getCompileClasspath() {
        Path classpath = new Path(project);

        // add dest dir to classpath so that previously compiled and
        // untouched classes are on classpath

        if (destDir != null && getJavac().isIncludeDestClasses()) {
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

    /**
     * Get the command line arguments for the switches.
     * @param cmd the command line
     * @return the command line
     */
    protected Commandline setupJavacCommandlineSwitches(Commandline cmd) {
        return setupJavacCommandlineSwitches(cmd, false);
    }

    /**
     * Does the command line argument processing common to classic and
     * modern.  Doesn't add the files to compile.
     * @param cmd the command line
     * @param useDebugLevel if true set set the debug level with the -g switch
     * @return the command line
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

        if (deprecation) {
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

            Path bp = getBootClassPath();
            if (bp.size() > 0) {
                cp.append(bp);
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

            Path bp = getBootClassPath();
            if (bp.size() > 0) {
                cmd.createArgument().setValue("-bootclasspath");
                cmd.createArgument().setPath(bp);
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
        } else if (getNoDebugArgument() != null) {
            cmd.createArgument().setValue(getNoDebugArgument());
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
     * @param cmd the command line
     * @return the command line
     */
    protected Commandline setupModernJavacCommandlineSwitches(Commandline cmd) {
        setupJavacCommandlineSwitches(cmd, true);
        if (attributes.getSource() != null && !assumeJava13()) {
            cmd.createArgument().setValue("-source");
            String source = attributes.getSource();
            if (source.equals("1.1") || source.equals("1.2")) {
                // support for -source 1.1 and -source 1.2 has been
                // added with JDK 1.4.2 - and isn't present in 1.5.0
                // or 1.6.0 either
                cmd.createArgument().setValue("1.3");
            } else {
                cmd.createArgument().setValue(source);
            }
        } else if ((assumeJava15() || assumeJava16())
                   && attributes.getTarget() != null) {
            String t = attributes.getTarget();
            if (t.equals("1.1") || t.equals("1.2") || t.equals("1.3")
                || t.equals("1.4")) {
                String s = t;
                if (t.equals("1.1")) {
                    // 1.5.0 doesn't support -source 1.1
                    s = "1.2";
                }
                attributes.log("", Project.MSG_WARN);
                attributes.log("          WARNING", Project.MSG_WARN);
                attributes.log("", Project.MSG_WARN);
                attributes.log("The -source switch defaults to 1.5 in JDK 1.5 and 1.6.",
                               Project.MSG_WARN);
                attributes.log("If you specify -target " + t
                               + " you now must also specify -source " + s
                               + ".", Project.MSG_WARN);
                attributes.log("Ant will implicitly add -source " + s
                               + " for you.  Please change your build file.",
                               Project.MSG_WARN);
                cmd.createArgument().setValue("-source");
                cmd.createArgument().setValue(s);
            }
        }
        return cmd;
    }

    /**
     * Does the command line argument processing for modern and adds
     * the files to compile as well.
     * @return the command line
     */
    protected Commandline setupModernJavacCommand() {
        Commandline cmd = new Commandline();
        setupModernJavacCommandlineSwitches(cmd);

        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Set up the command line.
     * @return the command line
     */
    protected Commandline setupJavacCommand() {
        return setupJavacCommand(false);
    }

    /**
     * Does the command line argument processing for classic and adds
     * the files to compile as well.
     * @param debugLevelCheck if true set the debug level with the -g switch
     * @return the command line
     */
    protected Commandline setupJavacCommand(boolean debugLevelCheck) {
        Commandline cmd = new Commandline();
        setupJavacCommandlineSwitches(cmd, debugLevelCheck);
        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param cmd the command line
     */
    protected void logAndAddFilesToCompile(Commandline cmd) {
        attributes.log("Compilation " + cmd.describeArguments(),
                       Project.MSG_VERBOSE);

        StringBuffer niceSourceList = new StringBuffer("File");
        if (compileList.length != 1) {
            niceSourceList.append("s");
        }
        niceSourceList.append(" to be compiled:");

        niceSourceList.append(StringUtils.LINE_SEP);

        for (int i = 0; i < compileList.length; i++) {
            String arg = compileList[i].getAbsolutePath();
            cmd.createArgument().setValue(arg);
            niceSourceList.append("    ");
            niceSourceList.append(arg);
            niceSourceList.append(StringUtils.LINE_SEP);
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
     * @return the exit code of the compilation
     */
    protected int executeExternalCompile(String[] args, int firstFileName) {
        return executeExternalCompile(args, firstFileName, true);
    }

    /**
     * Do the compile with the specified arguments.
     * @param args - arguments to pass to process on command line
     * @param firstFileName - index of the first source file in args,
     * if the index is negative, no temporary file will ever be
     * created, but this may hit the command line length limit on your
     * system.
     * @param quoteFiles - if set to true, filenames containing
     * spaces will be quoted when they appear in the external file.
     * This is necessary when running JDK 1.4's javac and probably
     * others.
     * @return the exit code of the compilation
     *
     * @since Ant 1.6
     */
    protected int executeExternalCompile(String[] args, int firstFileName,
                                         boolean quoteFiles) {
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
            if (Commandline.toString(args).length() > COMMAND_LINE_LIMIT
                && firstFileName >= 0) {
                PrintWriter out = null;
                try {
                    tmpFile = FILE_UTILS.createTempFile(
                        "files", "", getJavac().getTempdir(), true, true);
                    out = new PrintWriter(new FileWriter(tmpFile));
                    for (int i = firstFileName; i < args.length; i++) {
                        if (quoteFiles && args[i].indexOf(" ") > -1) {
                            args[i] = args[i].replace(File.separatorChar, '/');
                            out.println("\"" + args[i] + "\"");
                        } else {
                            out.println(args[i]);
                        }
                    }
                    out.flush();
                    commandArray = new String[firstFileName + 1];
                    System.arraycopy(args, 0, commandArray, 0, firstFileName);
                    commandArray[firstFileName] = "@" + tmpFile;
                } catch (IOException e) {
                    throw new BuildException("Error creating temporary file",
                                             e, location);
                } finally {
                    FileUtils.close(out);
                }
            } else {
                commandArray = args;
            }

            try {
                Execute exe = new Execute(
                                  new LogStreamHandler(attributes,
                                                       Project.MSG_INFO,
                                                       Project.MSG_WARN));
                if (Os.isFamily("openvms")) {
                    //Use the VM launcher instead of shell launcher on VMS
                    //for java
                    exe.setVMLauncher(true);
                }
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
     * Add extdirs to classpath
     * @param classpath the classpath to use
     * @deprecated since 1.5.x.
     *             Use org.apache.tools.ant.types.Path#addExtdirs instead.
     */
    protected void addExtdirsToClasspath(Path classpath) {
        classpath.addExtdirs(extdirs);
    }

    /**
     * Adds the command line arguments specific to the current implementation.
     * @param cmd the command line to use
     */
    protected void addCurrentCompilerArgs(Commandline cmd) {
        cmd.addArguments(getJavac().getCurrentCompilerArgs());
    }

    /**
     * Shall we assume JDK 1.1 command line switches?
     * @return true if jdk 1.1
     * @since Ant 1.5
     */
    protected boolean assumeJava11() {
        return "javac1.1".equals(attributes.getCompilerVersion());
    }

    /**
     * Shall we assume JDK 1.2 command line switches?
     * @return true if jdk 1.2
     * @since Ant 1.5
     */
    protected boolean assumeJava12() {
        return "javac1.2".equals(attributes.getCompilerVersion())
            || ("classic".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2))
            || ("extJavac".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2));
    }

    /**
     * Shall we assume JDK 1.3 command line switches?
     * @return true if jdk 1.3
     * @since Ant 1.5
     */
    protected boolean assumeJava13() {
        return "javac1.3".equals(attributes.getCompilerVersion())
            || ("classic".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3))
            || ("modern".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3))
            || ("extJavac".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3));
    }

    /**
     * Shall we assume JDK 1.4 command line switches?
     * @return true if jdk 1.4
     * @since Ant 1.6.3
     */
    protected boolean assumeJava14() {
        return "javac1.4".equals(attributes.getCompilerVersion())
            || ("classic".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4))
            || ("modern".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4))
            || ("extJavac".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_4));
    }

    /**
     * Shall we assume JDK 1.5 command line switches?
     * @return true if JDK 1.5
     * @since Ant 1.6.3
     */
    protected boolean assumeJava15() {
        return "javac1.5".equals(attributes.getCompilerVersion())
            || ("classic".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_5))
            || ("modern".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_5))
            || ("extJavac".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_5));
    }

    /**
     * Shall we assume JDK 1.6 command line switches?
     * @return true if JDK 1.6
     * @since Ant 1.7
     */
    protected boolean assumeJava16() {
        return "javac1.6".equals(attributes.getCompilerVersion())
            || ("classic".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_6))
            || ("modern".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_6))
            || ("extJavac".equals(attributes.getCompilerVersion())
                && JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_6));
    }

    /**
     * Combines a user specified bootclasspath with the system
     * bootclasspath taking build.sysclasspath into account.
     *
     * @return a non-null Path instance that combines the user
     * specified and the system bootclasspath.
     */
    protected Path getBootClassPath() {
        Path bp = new Path(project);
        if (bootclasspath != null) {
            bp.append(bootclasspath);
        }
        return bp.concatSystemBootClasspath("ignore");
    }

    /**
     * The argument the compiler wants to see if the debug attribute
     * has been set to false.
     *
     * <p>A return value of <code>null</code> means no argument at all.</p>
     *
     * @return "-g:none" unless we expect to invoke a JDK 1.1 compiler.
     *
     * @since Ant 1.6.3
     */
    protected String getNoDebugArgument() {
        return assumeJava11() ? null : "-g:none";
    }
}

