/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.compilers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * This is the default implementation for the CompilerAdapter interface.
 * Currently, this is a cut-and-paste of the original javac task.
 *
 * @since Ant 1.3
 */
public abstract class DefaultCompilerAdapter
    implements CompilerAdapter, CompilerAdapterExtension {

    private static final int COMMAND_LINE_LIMIT;
    static {
        if (Os.isFamily("os/2")) {
            // OS/2 CMD.EXE has a much smaller limit around 1K
            COMMAND_LINE_LIMIT = 1000;
        } else {
            COMMAND_LINE_LIMIT = 4096;  // 4K
        }
    }
    // CheckStyle:VisibilityModifier OFF - bc

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    //must keep for subclass BC, though unused:
    // CheckStyle:ConstantNameCheck OFF - bc
    @Deprecated
    protected static final String lSep = StringUtils.LINE_SEP;
    // CheckStyle:ConstantNameCheck ON

    private static final Pattern JAVAC_ARG_FILE_CHARS_TO_QUOTE = Pattern.compile("[ #]"); // space or # character

    protected Path src;
    protected File destDir;
    protected String encoding;
    protected boolean debug = false;
    protected boolean optimize = false;
    protected boolean deprecation = false;
    protected boolean depend = false;
    protected boolean verbose = false;
    protected String target;
    protected String release;
    protected Path bootclasspath;
    protected Path extdirs;
    protected Path compileClasspath;
    protected Path modulepath;
    protected Path upgrademodulepath;
    protected Path compileSourcepath;
    protected Path moduleSourcepath;
    protected Project project;
    protected Location location;
    protected boolean includeAntRuntime;
    protected boolean includeJavaRuntime;
    protected String memoryInitialSize;
    protected String memoryMaximumSize;

    protected File[] compileList;
    protected Javac attributes;

    // CheckStyle:VisibilityModifier ON

    /**
     * Set the Javac instance which contains the configured compilation
     * attributes.
     *
     * @param attributes a configured Javac task.
     */
    @Override
    public void setJavac(final Javac attributes) {
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
        release = attributes.getRelease();
        bootclasspath = attributes.getBootclasspath();
        extdirs = attributes.getExtdirs();
        compileList = attributes.getFileList();
        compileClasspath = attributes.getClasspath();
        modulepath = attributes.getModulepath();
        upgrademodulepath = attributes.getUpgrademodulepath();
        compileSourcepath = attributes.getSourcepath();
        moduleSourcepath = attributes.getModulesourcepath();
        project = attributes.getProject();
        location = attributes.getLocation();
        includeAntRuntime = attributes.getIncludeantruntime();
        includeJavaRuntime = attributes.getIncludejavaruntime();
        memoryInitialSize = attributes.getMemoryInitialSize();
        memoryMaximumSize = attributes.getMemoryMaximumSize();
        if (moduleSourcepath != null && src == null && compileSourcepath == null) {
            //Compatibility to prevent NPE from Jikes, Jvc, Kjc
            compileSourcepath = new Path(getProject());
        }
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
     * By default, only recognize files with a Java extension,
     * but specialized compilers can recognize multiple kinds
     * of files.
     */
    @Override
    public String[] getSupportedFileExtensions() {
        return new String[] {"java"};
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
        final Path classpath = new Path(project);

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
     * Builds the modulepath.
     * @return the modulepath
     * @since 1.9.7
     */
    protected Path getModulepath() {
        final Path mp = new Path(getProject());
        if (modulepath != null) {
            mp.addExisting(modulepath);
        }
        return mp;
    }

    /**
     * Builds the upgrademodulepath.
     * @return the upgrademodulepath
     * @since 1.9.7
     */
    protected Path getUpgrademodulepath() {
        final Path ump = new Path(getProject());
        if (upgrademodulepath != null) {
            ump.addExisting(upgrademodulepath);
        }
        return ump;
    }

    /**
     * Builds the modulesourcepath for multi module compilation.
     * @return the modulesourcepath
     * @since 1.9.7
     */
    protected Path getModulesourcepath() {
        final Path msp = new Path(getProject());
        if (moduleSourcepath != null) {
            msp.add(moduleSourcepath);
        }
        return msp;
    }

    /**
     * Get the command line arguments for the switches.
     * @param cmd the command line
     * @return the command line
     */
    protected Commandline setupJavacCommandlineSwitches(final Commandline cmd) {
        return setupJavacCommandlineSwitches(cmd, false);
    }

    /**
     * Does the command line argument processing common to classic and
     * modern.  Doesn't add the files to compile.
     * @param cmd the command line
     * @param useDebugLevel if true set set the debug level with the -g switch
     * @return the command line
     */
    protected Commandline setupJavacCommandlineSwitches(final Commandline cmd,
                                                        final boolean useDebugLevel) {
        final Path classpath = getCompileClasspath();
        // For -sourcepath, use the "sourcepath" value if present.
        // Otherwise default to the "srcdir" value.
        Path sourcepath;
        if (compileSourcepath != null) {
            sourcepath = compileSourcepath;
        } else {
            sourcepath = src;
        }

        final String memoryParameterPrefix = assumeJava1_2Plus() ? "-J-X" : "-J-";

        if (memoryInitialSize != null) {
            if (!attributes.isForkedJavac()) {
                attributes.log(
                    "Since fork is false, ignoring memoryInitialSize setting.",
                    Project.MSG_WARN);
            } else {
                cmd.createArgument().setValue(memoryParameterPrefix
                                              + "ms" + memoryInitialSize);
            }
        }

        if (memoryMaximumSize != null) {
            if (!attributes.isForkedJavac()) {
                attributes.log(
                    "Since fork is false, ignoring memoryMaximumSize setting.",
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

        // Just add "sourcepath" to classpath (for JDK1.1)
        // as well as "bootclasspath" and "extdirs"
        if (!assumeJava1_2Plus()) {
            final Path cp = new Path(project);
            Optional.ofNullable(getBootClassPath()).ifPresent(cp::append);

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
            if (release == null || !assumeJava9Plus()) {
                if (target != null) {
                    cmd.createArgument().setValue("-target");
                    cmd.createArgument().setValue(target);
                }

                final Path bp = getBootClassPath();
                if (!bp.isEmpty()) {
                    cmd.createArgument().setValue("-bootclasspath");
                    cmd.createArgument().setPath(bp);
                }
            }

            if (extdirs != null && !extdirs.isEmpty()) {
                cmd.createArgument().setValue("-extdirs");
                cmd.createArgument().setPath(extdirs);
            }
        }

        if (encoding != null) {
            cmd.createArgument().setValue("-encoding");
            cmd.createArgument().setValue(encoding);
        }
        if (debug) {
            if (useDebugLevel && assumeJava1_2Plus()) {
                final String debugLevel = attributes.getDebugLevel();
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
        	if (assumeJava1_3Plus()) {
        		attributes.log(
                        "depend attribute is not supported by the modern compiler",
                        Project.MSG_WARN);
        	} else if (assumeJava1_2Plus()) {
        		cmd.createArgument().setValue("-Xdepend");
        	} else { //java 1.1
                cmd.createArgument().setValue("-depend");
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
    protected Commandline setupModernJavacCommandlineSwitches(final Commandline cmd) {
        setupJavacCommandlineSwitches(cmd, true);
        if (assumeJava1_4Plus()) { // -source added with JDK 1.4
            final String t = attributes.getTarget();
            final String s = attributes.getSource();
            if (release == null || !assumeJava9Plus()) {
                if (release != null) {
                    attributes.log(
                        "Support for javac --release has been added in Java9 ignoring it");
                }
                if (s != null) {
                    cmd.createArgument().setValue("-source");
                    cmd.createArgument().setValue(adjustSourceValue(s));

                } else if (t != null && mustSetSourceForTarget(t)) {
                    setImplicitSourceSwitch(cmd, t, adjustSourceValue(t));
                }
            } else { // Java 9+ and release has been set
                if (t != null || s != null || getBootClassPath().size() > 0) {
                    attributes.log(
                        "Ignoring source, target and bootclasspath as release has been set",
                        Project.MSG_WARN);
                }
                cmd.createArgument().setValue("--release");
                cmd.createArgument().setValue(release);
            }
        }
        final Path msp = getModulesourcepath();
        if (!msp.isEmpty()) {
            cmd.createArgument().setValue("--module-source-path");
            cmd.createArgument().setPath(msp);
        }
        final Path mp = getModulepath();
        if (!mp.isEmpty()) {
            cmd.createArgument().setValue("--module-path");
            cmd.createArgument().setPath(mp);
        }
        final Path ump = getUpgrademodulepath();
        if (!ump.isEmpty()) {
            cmd.createArgument().setValue("--upgrade-module-path");
            cmd.createArgument().setPath(ump);
        }
        if (attributes.getNativeHeaderDir() != null) {
            if (!assumeJava1_8Plus()) {
                attributes.log(
                    "Support for javac -h has been added in Java8, ignoring it");
            } else {
                cmd.createArgument().setValue("-h");
                cmd.createArgument().setFile(attributes.getNativeHeaderDir());
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
        final Commandline cmd = new Commandline();
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
    protected Commandline setupJavacCommand(final boolean debugLevelCheck) {
        final Commandline cmd = new Commandline();
        setupJavacCommandlineSwitches(cmd, debugLevelCheck);
        logAndAddFilesToCompile(cmd);
        return cmd;
    }

    /**
     * Logs the compilation parameters, adds the files to compile and logs the
     * &quot;niceSourceList&quot;
     * @param cmd the command line
     */
    protected void logAndAddFilesToCompile(final Commandline cmd) {
        attributes.log("Compilation " + cmd.describeArguments(), Project.MSG_VERBOSE);

        attributes.log(String.format("%s to be compiled:",
                compileList.length == 1 ? "File" : "Files"), Project.MSG_VERBOSE);

        attributes.log(Stream.of(compileList).map(File::getAbsolutePath)
                        .peek(arg -> cmd.createArgument().setValue(arg))
                        .map(arg -> String.format("    %s%n", arg))
                        .collect(Collectors.joining("")), Project.MSG_VERBOSE);
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
    protected int executeExternalCompile(final String[] args, final int firstFileName) {
        return executeExternalCompile(args, firstFileName, true);
    }

    /**
     * Do the compile with the specified arguments.
     *
     * <p>The working directory if the executed process will be the
     * project's base directory.</p>
     *
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
    protected int executeExternalCompile(final String[] args, final int firstFileName,
                                         final boolean quoteFiles) {
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
                try {
                    tmpFile = FILE_UTILS.createTempFile(
                        getProject(), "files", "", getJavac().getTempdir(), true, true);
                    try (BufferedWriter out =
                        new BufferedWriter(new FileWriter(tmpFile))) {
                        for (int i = firstFileName; i < args.length; i++) {
                            if (quoteFiles && JAVAC_ARG_FILE_CHARS_TO_QUOTE.matcher(args[i]).find()) {
                                args[i] =
                                    args[i].replace(File.separatorChar, '/');
                                out.write("\"" + args[i] + "\"");
                            } else {
                                out.write(args[i]);
                            }
                            out.newLine();
                        }
                        out.flush();
                        commandArray = new String[firstFileName + 1];
                        System.arraycopy(args, 0, commandArray, 0,
                            firstFileName);
                        commandArray[firstFileName] = "@" + tmpFile;
                    }
                } catch (final IOException e) {
                    throw new BuildException("Error creating temporary file",
                                             e, location);
                }
            } else {
                commandArray = args;
            }

            try {
                final Execute exe = new Execute(
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
            } catch (final IOException e) {
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
    @Deprecated
    protected void addExtdirsToClasspath(final Path classpath) {
        classpath.addExtdirs(extdirs);
    }

    /**
     * Adds the command line arguments specific to the current implementation.
     * @param cmd the command line to use
     */
    protected void addCurrentCompilerArgs(final Commandline cmd) {
        cmd.addArguments(getJavac().getCurrentCompilerArgs());
    }

    /**
     * Shall we assume JDK 1.1 command line switches?
     * @return true if jdk 1.1
     * @since Ant 1.5
     * @deprecated since Ant 1.10.7, use assumeJava1_1Plus, if necessary combined with !assumeJava1_2Plus
     */
    @Deprecated
    protected boolean assumeJava11() {
        return assumeJava1_1Plus() && !assumeJava1_2Plus();
    }
    
    /**
     * Shall we assume JDK 1.1+ command line switches?
     * @return true if jdk 1.1 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_1Plus() {
        return CompilerAdapterFactory.COMPILER_JAVAC_1_1
            .equalsIgnoreCase(attributes.getCompilerVersion())
            || assumeJava1_2Plus();
    }
    
    /**
     * Shall we assume JDK 1.2 command line switches?
     * @return true if jdk 1.2
     * @since Ant 1.5
     * @deprecated since Ant 1.10.7, use assumeJava1_2Plus, if necessary combined with !assumeJava1_3Plus
     */
    @Deprecated
    protected boolean assumeJava12() {
    	return assumeJava1_2Plus() && !assumeJava1_3Plus();
    }

    /**
     * Shall we assume JDK 1.2+ command line switches?
     * @return true if jdk 1.2 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_2Plus() {
        return CompilerAdapterFactory.COMPILER_JAVAC_1_2
            .equalsIgnoreCase(attributes.getCompilerVersion())
            || assumeJava1_3Plus();
    }
    
    /**
     * Shall we assume JDK 1.3 command line switches?
     * @return true if jdk 1.3
     * @since Ant 1.5
     * @deprecated since Ant 1.10.7, use assumeJava1_3Plus, if necessary combined with !assumeJava1_4Plus
     */
    @Deprecated
    protected boolean assumeJava13() {
    	return assumeJava1_3Plus() && !assumeJava1_4Plus();
    }

    /**
     * Shall we assume JDK 1.3+ command line switches?
     * @return true if jdk 1.3 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_3Plus() {
        return CompilerAdapterFactory.COMPILER_JAVAC_1_3
            .equalsIgnoreCase(attributes.getCompilerVersion())
            || assumeJava1_4Plus();
    }
    
    /**
     * Shall we assume JDK 1.4 command line switches?
     * @return true if jdk 1.4
     * @since Ant 1.6.3
     * @deprecated since Ant 1.10.7, use assumeJava1_4Plus, if necessary combined with !assumeJava1_5Plus
     */
    @Deprecated
    protected boolean assumeJava14() {
    	return assumeJava1_4Plus() && !assumeJava1_5Plus();
    }
    
    /**
     * Shall we assume JDK 1.4+ command line switches?
     * @return true if jdk 1.4 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_4Plus() {
        return assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_1_4, JavaEnvUtils.JAVA_1_4)
             || assumeJava1_5Plus();
    }

    /**
     * Shall we assume JDK 1.5 command line switches?
     * @return true if JDK 1.5
     * @since Ant 1.6.3
     * @deprecated since Ant 1.10.7, use assumeJava1_5Plus, if necessary combined with !assumeJava1_6Plus
     */
    @Deprecated
    protected boolean assumeJava15() {
    	return assumeJava1_5Plus() && !assumeJava1_6Plus();
    }
    
    /**
     * Shall we assume JDK 1.5+ command line switches?
     * @return true if jdk 1.5 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_5Plus() {
        return assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_1_5, JavaEnvUtils.JAVA_1_5)
            || assumeJava1_6Plus();
    }

    /**
     * Shall we assume JDK 1.6 command line switches?
     * @return true if JDK 1.6
     * @since Ant 1.7
     * @deprecated since Ant 1.10.7, use assumeJava1_6Plus, if necessary combined with !assumeJava1_7Plus
     */
    @Deprecated
    protected boolean assumeJava16() {
    	return assumeJava1_6Plus() && !assumeJava1_7Plus();
    }

    /**
     * Shall we assume JDK 1.6+ command line switches?
     * @return true if jdk 1.6 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_6Plus() {
        return assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_1_6, JavaEnvUtils.JAVA_1_6)
            || assumeJava1_7Plus() ;
    }
    
    /**
     * Shall we assume JDK 1.7 command line switches?
     * @return true if JDK 1.7
     * @since Ant 1.8.2
     * @deprecated since Ant 1.10.7, use assumeJava1_7Plus, if necessary combined with !assumeJava1_8Plus
     */
    @Deprecated
    protected boolean assumeJava17() {
    	return assumeJava1_7Plus() && !assumeJava1_8Plus();
    }
    
    /**
     * Shall we assume JDK 1.7+ command line switches?
     * @return true if jdk 1.7 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_7Plus() {
        return assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_1_7, JavaEnvUtils.JAVA_1_7)
             || assumeJava1_8Plus();
    }

    /**
     * Shall we assume JDK 1.8 command line switches?
     * @return true if JDK 1.8
     * @since Ant 1.8.3
     * @deprecated since Ant 1.10.7, use assumeJava1_8Plus, if necessary combined with !assumeJava9Plus
     */
    @Deprecated
    protected boolean assumeJava18() {
    	return assumeJava1_8Plus() && !assumeJava9Plus();
    }

    /**
     * Shall we assume JDK 1.8+ command line switches?
     * @return true if jdk 1.8 and above
     * @since Ant 1.10.7
     */
    protected boolean assumeJava1_8Plus() {
        return assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_1_8, JavaEnvUtils.JAVA_1_8)
            || assumeJava9Plus();
    }
    
    /**
     * Shall we assume JDK 9 command line switches?
     * @return true if JDK 9
     * @since Ant 1.9.4
     * @deprecated use #assumeJava9 instead
     */
    @Deprecated
    protected boolean assumeJava19() {
    	return assumeJava9();
    }

    /**
     * Shall we assume JDK 9 command line switches?
     * @return true if JDK 9
     * @since Ant 1.9.8
     * @deprecated since Ant 1.10.7, use assumeJava9Plus, in the future if necessary combined with !assumeJava10Plus
     */
    @Deprecated
    protected boolean assumeJava9() {
        return assumeJava9Plus() && !assumeJava10Plus();
    }

    /**
     * Shall we assume JDK 9+ command line switches?
     * @return true if JDK 9+
     * @since Ant 1.10.2
     */
    protected boolean assumeJava9Plus() {
        return assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_9, JavaEnvUtils.JAVA_9)
            || assumeJavaXY(CompilerAdapterFactory.COMPILER_JAVAC_9_ALIAS, JavaEnvUtils.JAVA_9)
            || assumeJava10Plus();
    }

    /**
     * Shall we assume JDK 10+ command line switches?
     * @return true if JDK 10+
     * @since Ant 1.10.7
     */
    protected boolean assumeJava10Plus() {
        return CompilerAdapterFactory.COMPILER_JAVAC_10_PLUS.equalsIgnoreCase(attributes.getCompilerVersion())
                || (JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_10)
                    && CompilerAdapterFactory.isJdkCompilerNickname(attributes.getCompilerVersion()));
    }
    
    /**
     * Shall we assume command line switches for the given version of Java?
     * @since Ant 1.8.3
     */
    private boolean assumeJavaXY(final String javacXY, final String javaEnvVersionXY) {
        String compilerVersion = attributes.getCompilerVersion();
        return javacXY.equalsIgnoreCase(compilerVersion) ||
                (JavaEnvUtils.isJavaVersion(javaEnvVersionXY)
                 && CompilerAdapterFactory.isJdkCompilerNickname(attributes.getCompilerVersion()));
    }

    /**
     * Combines a user specified bootclasspath with the system
     * bootclasspath taking build.sysclasspath into account.
     *
     * @return a non-null Path instance that combines the user
     * specified and the system bootclasspath.
     */
    protected Path getBootClassPath() {
        final Path bp = new Path(project);
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
    	return assumeJava1_2Plus() ? "-g:none" : null;
    }

    private void setImplicitSourceSwitch(final Commandline cmd,
                                         final String target, final String source) {
        attributes.log("", Project.MSG_WARN);
        attributes.log("          WARNING", Project.MSG_WARN);
        attributes.log("", Project.MSG_WARN);
        attributes.log("The -source switch defaults to " + getDefaultSource()
                       + ".",
                       Project.MSG_WARN);
        attributes.log("If you specify -target " + target
                       + " you now must also specify -source " + source
                       + ".", Project.MSG_WARN);
        attributes.log("Ant will implicitly add -source " + source
                       + " for you.  Please change your build file.",
                       Project.MSG_WARN);
        cmd.createArgument().setValue("-source");
        cmd.createArgument().setValue(source);
    }

    /**
     * A string that describes the default value for -source of the
     * selected JDK's javac.
     */
    private String getDefaultSource() {
        if (assumeJava9Plus()) {
            return "9 in JDK 9";
        }
        if (assumeJava1_8Plus()) {
            return "1.8 in JDK 1.8";
        }
        if (assumeJava1_7Plus()) {
            return "1.7 in JDK 1.7";
        }
        if (assumeJava1_5Plus()) {
            return "1.5 in JDK 1.5 and 1.6";
        }
        return "";
    }

    /**
     * Whether the selected -target is known to be incompatible with
     * the default -source value of the selected JDK's javac.
     *
     * <p>Assumes it will never be called unless the selected JDK is
     * at least Java 1.5.</p>
     *
     * @param t the -target value, must not be null
     */
    private boolean mustSetSourceForTarget(String t) {
        if (!assumeJava1_5Plus()) {
            return false;
        }
        if (t.startsWith("1.")) {
            t = t.substring(2);
        }
        return "1".equals(t) || "2".equals(t) || "3".equals(t) || "4".equals(t)
                || (("5".equals(t) || "6".equals(t)) && assumeJava1_7Plus())
                || ("7".equals(t) && assumeJava1_8Plus())
                || ("8".equals(t) && assumeJava9Plus())
                || ("9".equals(t) && assumeJava10Plus());
    }

    /**
     * Turn the task's attribute for -source into something that is
     * understood by all javac's after 1.4.
     *
     * <p>support for -source 1.1 and -source 1.2 has been added with
     * JDK 1.4.2 but isn't present in 1.5.0+</p>
     */
    private String adjustSourceValue(final String source) {
        return "1.1".equals(source) || "1.2".equals(source) ? "1.3" : source;
    }
}
